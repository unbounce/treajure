(ns com.unbounce.treajure.map)

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn update-if-exists
  "Variation of clojure.core/update-in which performs no-operation if the
  key is not present in the map. "
  [m keys f & args]
  (let [value (get-in m keys ::not-found)]
    (if-not (identical? value ::not-found)
      (assoc-in m keys (apply f value args))
      m)))

(defn map-values
  "Create a new map of same keys and applying f to all the values.

  Taken from plumatic/plumbing.
  "
  [m f]
  (cond
    (sorted? m)
    (reduce-kv (fn [out-m k v] (assoc out-m k (f v))) (sorted-map) m)
    :else
    (persistent! (reduce-kv (fn [out-m k v] (assoc! out-m k (f v))) (transient {}) m))))
