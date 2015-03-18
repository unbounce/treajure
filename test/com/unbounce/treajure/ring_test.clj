(ns com.unbounce.treajure.ring-test
    (:require [clojure.test :refer :all]
              [clojure.java.io :as io]
              [com.unbounce.treajure.ring :as tring]
              [me.raynes.fs :as fs]))

(def ^:dynamic *test-file* nil)

(defonce ^:private test-file-contents "this is a test file")
(defonce ^:private test-stream-contents "streamed data")

(defn- with-app [f]
  (let [test-file (fs/temp-file "treajure" "test")]
    (spit test-file test-file-contents)
    (binding [*test-file* test-file]
      (f))
    (fs/delete test-file)))

(use-fixtures :once with-app)

(deftest request-client-ip
  (are [a e] (= a e)
    (tring/request-client-ip {:remote-addr "1.2.3.4"}) "1.2.3.4"
    (tring/request-client-ip {:remote-addr "1.2.3.4"}) "1.2.3.4"
    (tring/request-client-ip {:remote-addr "1.2.3.4" :headers {"x-forwarded-for" "5.6.7.8"}}) "5.6.7.8"
    (tring/request-client-ip {:remote-addr "1.2.3.4" :headers  {"x-forwarded-for" "5.6.7.8, 9.10.11.12 "}}) "9.10.11.12"))

(deftest request-scheme
  (are [a e] (= a e)
    (tring/request-scheme {:scheme :http }) :http
    (tring/request-scheme {:scheme :https}) :https
    (tring/request-scheme {:scheme :https :headers {"x-forwarded-proto" "http"}}) :http
    (tring/request-scheme {:scheme :http  :headers  {"x-forwarded-proto" "https"}}) :https))

(deftest request-scheme-host-port
  (is
    (thrown? IllegalArgumentException
      (tring/request-scheme-host-port {:scheme :http :headers {"host" "wrong:123:456"}})))
  (is
    (thrown? IllegalArgumentException
      (tring/request-scheme-host-port {:scheme :http :headers {"host" "wrong:123/"}})))
  (is
    (thrown? IllegalArgumentException
      (tring/request-scheme-host-port {:scheme :http :headers {"host" "http://wrong"}})))
  (is
    (thrown? IllegalArgumentException
      (tring/request-scheme-host-port {:scheme :http :headers {"host" "http://wrong:123"}})))

  (are [a e] (= a e)
    (tring/request-scheme-host-port {:scheme :http  :headers {"host" "test"}}) [:http "test" 80]
    (tring/request-scheme-host-port {:scheme :https :headers {"host" "test"}}) [:https "test" 443]
    (tring/request-scheme-host-port {:scheme :http  :headers {"host" "test:8080"}}) [:http "test" 8080]
    (tring/request-scheme-host-port {:scheme :https :headers {"host" "test:8443"}}) [:https "test" 8443]
    (tring/request-scheme-host-port {:scheme :http  :headers {"host" "test" "x-forwarded-port" "9080"}}) [:http "test" 9080]
    (tring/request-scheme-host-port {:scheme :https :headers {"host" "test" "x-forwarded-port" "9443"}}) [:https "test" 9443]
    (tring/request-scheme-host-port {:scheme :http  :headers {"host" "test:80" "x-forwarded-port" "9080"}}) [:http "test" 9080]
    (tring/request-scheme-host-port {:scheme :https :headers {"host" "test:8443" "x-forwarded-port" "9443"}}) [:https "test" 9443]
    (tring/request-scheme-host-port {:scheme :foo   :server-port 123 :headers {"host" "test"}}) [:foo "test" 123]))

(deftest request-base-uri
  (are [a e] (= a e)
    (tring/request-base-uri {:scheme :http  :server-port 8080}) "http://localhost:8080"
    (tring/request-base-uri {:scheme :https :server-port 8080}) "https://localhost:8080"
    (tring/request-base-uri {:scheme :http  :server-port 8080 :headers {"host" "test"}}) "http://test:80"
    (tring/request-base-uri {:scheme :https :server-port 8080 :headers {"host" "test"}}) "https://test:443"
    (tring/request-base-uri {:scheme :http  :server-port 8080 :headers  {"host" "test:8181"}}) "http://test:8181"
    (tring/request-base-uri {:scheme :http  :server-port 8080 :headers  {"host" "test:8181" "x-forwarded-proto" "https"}}) "https://test:8181"))

(deftest strip-charset
  (are [a e] (= a e)
    (tring/strip-charset nil) nil
    (tring/strip-charset "") ""
    (tring/strip-charset "text/html") "text/html"
    (tring/strip-charset "text/html; charset=UTF-16") "text/html"))

(defn- bytes-seq [e]
  (when e (seq (.getBytes e))))

(deftest request-bytes
  (with-open [r (io/input-stream (.getBytes test-stream-contents))]
    (are [a e] (= (seq a) (bytes-seq e))
      (tring/request-bytes {}) nil
      (tring/request-bytes {:body nil}) nil
      (tring/request-bytes {:body r}) test-stream-contents)))

(deftest response-encoding
  (are [a e] (= a e)
    (tring/response-encoding {}) tring/default-encoding
    (tring/response-encoding {:headers {"Content-Type" "text/html"}}) tring/default-encoding
    (tring/response-encoding {:headers {"Content-Type" "text/html; charset=UTF-16"}}) "UTF-16"))

(deftest response-size
  (with-open [r (io/input-stream (.getBytes test-stream-contents))]
    (are [a e] (= a e)
      (tring/response-size {}) -1
      (tring/response-size {:body :crap}) -1
      (tring/response-size {:body "abc" :headers {"Content-Type" "text/html; charset=UTF-42"}}) -1
      (tring/response-size {:headers {"Content-Length" "123"}}) 123
      (tring/response-size {:body "abc"}) 3
      (tring/response-size {:body "abc" :headers {"Content-Type" "text/html; charset=ISO-8859-4"}}) 3
      (tring/response-size {:body '("abc" "def")}) 6
      (tring/response-size {:body *test-file*}) (count test-file-contents))
      (tring/response-size {:body r}) (count test-stream-contents)))

(deftest response-bytes
  (with-open [r (io/input-stream (.getBytes test-stream-contents))]
    (are [a e] (= (seq a) (bytes-seq e))
      (tring/response-bytes {}) nil
      (tring/response-bytes {:body :crap}) nil
      (tring/response-bytes {:headers {"Content-Length" "123"}}) nil
      (tring/response-bytes {:body "abc"}) "abc"
      (tring/response-bytes {:body "abc" :headers {"Content-Type" "text/html; charset=ISO-8859-4"}}) "abc"
      (tring/response-bytes {:body '("abc" "def")}) "abcdef"
      (tring/response-bytes {:body *test-file*}) test-file-contents
      (tring/response-bytes {:body r}) test-stream-contents)))
