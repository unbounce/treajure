(ns com.unbounce.treajure.map-test
  (:require [com.unbounce.treajure.map :refer :all]
            [clojure.test :refer :all]))

(deftest deep-merge-tests
  (testing "it merges nested maps"
    (is (= {:one {:two {:three "three"}}
            :uno {:dos "dos"}
            :eins "eins"
            :other {:nested {:value :meh}}}

           (deep-merge {:one {:two {:three 3}}
                        :uno {:dos 2}
                        :eins 1}
                       {:one {:two {:three "three"}}
                        :uno {:dos "dos"}
                        :eins "eins"
                        :other {:nested {:value :meh}}})))))
