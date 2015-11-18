(ns com.unbounce.treajure.either-test
  (:require [com.unbounce.treajure.either :as either]
            [com.unbounce.treajure.monad :as m]
            [com.unbounce.treajure.test :refer [assert-left assert-right]]
            [clojure.test :refer :all]))


(deftest test-try-either
  (testing "when call throws exception"
    (assert-left #(is (instance? ArithmeticException %))
                 (either/try-either (/ 1 0))))

  (testing "when call doesn't throw an exception"
    (assert-right #(= 3.0 %)
                  (either/try-either (/ 6 2))))

  (testing "with on-failure callback"
    (assert-left #(is (= :called %))
                 (either/try-either (/ 1 0)
                                    (constantly :called)))))

(deftest test-or-else
  (testing "returns first right value"
    (is (= "right 1"
           (either/from-right "failing default"
                              (either/or-else*
                               (either/right "right 1")
                               (either/left "left 2")
                               (either/right "right 3")))))
    (is (= "right 3"
           (either/from-right "failing default"
                              (either/or-else*
                               (either/left "left 1")
                               (either/left "left 2")
                               (either/right "right 3"))))))

  (testing "returns last left if no right value"
    (is (= "left 3"
           (either/from-left "failing default"
                             (either/or-else*
                              (either/left "left 1")
                              (either/left "left 2")
                              (either/left "left 3")))))))


(deftest test-left-map
  (testing "with left value"
    (assert-left #(is (= 2 %))
                 (either/left-map inc (either/left 1))))

  (testing "with right value"
    (assert-right #(is (zero? %))
                  (either/left-map inc (either/right 0)))))


(deftest test-bind-ignore-with-left
  (let [error-msg :err]
    (assert-left
     #(is (= error-msg %))
     (m/bind-ignore
      (either/right 123)
      (either/left error-msg)
      (either/right 456)))))
