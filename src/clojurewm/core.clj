(ns clojurewm.core
  (:require [clojure.clr.emit :as emit]))

;; TODO simplify this.

(def ^:private dg-type-cache (atom {}))

(def commands (atom {}))

(defn- get-dg-type [ret-type param-types]
  (let [dg-sig (into [ret-type] param-types)]
    (or (@dg-type-cache dg-sig)
        (let [dg-type (emit/clr-delegate* ret-type param-types)]
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

(defn index-command [name keys]
  (println name keys))

(defmacro defcommand [name keys & body]
  (let [keys (eval keys)
        gen-key (fn [key] (eval (symbol (str "System.Windows.Forms.Keys/" (name key)))))
        ;;hotkeys (vec (map gen-key keys))
        ]
    (doseq [key keys]
      (println (name (eval key))))))
