(ns clojurewm.win.keys
  (:require [clojure.tools.logging :as log])
  (:use [clojurewm.win :only [gen-c-delegate this-proc-addr]]
        [clojure.clr.pinvoke :only [dllimports]])
  (:import [System.Runtime.InteropServices Marshal]))

(def WH_KEYBOARD_LL (int 13))
(def WM_KEYDOWN 0x100)
(def WM_KEYUP 0x101)
(def WM_SYSKEYDOWN 0x104)
(def WM_SYSKEYUP  0x105)

(def key-state (atom {}))
(def hooks-context (atom {}))

(dllimports
 "User32.dll"
 (CallNextHookEx IntPtr [IntPtr Int32 UIntPtr IntPtr])
 (SetWindowsHookEx IntPtr [Int32 IntPtr IntPtr UInt32])
 (UnhookWindowsHookEx Boolean [IntPtr]))

(def keyboard-hook-proc
  (gen-c-delegate
   IntPtr [Int32 UInt32 IntPtr] [n-code w-param l-param]
   (let [key (Marshal/ReadInt32 l-param)]
     (log/info "Key:" key "State:" (.ToString w-param "X")))
   (CallNextHookEx (:keyboard-hook @hooks-context) n-code w-param l-param)
   IntPtr/Zero))

(defn register-hooks []
  (swap! hooks-context assoc :keyboard-hook (SetWindowsHookEx WH_KEYBOARD_LL
                                                              (:fp keyboard-hook-proc)
                                                              this-proc-addr
                                                              (uint 0)))
  (System.Windows.Forms.Application/Run))

(defn remove-hooks []
  (UnhookWindowsHookEx (:keyboard-hook @hooks-context))
  (.Abort (:thread @hooks-context)))

