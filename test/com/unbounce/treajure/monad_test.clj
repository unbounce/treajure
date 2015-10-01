(ns com.unbounce.treajure.monad-test
  (:require [com.unbounce.treajure.monad :as m]
            [com.unbounce.treajure.either :as either]

            [com.unbounce.treajure.test :refer [fail]]
            [clojure.test :refer :all]))

(deftest bind-ignore-tests
  (let [ref (atom :not-called)]

    (either/either
     #(fail (str "should not receieve Left value: " %))
     #(is (= 0 %) (str "should have received last value; got instead: " %))
     (m/bind-ignore
      (either/right 123)
      (do
        (reset! ref :called)
        (either/right 999))
      (either/right 0)))

    (is (= :called @ref)
        "side-effect was not performed on monadic sub-routine")))
