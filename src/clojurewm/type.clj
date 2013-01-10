(ns clojurewm.type
  (:use clojure.clr.emit)
  (:import [System.Runtime.InteropServices Marshal]))

(defonce rect-struct
  (let [rect-builder (clr-type*
                      (cur-gen-context)
                      "clojurewm.RectStruct" [:SequentialLayout] nil [])]
    (clr-field* rect-builder "Left" Int32 [:Public])
    (clr-field* rect-builder "Top" Int32 [:Public])
    (clr-field* rect-builder "Right" Int32 [:Public])
    (clr-field* rect-builder "Bottom" Int32 [:Public])
    (.CreateType rect-builder)))

(defonce monitor-info
  (let [mi-builder (clr-type*
                    (cur-gen-context)
                    "clojurewm.MonitorInfo" [:SequentialLayout] nil [])]
    (clr-field* mi-builder "Size" Int32 [:Public])
    (clr-field* mi-builder "Monitor" clojurewm.RectStruct [:Public])
    (clr-field* mi-builder "WorkArea" clojurewm.RectStruct [:Public])
    (clr-field* mi-builder "Flags" UInt32 [:Public])
    (.CreateType mi-builder)))

(defn new-monitor-info []
  (let [mi (Activator/CreateInstance clojurewm.MonitorInfo)]
    (.SetValue (.GetField clojurewm.MonitorInfo "Size") mi (Marshal/SizeOf mi))
    mi))
