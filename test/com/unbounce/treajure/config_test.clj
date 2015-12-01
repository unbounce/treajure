(ns com.unbounce.treajure.config-test
  (:require [com.unbounce.treajure.config :as config]
            [clojure.test :refer :all])
  (:import [com.unbounce.treajure.config ConfigPath]))


(deftest config-tests

  (let [path (config/filepath "treajure_config.edn")]

    (testing "filepath returns a ConfigPath"
      (is (instance? ConfigPath path)))

    (let [config (config/fetch-config path)]

      (testing "returns configuration map"
        (is (not (nil? config))))

      (testing "returns var-env value"
        (is  (= (System/getenv "PATH")
                (:option-1 config))))

      (testing "returns non-nil value for key entries with defaults"
        (is (= "option_2" (:option-2 config)))))))
