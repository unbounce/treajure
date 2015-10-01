(ns com.unbounce.treajure.java)

(defprotocol ToClojure
  (->clj [o]))

(extend-protocol ToClojure
  java.util.Map
  (->clj [o] (let [entries (.entrySet o)]
               (reduce (fn [m [k v]]
                         (assoc m k (->clj v)))
                       {} entries)))

  java.util.List
  (->clj [o] (vec (map ->clj o)))

  java.lang.Object
  (->clj [o] o)

  nil
  (->clj [_] nil))


;; Java collections utils
(defn in? [^Object key ^Collection collection]
  (.contains collection key))

