(ns com.unbounce.treajure.io-test
  (:require [clojure.test :refer :all]
            [byte-streams :as bs]
            [com.unbounce.treajure.io :as io]))

(deftest split-emit-output-stream
  (let [emitted (atom [])
        seos (io/split-emit-output-stream
               \newline
               #(swap! emitted conj (bs/to-string %)))]

    (testing "Emit on split char"
      (.write seos 65)
      (.write seos (byte-array [66 67]))
      (.write seos (byte-array [66 67 68 69 70 71]) 2 2)
      (is (= @emitted []))
      (.write seos (byte-array [13 10]))
      (is (= @emitted ["ABCDE\r\n"]))
      (.write seos (byte-array [70 71]))
      (.write seos (byte-array [0xE2 0x99 0xA1]))
      (.write seos (byte-array [13 10]))
      (.write seos (byte-array [72 73]))
      (is (= @emitted ["ABCDE\r\n" "FG\u2661\r\n"])))

    (reset! emitted [])

    (testing "Emit on close"
      (.write seos (byte-array [74 75]))
      (is (= @emitted []))
      (.close seos)
      ;; we still have 72 73 from the previous
      ;; interactions in the buffer
      (is (= @emitted ["HIJK"])))

    (testing "Writing after close"
      (is
        (thrown? IllegalStateException
                 (.write seos 65))))

    (testing "Call close-fn on close"
      (let [closed (atom false)
            seos (io/split-emit-output-stream
                   \newline
                   #()
                   #(reset! closed true))]
        (.close seos)
        (is (= @closed true))))))
