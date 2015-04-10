(ns com.unbounce.treajure.concurrent.mvar-test
  (:require [com.unbounce.treajure.concurrent.mvar :refer :all]
            [clojure.test :refer :all]))

(deftest mvar-tests
  (testing "take-mvar"
    (let [input 777
          mvar-val (empty-mvar)
          take-future (future (take-mvar mvar-val))]

      (Thread/sleep 200)
      (is (not (realized? take-future))
          "take-mvar didn't block with empty mvar")

      (put-mvar mvar-val input)
      (Thread/sleep 200)
      (is (realized? take-future)
          "take-mvar did not get unblocked after put-mvar call")

      (is (= input @take-future)
          "take-mvar is not returning expected put value")

      (let [put-future (future (put-mvar mvar-val input))]
        (Thread/sleep 200)
        (is (realized? put-future)
            "take-mvar didn't empty mvar"))))

  (testing "put-mvar"
    (let [initial-input 777
          input 666
          mvar-val (mvar initial-input)
          put-future (future (put-mvar mvar-val input))]

      (Thread/sleep 200)
      (is (not (realized? put-future))
          "put-mvar didn't block with filled mvar")

      (is (= initial-input (take-mvar mvar-val))
          "take-mvar didn't return put value")

      (Thread/sleep 200)
      (is (realized? put-future)
          "put-mvar didn't stop blocking after a take")))


  (testing "read-mvar"
    (let [initial-input 777
          input 666
          mvar-val (mvar initial-input)
          put-future (future (put-mvar mvar-val input))]

      (Thread/sleep 200)
      (is (not (realized? put-future))
          "put-mvar didn't block with filled mvar")

      (is (= initial-input (read-mvar mvar-val))
          "take-mvar didn't return put value")

      (Thread/sleep 200)
      (is (not (realized? put-future))
          "read-mvar unblocked thread doing put-mvar when it should not"))))
