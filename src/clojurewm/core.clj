(ns clojurewm.core
  (:require [clojurewm.win.keys :as keys])
  (:import
   [System.Threading Thread ThreadStart]))

(defn init-hooks []
  (let [thread (Thread.
                (gen-delegate ThreadStart [] (keys/register-hooks)))]
    (swap! keys/hooks-context assoc :thread thread)
    (.Start thread)))

(defn stop-hooks []
  (keys/remove-hooks))
