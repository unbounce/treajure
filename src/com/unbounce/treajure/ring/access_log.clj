(ns com.unbounce.treajure.ring.access-log
  "Access logger based on extended NCSA Common Logging."
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [com.unbounce.treajure.ring :as tring])
  (:import java.text.MessageFormat
           java.util.concurrent.TimeUnit))

;; {0} client IP address
;;  -  rfc931 user ID
;; {1} authenticated username
;; {2} datetime the request ended
;; {3} host
;; {4} request info
;; {5} response status code
;; {6} response size in bytes
;; {7} referrer
;; {8} user-agent
;; {9} time taken to serve the request, in microseconds
;; {10} unique request ID
(def ^:const log-format
  "The MessageFormat used for formatting access log lines."
  "{0} - {1} [{2,date,dd/MMM/yyyy:HH:mm:ss Z}] {3} \"{4}\" {5} {6} \"{7}\" \"{8}\" {9} {10}")

(defn- blankable-string [v]
  (let [s (str v)]
    (if (str/blank? s) "-" s)))

;; follows http://www.w3.org/TR/WD-logfile.html
(defn- sanitize-string [s]
  (str/escape s {\" "\"\""}))

(defn -make-message
  "Only public for testing, not intended for direct use."
  [start-time-nanos request response auth-principal-fn]
  (let [end-time-nanos (System/nanoTime)
        end-time-millis (System/currentTimeMillis)
        process-time-micros (.toMicros
                              TimeUnit/NANOSECONDS
                              (- end-time-nanos start-time-nanos))
        client-ip (blankable-string
                    (tring/request-client-ip request))
        auth-principal (blankable-string (auth-principal-fn request response))
        host (first
               (str/split
                 (blankable-string
                   (get-in request [:headers "host"])) #":"))
        method (str/upper-case
                 (name
                   (get request :request-method :no-method)))
        path (blankable-string (:uri request))
        query-string (get request :query-string)
        protocol (tring/request-protocol request)
        request-info (str method " " path
                       (when query-string "?")
                       query-string " " protocol)
        response-status (get response :status 500)
        response-size (tring/response-size response)
        response-size-string (if (neg? response-size)
                               "-"
                               (str response-size))
        referrer (blankable-string
                   (get-in request [:headers "referer"]))
        user-agent (blankable-string
                     (get-in request [:headers "user-agent"]))
        request-id (blankable-string
                     (tring/request-id request))]

    (MessageFormat/format
      log-format
      (into-array
        Object
        [client-ip
         auth-principal
         end-time-millis
         host
         (sanitize-string request-info)
         response-status
         response-size-string
         (sanitize-string referrer)
         (sanitize-string user-agent)
         process-time-micros
         request-id]))))

(defn log-access
  "Ouputs an access log line. Typically not used directly but via the access-logger middleware."
  [start-time-nanos request response auth-principal-fn]
  {:pre [(integer? start-time-nanos)
         (map? request)
         (map? response)
         (fn? auth-principal-fn)]}
  (try
    (log/log
      'accesslog
      :info
      nil
      (-make-message
        start-time-nanos
        request
        response
        auth-principal-fn))
    (catch Throwable t
      (log/error t
        "Failed to log request:" request
        ", response: " response))))

(defn access-logger
  "Ring middleware that produces access logs line on the standard Clojure logger.

   Accepts the following options:

   :auth-principal-fn - a function that takes a Ring request and response
                        and returns the authenticated principal for the request
                        or nil if none.
                        The authenticated principal will be automatically cast
                        to string before logging by the str function.
                        Note that the response can be nil in case an exception
                        was thrown by the handler."

  [handler & [{:keys [auth-principal-fn]
               :or {auth-principal-fn
                    (constantly nil)}}]]
  {:pre [(fn? handler)
         (fn? auth-principal-fn)]}
  (fn -access-logger [request]
    (let [start-time-nanos (System/nanoTime)]
      (try
        (let [response (handler request)]
          (log-access
            start-time-nanos
            request
            response
            auth-principal-fn)
          response)
        (catch Throwable t
          (log-access
            start-time-nanos
            request
            {}
            auth-principal-fn)
          (throw t))))))
