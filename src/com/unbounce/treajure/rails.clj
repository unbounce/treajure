(ns com.unbounce.treajure.rails
  (:require [clojure.string :as str])
  (:import java.net.URLDecoder
           java.util.Collections
           javax.crypto.Mac
           javax.crypto.spec.SecretKeySpec
           javax.xml.bind.DatatypeConverter))

(defonce ^:private encoding "US-ASCII")
(defonce ^:private algorithm "HmacSHA1")

(defn- string->bytes
  [s]
  (.getBytes s encoding))

(defonce ^:private user-id-field-marker
  (seq
    (string->bytes
      "user_id\u0006;\u0000Fi")))

(def ^:private secret->key-spec
  (memoize
    (fn -secret->key-spec
      [secret]
      (SecretKeySpec.
        (string->bytes
          secret)
        algorithm))))

(defn- bytes->hex-string
  [bs]
  (DatatypeConverter/printHexBinary bs))

(defn- b64-string->bytes
  [b64-string]
  (DatatypeConverter/parseBase64Binary b64-string))

(defn- aindex-of
  [haystack needle]
  (let [index (Collections/indexOfSubList haystack needle)]
    (when-not (neg? index)
      index)))

(defn- n-bytes->number
  [bs size]
  (reduce
    (fn -n-bytes->number
      [acc [b i]]
      (+ acc
        (bit-shift-left
          (short (bit-and b 0xFF))
          (* i 8))))
    0
    (partition
      2
      (interleave
        (take size bs)
        (range)))))

(defn- get-user-id
  [session-bs0]
  (if-let [user-id-index (aindex-of
                           session-bs0
                           user-id-field-marker)]
    (let [session-bs (nthrest
                       session-bs0
                       (+ user-id-index
                         (count user-id-field-marker)))
          long-size (first session-bs)]
      ;; fight with ruby's marshalling "strategy" for longs
      (cond
        ;; long size is not a size at all in these cases...
        (zero? long-size) [0 nil]
        (>= long-size 5)  [(- long-size 5) nil]
        (< long-size -4)  [(+ long-size 5) nil]
        ;; otherwise, long size is actually a size! w00t!
        :else             [(n-bytes->number
                             (rest session-bs)
                             long-size) nil]))
    [nil "Invalid session"]))

(defn- session->signature
  [session-b64 secret]
  (bytes->hex-string
    (let [mac (Mac/getInstance algorithm)
          _ (.init mac (secret->key-spec secret))]
      (.doFinal
        mac
        (string->bytes session-b64)))))

(defn- decode-session
  [session signature secret]
  (let [session-b64 (URLDecoder/decode session encoding)
        expected-signature (session->signature session-b64 secret)]
    (if (= (str/lower-case signature)
           (str/lower-case expected-signature))
      (get-user-id
        (seq
          (b64-string->bytes session-b64)))
      [nil "Invalid signature"])))

(defn session-token->user-id
  "Extracts the user ID from a Rails session token.
   Returns a vector of:

   [user-id nil]      - if the user ID can be extracted
   [nil error-string] - if the user ID can not be extracted"
  [token secret]

  {:pre [(or (nil? token) (string? token))
         (and (string? secret) (pos? (count secret)))]
   :post [(= (count %) 2)
          (or (nil? (first %)) (integer? (first %)))
          (or (nil? (second %)) (string? (second %)))]}

  (let [[_ session signature]
        (re-matches #"^([^-]+)--([^-]+)$" (str token))]
    (if (and session signature)
      (try
        (decode-session
          session
          signature
          secret)
        (catch Throwable t
          [nil (str "Failed to decode session: " t)]))
      [nil "Invalid format"])))
