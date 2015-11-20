(ns com.unbounce.treajure.config
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(defrecord ConfigPath
    [type val default])

(defn env-var
  "Builds a `ConfigPath' that specifies the configuration filepath is OS VAR ENV
  with name `var-name'. If VAR ENV is null, uses `default-filename'"
  [var-name default-filename]
  {:post [(instance? ConfigPath %)]}
  (->ConfigPath :envvar (str/upper-case var-name)
                default-filename))

(defn filepath
  "Builds a `ConfigPath' that specifies the configuration filepath"
  [filepath]
  {:post [(instance? ConfigPath %)]}
  (->ConfigPath :filepath filepath
                nil))

(defprotocol EDNEnvReader
  (edn-env-reader [self]))

(extend-protocol EDNEnvReader
  java.lang.String
  (edn-env-reader [name]
    (System/getenv name))

  clojure.lang.PersistentVector
  (edn-env-reader [[name def]]
    (assert (instance? String def))
    (or (System/getenv name)
        def)))

(defmulti fetch-config
  "Fetches configuration file from `source'.

`source' can be either a result from `env-var' or `filepath' functions. In case
  `env-var' is used, this env var must hold the path of the filepath that
  contains the configuration."
  (fn [source]
    {:pre [(instance? ConfigPath source)]}
    (:type source)))

(defmethod fetch-config :envvar [{:keys [val default]}]
  (fetch-config (filepath (or (System/getenv val)
                              default))))

(defmethod fetch-config :filepath [{:keys [val]}]
  (->> val
       io/resource
       io/reader
       java.io.PushbackReader.
       (edn/read {:readers {'env edn-env-reader}})))
