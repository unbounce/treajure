(ns com.unbounce.treajure.io-test
  (:require [clojure.test :refer :all]
            [byte-streams :as bs]
            [com.unbounce.treajure.io :as io]))

(deftest split-emit-output-stream
  (let [emitted (atom [])
        seos (io/split-emit-output-stream
               \newline
               #(swap! emitted conj (bs/to-string %)))]

    (testing "Emission on split char"
      (.write seos 65)
      (.write seos (byte-array [66 67]))
      (.write seos (byte-array [66 67 68 69 70 71]) 2 2)
      (is (= @emitted []))
      (.write seos (byte-array [13 10]))
      (is (= @emitted ["ABCDE\r\n"]))
      (.write seos (byte-array [70 71]))
      (.write seos (byte-array [13 10]))
      (.write seos (byte-array [72 73]))
      (is (= @emitted ["ABCDE\r\n" "FG\r\n"])))))
