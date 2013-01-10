(ns clojurewm.core
  (:require [clojure.tools.logging :as log]
            [clojurewm.win :as win])
  (:use [clojure.clr.pinvoke :only [dllimports]]
        [clojurewm.util :only [gen-c-delegate defcommand commands get-command]]
        [clojurewm.const])
  (:import [System.Runtime.InteropServices Marshal]
           [System.Windows.Forms Keys]
           [System.Threading ThreadPool Thread ThreadStart WaitCallback]
           [System.Windows.Forms MessageBox]
           [clojurewm.util tag]))

(def hook-context {:thread nil :keyboard-hook nil})

(def ^:dynamic *state* nil)

(def tags (atom {}))

;; p-invoke

(dllimports
 "User32.dll"
 (CallNextHookEx IntPtr [IntPtr Int32 UInt32 IntPtr])
 (SetWindowsHookEx IntPtr [Int32 IntPtr IntPtr UInt32])
 (UnhookWindowsHookEx Boolean [IntPtr])
 (GetKeyState Int16 [Int32]))

(def key-modifiers
  (list Keys/LControlKey Keys/RControlKey
        Keys/LMenu Keys/RMenu
        Keys/LShiftKey Keys/RShiftKey
        Keys/LWin Keys/RWin))

(defn get-key-state [key]
  (= 0x8000
     (bit-and (GetKeyState key) 0x8000)))

(defn get-modifiers []
  (filter get-key-state key-modifiers))

(defn is-modifier? [key]
  (some #(= key %) key-modifiers))

(defn get-tag [key-sequence]
  (@tags key-sequence))

;; command processing

(defn focus-windows [tag]
  (let [{:keys [modifiers windows]} tag]
    (log/info "Focus windows" tag)
    (doseq [hwnd windows]
      (win/force-foreground-window hwnd))))

(defcommand handle-tag-window [:T :LMenu :LShiftKey]
  (log/info "Assigning key...")
  (win/show-info-text "Waiting for keystroke...")
  (set! *state* (assoc *state* :is-assigning true)))

(defn tag-window [hotkey]
  (let [hwnd (win/GetForegroundWindow)]
    (if (get-tag hotkey)
      (swap! tags update-in [hotkey :windows] conj hwnd)
      (swap! tags assoc hotkey (tag. hotkey #{hwnd})))
    (set! *state* (assoc *state* :is-assigning false))
    (log/info "New tag:" (@tags hotkey))
    (win/hide-info-bar)))

(defn dispatch-key [key]
  (let [modifiers (get-modifiers)
        key-sequence (conj modifiers key)
        command (get-command key-sequence)
        tag (get-tag key-sequence)]
    (cond
     command (command)
     (:is-assigning *state*) (tag-window key-sequence)
     tag (focus-windows tag)
     :else :pass-key)))

(defn handle-key [key key-state]
  (when (and (= key-state :key-down) (not (is-modifier? key)))
    (if (= (dispatch-key key) :pass-key)
      (int 0)
      (int 1))))

;; init/destruction

(def keyboard-hook-proc
  (gen-c-delegate
   Int32 [Int32 UInt32 IntPtr] [n-code w-param l-param]
   (if (>= n-code 0)
     (try
       (let [key (Enum/ToObject Keys (Marshal/ReadInt32 l-param))]
         (handle-key key (if (or (= w-param WM_KEYDOWN)
                                 (= w-param WM_SYSKEYDOWN))
                           :key-down
                           :key-up)))
       (catch Exception ex
         (log/error ex)))
     (CallNextHookEx (:keyboard-hook hook-context) n-code w-param l-param))))

(defn register-hooks []
  (binding [*state* {:is-assigning false}]
    (alter-var-root #'hook-context assoc :keyboard-hook (SetWindowsHookEx
                                                         WH_KEYBOARD_LL
                                                         (:fp keyboard-hook-proc)
                                                         win/this-proc-addr
                                                         (uint 0)))
    (System.Windows.Forms.Application/Run win/info-bar)))

(defn remove-hooks []
  (UnhookWindowsHookEx (:keyboard-hook @hook-context))
  (.Abort (:thread hook-context)))

(defn init-hooks []
  (let [thread (Thread.
                (gen-delegate ThreadStart [] (register-hooks)))]
    (alter-var-root #'hook-context assoc :thread thread)
    (.Start thread)))

(defcommand exit-clojurewm [:Q :LMenu :LShiftKey]
  (win/show-info-text "Exiting...")
  (Thread/Sleep 500)
  (doseq [{:keys [hwnd fullscreen] :as window} (vals (win/get-tracked-windows))]
    (when fullscreen (win/unset-fullscreen window)))
  (. (:thread hook-context) Abort))


;; other commands

(defcommand next-window [:J :LMenu]
  (log/debug "next-window"))

(defcommand prev-window [:K :LMenu]
  (log/debug "prev-window"))

(defcommand clear-hotkey [:C :LMenu :LShiftKey]
  (log/debug "clear-hotkey"))
