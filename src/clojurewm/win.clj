(ns clojurewm.win
  (:use [clojure.clr.pinvoke]
        [clojurewm.const]
        [clojurewm.type :only [new-monitor-info]]
        [clojurewm.core :only [gen-c-delegate defcommand]])
  (:require [clojure.tools.logging :as log]
            [clojure.clr.emit :as emit])
  (:import
   [System.Threading Thread ThreadPool WaitCallback]
   [System.Windows.Forms Label]
   [System.Drawing Point Size]
   [System.Text StringBuilder]
   [System.Runtime.InteropServices Marshal]))

(def this-proc-addr
  (.. (System.Diagnostics.Process/GetCurrentProcess) MainModule BaseAddress))


(def windows (atom {}))

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
 (GetWindowTextLength Int32 [IntPtr])
 (SendMessageTimeout IntPtr [IntPtr UInt32 UIntPtr IntPtr UInt32 UInt32 IntPtr])
 (GetWindowThreadProcessId UInt32 [IntPtr IntPtr])
 (AttachThreadInput Boolean [UInt32 UInt32 Boolean])
 (MonitorFromWindow IntPtr [IntPtr UInt32])
 (GetMonitorInfo Boolean [IntPtr IntPtr])
 (SetWindowLong Int32 [IntPtr Int32 Int32])
 (GetWindowLong Int32 [IntPtr Int32])
 (GetWindowRect Boolean [IntPtr IntPtr])
 (SetWindowPos Boolean [IntPtr IntPtr Int32 Int32 Int32 Int32 Int32]))

(dllimports
 "kernel32.dll"
 (GetCurrentThreadId UInt32 []))

(defn get-window-text [hwnd]
  (let [sb (StringBuilder. (inc (GetWindowTextLength hwnd)))]
    (GetWindowText hwnd sb (.Capacity sb))
    sb))

(defn is-window-hung? [hwnd]
  (=
   IntPtr/Zero
   (SendMessageTimeout hwnd (uint WM_NULL) UIntPtr/Zero IntPtr/Zero
                       (uint (bit-or SMTO_ABORTIFHUNG SMTO_BLOCK))
                       (uint 3000) IntPtr/Zero)))

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

(defn try-cross-thread-set-foreground-window [hwnd foreground-window]
  (when-not (is-window-hung? foreground-window)
    (let [fground-thread (GetWindowThreadProcessId foreground-window IntPtr/Zero)]
      (when (AttachThreadInput (GetCurrentThreadId) fground-thread true)
        (let [target-hwnd-thread (GetWindowThreadProcessId hwnd IntPtr/Zero)
              hwnd-attached? (and (not= fground-thread target-hwnd-thread)
                                  (AttachThreadInput fground-thread target-hwnd-thread true))]
          (try-set-foreground-window hwnd)
          (when hwnd-attached?
            (AttachThreadInput fground-thread target-hwnd-thread false))
          (AttachThreadInput (GetCurrentThreadId) fground-thread false))))))

;; TODO parent window checking?
(defn force-foreground-window [hwnd]
  (when-not (is-window-hung? hwnd)
    (let [foreground-window (GetForegroundWindow)]
      (if (not= hwnd foreground-window)
        (if (= foreground-window IntPtr/Zero)
          (try-set-foreground-window hwnd)
          (try-cross-thread-set-foreground-window hwnd foreground-window))
        (BringWindowToTop hwnd)))))

(defn get-monitor-info [hwnd]
  (let [hmon (MonitorFromWindow hwnd MONITOR_DEFAULTTONEAREST)
        mi (new-monitor-info)
        ptr-mi (Marshal/AllocHGlobal (Marshal/SizeOf mi))]
    (Marshal/StructureToPtr mi ptr-mi false)
    (GetMonitorInfo hmon ptr-mi)
    (Marshal/PtrToStructure ptr-mi mi)
    (Marshal/FreeHGlobal ptr-mi)
    mi))

(defn get-window-rect [hwnd]
  (let [rect (Activator/CreateInstance clojurewm.RectStruct)
        ptr-rect (Marshal/AllocHGlobal (Marshal/SizeOf rect))]
    (Marshal/StructureToPtr rect ptr-rect false)
    (GetWindowRect hwnd ptr-rect)
    (Marshal/PtrToStructure ptr-rect rect)
    (Marshal/FreeHGlobal ptr-rect)
    rect))

(defn save-window-style [hwnd]
  (let [win-style (GetWindowLong hwnd GWL_STYLE)
        win-ex-style (GetWindowLong hwnd GWL_EXSTYLE)
        rect (get-window-rect hwnd)
        win-map {:win-style win-style :win-ex-style win-ex-style :rect rect
                 :fullscreen true :hwnd hwnd}]
    (swap! windows assoc hwnd win-map)
    win-map))

(defn unset-fullscreen [hwnd]
  (let [{:keys [win-style win-ex-style rect]} (@windows hwnd)]
    (SetWindowLong hwnd GWL_STYLE win-style)
    (SetWindowLong hwnd GWL_EXSTYLE win-ex-style)
    (SetWindowPos hwnd IntPtr/Zero
                  (.Left rect) (.Top rect) (.Right rect) (.Bottom rect)
                  (int (bit-or SWP_NOZORDER SWP_NOACTIVATE SWP_FRAMECHANGED)))))

;; need to possibly restore (if maximized) to properly scale as fullscreen?
(defn set-fullscreen [hwnd]
  (let [mi (get-monitor-info hwnd)
        mon (.Monitor mi)
        {:keys [win-style win-ex-style]} (save-window-style hwnd)]
    (log/info "setting" hwnd "fullscreen")
    (SetWindowLong hwnd GWL_STYLE
                   (int (bit-and win-style
                                 (bit-not (bit-or WS_CAPTION WS_THICKFRAME)))))
    (SetWindowLong hwnd GWL_EXSTYLE
                   (int (bit-and win-ex-style
                                 (bit-not (bit-or WS_EX_DLGMODALFRAME WS_EX_WINDOWEDGE
                                                  WS_EX_CLIENTEDGE WS_EX_STATICEDGE)))))
    (SetWindowPos hwnd IntPtr/Zero
                  (.Left mon) (.Top mon) (.Right mon) (.Bottom mon)
                  (int (bit-or SWP_NOZORDER SWP_NOACTIVATE SWP_FRAMECHANGED)))))

(defcommand toggle-fullscreen [:F :LMenu :LShiftKey]
  (let [hwnd (GetForegroundWindow)
        window (@windows hwnd)]
    (if (and window (:fullscreen window))
      (do (unset-fullscreen hwnd)
          (swap! windows assoc-in [hwnd :fullscreen] false))
      (set-fullscreen hwnd))))
