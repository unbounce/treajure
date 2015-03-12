(ns com.unbounce.treajure.io
  (:import [java.io OutputStream
                    ByteArrayOutputStream
                    FilterOutputStream]))

(defn- emit-and-reset-bytes! [baos emit-fn]
  (when (> (.size baos) 0)
    (emit-fn (.toByteArray baos))
    (.reset baos)))

(defn split-emit-output-stream
  "This class is an outputstream that acculates bytes until the specified character is met or until close are called.
   When one of these events occur, it emits the accumulated bytes to the provided function."
  [split-char emit-fn]
    {:pre  [(char? split-char)
            (fn? emit-fn)]
     :post [(instance? OutputStream %)]}

    (let [split-int (int split-char)
          open? (atom true)
          baos (ByteArrayOutputStream.)
          erb! #(emit-and-reset-bytes! baos emit-fn)

          seos (proxy [java.io.OutputStream] []

                 (write [c]
                   (locking baos
                     (when-not @open?
                       (throw
                         (IllegalStateException.
                           "The outputstream has been closed.")))
                     (.write baos c)
                     (when (= c split-int)
                       (erb!))))

                 (close []
                   (locking baos
                     (when @open?
                       (reset! open? false)
                       (erb!)))))]

      ;; wrap the proxy in a FilterOutputStream to increase the compliance
      ;; to the Java API, thus avoiding issues related to the multiple
      ;; arities of the write method in OutputStream.
      (FilterOutputStream. seos)))
