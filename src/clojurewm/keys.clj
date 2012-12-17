(ns clojurewm.keys
  (:require [clojure.tools.logging :as log]
            [clojurewm.win :as win])
  (:use [clojure.clr.pinvoke :only [dllimports]]
        [clojurewm.const])
  (:import [System.Runtime.InteropServices Marshal]
           [System.Windows.Forms Keys]
           [System.Threading ThreadPool Thread ThreadStart WaitCallback]
           [System.Windows.Forms MessageBox]))

(def state (atom {:is-assigning false}))
(def hooks-context (atom {}))
(def hotkeys (atom {}))

(dllimports
 "User32.dll"
 (CallNextHookEx IntPtr [IntPtr Int32 UInt32 IntPtr])
 (SetWindowsHookEx IntPtr [Int32 IntPtr IntPtr UInt32])
 (UnhookWindowsHookEx Boolean [IntPtr])
 (GetKeyState Int16 [Int32]))

(def key-modifiers [Keys/LControlKey Keys/RControlKey
                    Keys/LMenu Keys/RMenu
                    Keys/LShiftKey Keys/RShiftKey
                    Keys/LWin Keys/RWin])

(defn get-key-state [key]
  (= 0x8000
     (bit-and (GetKeyState key) 0x8000)))

(defn get-modifiers []
  (vec (filter get-key-state key-modifiers)))

(defn is-modifier? [key]
  (some #(= key %) key-modifiers))

(defn focus-window [hotkey]
  (let [{:keys [modifiers hwnd]} hotkey]
    (when (= (get-modifiers) modifiers)
      (log/info "Focus window" hotkey)
      ;;(win/try-set-foreground-window hwnd)
      (win/force-foreground-window hwnd)
      (int 1))))

(defn handle-assign-key []
  (log/info "Assigning key...")
  (win/show-info-text "Waiting for keystroke...")
  (swap! state assoc :is-assigning true)
  (int 1))

(defn assign-key [key]
  (let [key-map {:key key :modifiers (get-modifiers) :hwnd (win/GetForegroundWindow)}]
    (swap! state assoc :is-assigning false)
    (log/info "Got key" key-map)
    (win/hide-info-bar)
    (swap! hotkeys assoc key key-map))
  (int 1))

(defn handle-key [key key-state]
  (when (and (= key-state :key-down) (not (is-modifier? key)))
    (cond
     (and (= Keys/T key)
          (= (get-modifiers) [Keys/LMenu Keys/LShiftKey])) (handle-assign-key)
     (:is-assigning @state) (assign-key key)
     (@hotkeys key) (focus-window (@hotkeys key))
     :else (int 0))))

(def keyboard-hook-proc
  (win/gen-c-delegate
   Int32 [Int32 UInt32 IntPtr] [n-code w-param l-param]
   (if (>= n-code 0)
     (try 
       (let [key (Marshal/ReadInt32 l-param)]
         (if-let [res (handle-key key (if (or (= w-param WM_KEYDOWN)
                                              (= w-param WM_SYSKEYDOWN))
                                          :key-down
                                          :key-up))]
           res
           (CallNextHookEx (:keyboard-hook @hooks-context) n-code w-param l-param)))
       (catch Exception ex
         (log/error ex)))
     (CallNextHookEx (:keyboard-hook @hooks-context) n-code w-param l-param))))

(defn register-hooks []
  (swap! hooks-context assoc :keyboard-hook (SetWindowsHookEx WH_KEYBOARD_LL
                                                              (:fp keyboard-hook-proc)
                                                              win/this-proc-addr
                                                              (uint 0)))
  (System.Windows.Forms.Application/Run win/info-bar))

(defn remove-hooks []
  (UnhookWindowsHookEx (:keyboard-hook @hooks-context))
  (.Abort (:thread @hooks-context)))

(defn init-hooks []
  (let [thread (Thread.
                (gen-delegate ThreadStart [] (register-hooks)))]
    (swap! hooks-context assoc :thread thread)
    (.Start thread)))
