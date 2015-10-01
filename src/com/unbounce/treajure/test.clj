(ns com.unbounce.treajure.test
  (:require  [clojure.test :refer [do-report]]))

(defmacro fail
  "Fails a unit test with the provided message."
  [message]
  `(do-report {:type :fail
               :message ~message}))
