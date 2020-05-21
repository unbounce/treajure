(ns com.unbounce.treajure.io
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:import [java.io OutputStream
                    ByteArrayOutputStream
                    FilterOutputStream]))

(defn- emit-and-reset-bytes!
  [^ByteArrayOutputStream baos emit-fn]
  (when (pos? (.size baos))
    (emit-fn (.toByteArray baos))
    (.reset baos)))

(defn split-emit-output-stream
  "Creates a java.io.OutputStream that acculates bytes until the specified character is met or until close are called.
   When one of these events occur, it emits the accumulated bytes to the provided function. An additional function
   can optionally be provided to be called after the OutputStream has been closed."
  (^OutputStream [split-char emit-fn]
   (split-emit-output-stream split-char emit-fn (fn [])))
  (^OutputStream [split-char emit-fn close-fn]
    {:pre  [(char? split-char)
            (fn? emit-fn)]
     :post [(instance? OutputStream %)]}

    (let [split-int (int split-char)
          open? (atom true)
          baos (ByteArrayOutputStream.)
          erb! #(emit-and-reset-bytes! baos emit-fn)

          seos (proxy [OutputStream] []

                 (write [c]
                   (locking baos
                     (when-not @open?
                       (throw
                         (IllegalStateException.
                           "The outputstream has been closed.")))
                     (.write baos (int c))
                     (when (= c split-int)
                       (erb!))))

                 (close []
                   (locking baos
                     (when @open?
                       (reset! open? false)
                       (erb!)
                       (close-fn)))))]

      ;; wrap the proxy in a FilterOutputStream to increase the compliance
      ;; to the Java API, thus avoiding issues related to the multiple
      ;; arities of the write method in OutputStream.
      (FilterOutputStream. seos))))


(defn slurp-bytes
  "Slurp the bytes from a slurpable thing.
   Lifted from: http://stackoverflow.com/a/26372677/387927"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream x) out)
    (.toByteArray out)))

(defn string-bytes-size
  "Get the size of a string in bytes, for the provided encoding.
   Returns -1 if the encoding is unknown, and logs an error."
  [^String string ^String encoding]
  {:pre [(or (nil? string) (string? string))
         (string? encoding)]}
  (if-not string
    0
    (try
      (alength (.getBytes string encoding))
      (catch Throwable t
        (log/error t
          "Failed to retrieve string bytes size with encoding:"
          encoding)
        -1))))
