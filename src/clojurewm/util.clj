(ns clojurewm.util
  (:require [clojure.clr.emit :as emit]
            [clojure.tools.logging :as log]))

;; TODO simplify this.

(def ^:private dg-type-cache (atom {}))


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

;; data types

(defrecord tag [hotkey windows])

;; commands

(def commands (atom {}))

(defn get-command [key-sequence]
  (when-let [command (@commands key-sequence)]
    (log/trace "Lookup command for:" key-sequence command)
    (resolve command)))

(defn index-command [cmd-name hotkey]
  (let [gen-key (fn [key] (eval (symbol (str "System.Windows.Forms.Keys/" (name key)))))
        hotkey (map gen-key hotkey)
        cmd-name (symbol (str (. *ns* Name) "/" cmd-name))]
    (swap! commands assoc hotkey cmd-name)))

(defmacro defcommand [cmd-name hotkey & body]
  (index-command cmd-name (eval hotkey))
  `(defn ~cmd-name []
     ~@body))


