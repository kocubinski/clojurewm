(ns clojurewm.win
  (:use [clojure.clr.pinvoke]
        [clojure.clr emit]
        [clojurewm.const])
  (:require [clojure.tools.logging :as log])
  (:import [System.Threading Thread ThreadPool WaitCallback]
           [System.Windows.Forms Label]
           [System.Drawing Point Size]
           [System.Text StringBuilder]))

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

;; info bar

(defn get-control [parent name]
  (first (filter #(= (.Name %) name ) (.Controls parent))))

(def info-bar
  (let [form (clojurewm.InfoBar.)
        text (doto (Label.)
               (.set_Name "text")
               (.set_Size (Size. 500 30))
               (.set_Location (Point. 10 10)))]
    (.Add (.Controls form) text)
    form))

(defn show-info-bar []
  (.Show info-bar)
  (set! (. info-bar TopMost) true))

(defn hide-info-bar []
  (.Hide info-bar))

(defn set-info-text [text]
  (let [label (get-control info-bar "text")]
    (set! (. label Text) text)))

(defn clear-info []
  (set-info-text ""))

(defn show-info-text [text]
  (show-info-bar)
  (set-info-text text))
;;;

(dllimports
 "User32.dll"
 (GetForegroundWindow IntPtr [])
 (SetForegroundWindow Boolean [IntPtr])
 (BringWindowToTop Boolean [IntPtr])
 (GetWindowText Int32 [IntPtr StringBuilder Int32])
 (GetWindowTextLength Int32 [IntPtr]))

(defn get-window-text [hwnd]
  (let [sb (StringBuilder. (inc (GetWindowTextLength hwnd)))]
    (GetWindowText hwnd sb (.Capacity sb))
    sb))

(defn try-set-foreground-window [hwnd]
  (loop [times (range 5)]
    (when (and (seq times)
               (not (SetForegroundWindow hwnd)))
      (Thread/Sleep 10)
      (when (= 1 (count times)) (log/warn "SetForegroundWindow failed."))
      (recur (rest times))))
  (loop [times (range 10)]
    (when (and (seq times)
               (not= (GetForegroundWindow) hwnd))
      (Thread/Sleep 30)
      (BringWindowToTop hwnd)
      (when (= 1 (count times)) (log/warn "BringWindowToTop failed."))
      (recur (rest times)))))


