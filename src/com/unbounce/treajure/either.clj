(ns com.unbounce.treajure.either
  (:require [monads.core :as monads]
            [monads.error :refer [error-m]]
            [monads.types]
            [com.unbounce.treajure.monad :refer [monadic-value?]]))

(defn run-either
  "Evaluates a monadic sub-routine of type Either"
  [action]
  (monads/run-monad error-m action))

(defn left
  "Wraps val on an Either's left value, normally corresponds to an error value"
  [val]
  (monads.types/left val))

(defn right
  "Wraps val on an Either's right value"
  [val]
  (monads.types/right val))

(defn either
  "Evaluates `val0' a monadic sub-routine of type Either, and calls `on-left' or
  `on-right' depending on the resulting Either value"
  [on-left on-right val0]
  (let [val (if (monadic-value? val0)
              (run-either val0)
              val0)]
    (monads.types/either on-left on-right val)))

(defn right?
  "Checks if `val' corresponds to an Either's right value.
If it receives a monadic sub-routine it _will_ evaluate it, careful if your
monadic sub-routine performs side-effects)"
  [val]
  (if (monadic-value? val)
    (monads.types/right? (run-either val))
    (monads.types/right? val)))

(defn left?
  "Checks if `val' corresponds to an Either's left value.
If it receives a monadic sub-routine it _will_ evaluate it, careful if your
monadic sub-routine performs side-effects)"
  [val]
  (if (monadic-value? val)
    (monads.types/left? (run-either val))
    (monads.types/left? val)))

(defn left-map
  "Calls `f' on the wrapped Either value when wrapper is a `left'"
  [f val]
  (either #(left (f %))
          right
          val))

(defmacro try-either
  "Performs a call that might fail and wraps its result on an Either.

When the call throws an exception, the exception is wrapped on a `left' value;
  otherwise a `right' value is returned.

When an `on-failure' function is given, it will be called with the received
  exception, and the returned value is returned wrapped on a `left' value"
  ([io-action]
   `(try
      (let [result# ~io-action]
        (right result#))
      (catch Exception e#
        (left e#))))
  ([io-action on-failure]
   `(try
      (let [result# ~io-action]
        (right result#))
      (catch Exception e#
        (left (~on-failure e#))))))
