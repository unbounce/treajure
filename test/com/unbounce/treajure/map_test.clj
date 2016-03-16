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

(deftest update-if-exists-tests
  (let [sample-object {:foo {:bar 0}}]
    (testing "updates a nested object"
      (is (= (update-if-exists sample-object
               [:foo :bar] inc)
             {:foo {:bar 1}})))
    (testing "updates a nested object"
      (is (= (update-if-exists sample-object
               [:foo :bar] inc)
             {:foo {:bar 1}})))
    (testing "does not update a misisng key"
      (is (= (update-if-exists sample-object [:foo :bar] + 3 4 5)
             {:foo {:bar 12}})))))

(deftest map-values-test
  (testing "sorted map"
    (is (= {:a 1 :b 1} (map-values (sorted-map :a 0 :b 0) inc)))
    (is (sorted? (map-values (sorted-map :a 0 :b 0) inc))))
  (testing "regular map"
    (let [sample-object {:foo 2 :bar 3}]
      (is (= (map-values sample-object inc)
             {:foo 3 :bar 4})))))
