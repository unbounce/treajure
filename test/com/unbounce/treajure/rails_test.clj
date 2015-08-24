(ns com.unbounce.treajure.rails-test
  (:require [clojure.test :refer :all]
            [com.unbounce.treajure.rails :as rails]))

(defonce secret "fake-secret-token")

(deftest session-token->user-id
  (testing "invalid format"
    (is (=
          (rails/session-token->user-id
            "bad stuff"
            secret)
          [nil "Invalid format"])))

  (testing "nil session"
    (is (=
          (rails/session-token->user-id
            nil
            secret)
          [nil "Invalid format"])))

  (testing "invalid session"
    (is (=
          (rails/session-token->user-id
            (str
              "aGVsbG8gd29ybGQ="
              "--2f3ce5e672f56139f0fa2b1b9d24a298b66f4e0c")
            secret)
          [nil "Invalid session"])))

  (testing "invalid signature"
    (is (=
          (rails/session-token->user-id
            (str
              "BAh7CEkiD3Nlc3Npb25faWQGOgZFRkkiJTI1ZjQ4YWI5NTk0N"
              "zRlMmY0ODU0YjEwNjE3NDExNDU1BjsAVEkiEF9jc3JmX3Rva2"
              "VuBjsARkkiMXp0SmExb2M4RWRkYUxzVXZxNHRpdEFPR0k5Y1l"
              "XdFlNd0pnSFBDNjNBaFU9BjsARkkiDHVzZXJfaWQGOwBGaTk="
              "--invalid")
            secret)
          [nil "Invalid signature"])))

  (testing "one byte user id"
    (is (=
          (rails/session-token->user-id
            (str
              "BAh7CEkiD3Nlc3Npb25faWQGOgZFRkkiJTI1ZjQ4YWI5NTk0N"
              "zRlMmY0ODU0YjEwNjE3NDExNDU1BjsAVEkiEF9jc3JmX3Rva2"
              "VuBjsARkkiMXp0SmExb2M4RWRkYUxzVXZxNHRpdEFPR0k5Y1l"
              "XdFlNd0pnSFBDNjNBaFU9BjsARkkiDHVzZXJfaWQGOwBGaTk="
              "--d231cf1a735df9c4048137580623a0a7f7e5f5fd")
            secret)
          [52 nil])))

  (testing "more than one byte user id"
    (is (=
          (rails/session-token->user-id
            (str
              "BAh7CUkiD3Nlc3Npb25faWQGOgZFRkkiJWIzMmUxM2QwZTA5O"
              "Tg4ZjlhNDRlNmI2YzhjNTk0ODZjBjsAVEkiEF9jc3JmX3Rva2"
              "VuBjsARkkiMTdKR1Z4ek1wc0dScktwZG9OdmpFMFl3K0NPT1d"
              "jaE1rU1dhL1ZVanBrQlU9BjsARkkiDHVzZXJfaWQGOwBGaQPI"
              "7whJIgpmbGFzaAY7AEZvOiVBY3Rpb25EaXNwYXRjaDo6Rmxhc"
              "2g6OkZsYXNoSGFzaAk6CkB1c2VkbzoIU2V0BjoKQGhhc2h7Bj"
              "oLbm90aWNlVDoMQGNsb3NlZEY6DUBmbGFzaGVzewY7CkkiG0x"
              "vZ2dlZCBpbiBzdWNjZXNzZnVsbHkGOwBGOglAbm93MA=="
              "--7fcd6e75ceaeaf9ae13576c960468ff949104913")
            secret)
          [585672 nil]))))
