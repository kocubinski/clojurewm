(ns clojurewm.win
  (:use [clojure.clr.pinvoke]
        [clojure.clr emit])
  (:require [clojure.tools.logging :as log])
  (:import [System.Threading Thread ThreadPool WaitCallback]))

(def this-proc-addr
  (.. (System.Diagnostics.Process/GetCurrentProcess) MainModule BaseAddress))

(def ^:private dg-type-cache (atom {}))

;; TODO simplify this.
(defn- get-dg-type [ret-type param-types]
  (let [dg-sig (into [ret-type] param-types)]
    (or (@dg-type-cache dg-sig)
        (let [dg-type (clr-delegate* ret-type param-types)]
          (swap! dg-type-cache assoc dg-sig dg-type)
          dg-type))))

(defmacro gen-c-delegate [ret params args & body]
  (let [dg-type (get-dg-type (eval ret) (eval params))]
    `(let [dg# (gen-delegate ~dg-type ~args ~@body)
           gch# (System.Runtime.InteropServices.GCHandle/Alloc
                 dg#)
           fp# (System.Runtime.InteropServices.Marshal/GetFunctionPointerForDelegate dg#)]
       {:dg dg#
        :gch gch#
        :fp fp#})))

(dllimports
 "User32.dll"
 (GetForegroundWindow IntPtr [])
 (SetForegroundWindow Boolean [IntPtr])
 (SetActiveWindow IntPtr [IntPtr])
 (ShowWindow Boolean [IntPtr Int32])
 (SwitchToThisWindow nil [IntPtr Boolean])
 (BringWindowToTop Boolean [IntPtr]))

;; deprecated?
(defn try-focus-window [hwnd]
  (ThreadPool/QueueUserWorkItem
   (gen-delegate |WaitCallback| [state]
                 (SwitchToThisWindow hwnd false)
                 (comment (loop [times (range 5)]
                            (when (seq times)
                              (ShowWindow hwnd (int 11))
                              (Thread/Sleep 50)
                              (ShowWindow hwnd (int 9))
                              (let [res (SetForegroundWindow hwnd)]
                                (log/info "SetForegroundWindow:" res)
                                (when-not res (recur (rest times))))))))))

(defn try-set-foreground-window [hwnd]
  (loop [times (range 5)]
    (when (and (seq times)
               (not (SetForegroundWindow hwnd)))
      (Thread/Sleep 10)
      (recur (rest times))))
  (loop [times (range 5)]
    (when (and (seq times)
               (not= (GetForegroundWindow) hwnd))
      (Thread/Sleep 30)
      (BringWindowToTop hwnd)
      (recur (rest times)))))


