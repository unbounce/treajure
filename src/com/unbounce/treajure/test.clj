(ns com.unbounce.treajure.test
  (:require  [com.unbounce.treajure.either :as either]
             [clojure.test :refer [do-report]]))

(defmacro fail
  "Fails a unit test with the provided message."
  [message]
  `(do-report {:type :fail
               :message ~message}))

(defmacro assert-left
  [assertion either-val]
  `(either/either
    ~assertion
    #(fail (str "Expecting to receieve a left value; got instead: " %))
    ~either-val))

(defmacro assert-right
  [assertion either-val]
  `(either/either
    #(fail (str "Expecting to receive a right value; got instead " %))
    ~assertion
    ~either-val))
