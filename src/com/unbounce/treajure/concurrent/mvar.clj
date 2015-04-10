(ns com.unbounce.treajure.concurrent.mvar
  "Haskell's MVar abstraction in Clojure.

  Check: http://chimera.labs.oreilly.com/books/1230000000929/ch07.html for more
  info around MVars."
  (:import [java.util.concurrent Semaphore]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lightweight Performance mutable tuple implementation in Clojure
;; (Look ma! no atoms!)

(defprotocol IMCons
  (mcar [p])
  (mcdr [p])
  (set-mcar! [p val])
  (set-mcdr! [p val]))

(deftype MCons [^{:volatile-mutable true} car
                ^{:volatile-mutable true} cdr]
  IMCons
  (mcar [this] car)
  (mcdr [this] cdr)
  (set-mcar! [this val]
    (set! car val))
  (set-mcdr! [this val]
    (set! cdr val)))

(defn mcons [a b]
  (MCons. a b))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MVar abstraction

(declare new-empty-mvar new-mvar put-mvar take-mvar read-mvar)

(defn new-empty-mvar
  "Create an empty MVar"
  []
  (let [take-sem (Semaphore. 1)
        put-sem  (Semaphore. 1)]
    (.acquire take-sem)
    (mcons nil (mcons take-sem put-sem))))

(defn new-mvar
  "Creates an MVar which contains given val"
  [val]
  (let [mvar (new-empty-mvar)]
    (put-mvar mvar val)
        mvar))

(defn put-mvar
  "Puts a value inside the MVar, if MVar is full it will block current thread
  waiting for MVar to be empty."
  [mvar val]
  (let [take-sem (mcar (mcdr mvar))
        put-sem  (mcdr (mcdr mvar))]
    (.acquire put-sem)
    (set-mcar! mvar val)
    (.release take-sem)))

(defn take-mvar
  "Takes a value out of the MVar, if MVar is empty it will block current thread
  waiting for MVar to be filled."
  [mvar]
  (let [take-sem (mcar (mcdr mvar))
        put-sem  (mcdr (mcdr mvar))]
    (.acquire take-sem)
    (let [val (mcar mvar)]
      (set-mcar! mvar nil)
      (.release put-sem)
      val)))

(defn read-mvar
  "Reads value inside the MVar, if MVar is empty it will block current thread
  waiting for MVar to be filled. This *will not* remove the value from the MVar."
  [mvar]
  (let [take-sem (mcar (mcdr mvar))]
    (.acquire take-sem)
    (let [val (mcar mvar)]
      (.release take-sem)
      val)))
