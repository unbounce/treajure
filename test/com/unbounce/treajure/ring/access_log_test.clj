(ns com.unbounce.treajure.ring.access-log-test
  (:require [clojure.test :refer :all]
    [com.unbounce.treajure.ring.access-log :as al]))

(deftest make-message
  (testing "log message for empty request/response"
    (let [message (al/-make-message
                    (System/nanoTime)
                    {}
                    {}
                    (constantly nil))]
      (is
        (re-matches
          #"^- - - \[\d{2}/\S{3}/\d{4}:\d{2}:\d{2}:\d{2} [\+\-]\d{4}\] - \"NO-METHOD - HTTP/1\.1\" 500 - \"-\" \"-\" \d+ - -$"
          message))))

  (testing "log message for full request/response"
    (let [message (al/-make-message
                    (System/nanoTime)
                    {:scheme :http
                     :server-port 8080
                     :request-method :get
                     :protocol "HTTP/1.0"
                     :uri "/fake-path"
                     :query-string "fake-query"
                     :headers {"host" "acme.com"
                               "referer" "ecma.com"
                               "user-agent" "SpamBot"
                               "x-forwarded-for" "10.10.10.10"
                               "x-forwarded-proto" "https"}
                     :request-id "fake-request-id"}
                    {:status 200
                     :body "ten chars!"}
                    (constantly 123456))]
      (is
        (re-matches
          #"^10\.10\.10\.10 - 123456 \[\d{2}/\S{3}/\d{4}:\d{2}:\d{2}:\d{2} [\+\-]\d{4}\] acme\.com \"GET /fake-path\?fake-query HTTP/1\.0\" 200 10 \"ecma\.com\" \"SpamBot\" \d+ fake-request-id https$"
          message)))))
