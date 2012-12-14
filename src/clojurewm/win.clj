(ns clojurewm.win
  (:use [clojure.clr.pinvoke]
        [clojure.clr emit])
  (:require [clojure.tools.logging :as log]))

(def this-proc-addr
  (.. (System.Diagnostics.Process/GetCurrentProcess) MainModule BaseAddress))

(def ^:private dg-type-cache (atom {}))

(def ^:private hooks-context (atom {}))

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

(comment (defmacro gen-c-delegate [ret params args & body]
           (let [dg-type (clr-delegate* (eval ret) (eval params))]
             `(let [dg# (gen-delegate ~dg-type ~args ~@body)
                    gch# (System.Runtime.InteropServices.GCHandle/Alloc
                          dg#)
                    fp# (System.Runtime.InteropServices.Marshal/GetFunctionPointerForDelegate dg#)]
                {:dg dg#
                 :gch gch#
                 :fp fp#}))))
