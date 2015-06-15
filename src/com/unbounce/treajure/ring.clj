(ns com.unbounce.treajure.ring
  "Helpers for dealing with Ring requests and responses."
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [com.unbounce.treajure.io :as tio]
            [ring.util.request :as request]
            [ring.util.parsing :refer [re-value]])
  (:import  [java.io File InputStream]
             clojure.lang.ISeq))

(defonce
  ^{:doc "Defines the default encoding used by Ring middlewares."}
   default-encoding "UTF-8")

(def ^:private charset-pattern
  (re-pattern (str ";(?:.*\\s)?(?i:charset)=(" re-value ")\\s*(?:;|$)")))

(defn- last-forwarded [x-forwarded-for]
  (str/trim
    (last
      (str/split x-forwarded-for #","))))

(defn request-client-ip
  "Retrieves the IP address (String) of the client that sent the request, taking the X-Forwarded-For header into account."
  [request]
  (if-let [x-forwarded-for (get-in request [:headers "x-forwarded-for"])]
    (last-forwarded x-forwarded-for)
    (:remote-addr request)))

(defn request-scheme
  "Retrieves the scheme (keyword) of the request, taking the X-Forwarded-Proto header into account."
  [request]
  (keyword
    (get-in
      request
      [:headers "x-forwarded-proto"]
      (:scheme request))))

(defn request-protocol
  "Retrieves the scheme (keyword) of the request, defaulting to HTTP/1.1 if the :protocol entry is absent."
  [request]
  (get request :protocol "HTTP/1.1"))

(defn- request-port
  [request scheme host-bits server-port]
  (if-let [^String x-forwarded-port (get-in
                                      request
                                      [:headers "x-forwarded-port"])]

  (Integer/valueOf x-forwarded-port)

  (if (> (count host-bits) 1)
    (Integer/valueOf ^String (second host-bits))
    (case scheme
      :http 80
      :https 443
      server-port))))

(defn- bail-with-bad-host
  ([request]
    (bail-with-bad-host request nil))

  ([request cause]
    (throw
      (IllegalArgumentException.
        (str
          "Bad Host header for request: "
          request)
        cause))))

(defn request-scheme-host-port
  "Retrieves a vector of [^keyword scheme ^String host ^Integer port] for the request, taking the X-Forwarded-Proto and X-Forwarded-Port headers into account."
  [request]
  (let [scheme (request-scheme request)
        server-port (:server-port request)
        host-bits (str/split
                    (get-in
                      request
                      [:headers "host"]
                      (str "localhost:" server-port))
                    #":")
        host (first host-bits)]

    (if
      (or
        (str/blank? host)
        (> (count host-bits) 2))

      (bail-with-bad-host request)

      (try
        [scheme
         host
         (request-port
           request
           scheme
           host-bits
           server-port)]
        (catch Throwable t
          (bail-with-bad-host request t))))))

(defn request-base-uri
  "Retrieves the base URI (String) of the request, taking into account all X- headers.
   The base URI is basically the scheme://host:port of the request originally sent by the browser."
  [request]
  (let [[scheme host port] (request-scheme-host-port request)]
    (str (name scheme) "://" host ":" port)))

(defn strip-charset
  "Removes the charset attribute of a media-type string, if present."
  [media-type]
  (when media-type
    (str/trim
      (first
        (str/split media-type #";")))))

(defn request-id
  "Retrieves the unique ID of the request."
  [request]
  (get request :request-id))

(defn request-encoding
  "Retrieves the character encoding of the request, using the default-encoding if the request doesn't specify it."
  [request]
  (or
    (request/character-encoding request)
    default-encoding))

(defn request-bytes
  "Retrieves a byte-array representation of the request. The input-stream body of the request will be consumed and will not be readable anymore."
  [request]
  (let [body (:body request)
        ^String encoding (request-encoding request)]
    (when body
      (.getBytes
        ^String (slurp body :encoding encoding)
        encoding))))

(defn body-string
  "Retrieves a String representation of the request, using the request-encoding for byte deserialization. The input-stream body of the request will be consumed and will not be readable anymore."
  [request]
  (let [body (:body request)
        encoding (request-encoding request)]
    (condp instance? body
      String body
      InputStream (slurp body :encoding encoding)
      nil)))

;; From: https://github.com/ring-clojure/ring/blob/master/ring-core/src/ring/util/request.clj#L33 (adapted for response)
(defn response-encoding
  "Retrieves the character encoding of the response, using the default-encoding if the request doesn't specify it."
  [response]
  (or
    (if-let [type (get-in response [:headers "Content-Type"])]
      (second (re-find charset-pattern type)))
    default-encoding))

(defn response-size
  "Retrieves the size of the response.
   If the response does not contain a Content-Length headers, the size will be estimated from the body value.
   This estimated size may be inaccurate for streaming payload if their InputStream implementation returns an inaccurate value for calls to .available().
   Returns a negative value if an issue occurred during the size calculation."
  [response]
  ; standard ring response middlewares use canonical casing, like: Content-Length
  (let [length (get-in response [:headers "Content-Length"])
        body (:body response)
        encoding (or (response-encoding response) default-encoding)]
    (if length
      (read-string length)
      (condp instance? body
        String (tio/string-bytes-size body encoding)
        ISeq (reduce + (map #(tio/string-bytes-size % encoding) body))
        File (.length ^File body)
        InputStream (.available ^InputStream body)
        -1))))

(defn response-bytes
  "Retrieves a byte-array representation of the response. If the response has an input-stream body, it will be consumed and will not be readable anymore."
  [response]
  (let [body (:body response)
        encoding (or (response-encoding response) default-encoding)]
    (condp instance? body
      String (.getBytes ^String body ^String encoding)
      ISeq (.getBytes ^String (str/join body) ^String encoding)
      File (tio/slurp-bytes body)
      InputStream (tio/slurp-bytes body)
      nil)))

