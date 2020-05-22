(ns com.unbounce.treajure.monad
  (:refer-clojure :exclude [do])
  (:require [monads.core :refer [>>=] :as monads])
  (:import [monads.types Bind Return]))

(defmulti monadic-value? type)

(defmethod monadic-value? Bind [_]
  true)

(defmethod monadic-value? Return [_]
  true)

(defmethod monadic-value? :default [_]
  false)

(defmacro do [& instr]
  `(monads/mdo ~@instr))

(def return monads/return)

(defn bind-ignore
  "Executes monadic sub-routine withhout binding it's return value.

This is useful when you want to compose sub-routines that perform side-effects,
  and the return value is not needed.

Example:
>  (bind-ignore
>   (try-either (log/info \"performing random sub-routine\"))
>   (random-sub-routine 123))
"
  ([m1 m2]
   {:post [(monadic-value? %)]}
   (>>= m1 (fn -bind-ignore [_]
             m2)))
  ([m1 m2 & rest]
   {:post [(monadic-value? %)]}
   (reduce bind-ignore
           (bind-ignore m1 m2)
           rest)))

(def *> bind-ignore)
