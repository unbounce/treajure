(ns com.unbounce.treajure.config
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
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
  (edn-env-reader [[name def & _]]
    (assert (instance? String def)
            (str "Default value for key `" name "' must be a String"))
    (if-let [val (System/getenv name)]
      (do
        (log/info "Using value from ENV variable " name)
        val)
      def)))

(defmulti fetch-config
  "Fetches configuration file from `source'.

`source' can be either a result from `env-var' or `filepath' functions. In case
  `env-var' is used, this env var must hold the path of the filepath that
  contains the configuration."
  (fn [source]
    (assert (instance? ConfigPath source))
    (:type source)))

(defn- fetch-file-config [path]
  (let [f (io/file path)]
    (when (.exists f)
      f)))

(defn- get-file-fetcher [path]
  (if (re-find #"^/" path)
    ;; it's an absolute path
    fetch-file-config
    ;; it's on classpath
    io/resource))

(defmethod fetch-config :envvar [{:keys [val default]}]
  (fetch-config (filepath (or (System/getenv val)
                              default))))

(defmethod fetch-config :filepath [{:keys [val]}]
  (assert (re-find #"\.edn$"
                   val)
          "Configuration file must have an .edn extension")

  (let [resource-fn (get-file-fetcher val)]
    (if-let [resource (resource-fn val)]
      (with-open [h (java.io.PushbackReader.
                     (io/reader resource))]
        (edn/read {:readers {'env edn-env-reader}} h)))))
