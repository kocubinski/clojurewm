(assembly-load "ClojureClrEx")
(assembly-load "System.Windows.Forms")

(ns clojurewm.init
  (:require [clojure.tools.logging :as log]
            [clojurewm.keys :as keys]))

(keys/init-hooks)
