(ns com.unbounce.treajure.json.schema
  "Utilities for dealing with JSON schemas."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [clojure.tools.logging :as log]
            [cheshire.core :as json])
  (:import  [java.net URI URL]))

(def ^:private ^:dynamic *cache*
  "A cache of uri->schema-map entries"
  nil)

(declare ^:private load-and-process)

(defmulti ^:private ^URI any->uri
  (fn [v] [(class v)]))

(defmethod ^:private any->uri [URI] [^URI v]
  v)

(defmethod ^:private any->uri [URL] [^URL v]
  (.toURI v))

(defmethod ^:private any->uri [String] [v]
  (URI. v))

(defn- load-schema
  [uri]
  (if-let [cached-schema-map (get @*cache* uri)]
    cached-schema-map
    (let [schema-map (json/parse-stream
                     (io/reader uri))]
      (swap! *cache* assoc uri schema-map)
      (log/debug "Loaded and cached:" uri)
      schema-map)))

(defn- get-schema-fragment
  [schema-map fragment]
  (get-in
    schema-map
    (remove
      str/blank?
      (str/split fragment #"/"))))

(defn- resolve-ref
  [^URI uri schema-map ^String $ref]
  (log/debug
    "Resolving ref:" $ref
    "for URI:" uri)
  (if-let [fragment (second (re-matches #"^#(.*)" $ref))]
    ; internal # reference
    (get-schema-fragment
      schema-map
      fragment)
    ; external # reference
    (load-and-process
      (.resolve uri $ref))))

(defn- process-schema-map-entry
  [uri schema-map [k v]]
  (if
    (= k "$ref")
    (resolve-ref uri schema-map v)
    [k v]))

(defn- process-schema-form
  [uri schema-map x]
  (if (map? x)
    (into
      {}
      (map
        (partial
          process-schema-map-entry
          uri
          schema-map)
        x))
    x))

(defn- deep-merge
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn- mixin-parent
  [uri schema-map]
  (if-let [parent-$ref (get-in schema-map ["extends" "$ref"])]
    (deep-merge
      (resolve-ref
        uri
        schema-map
        parent-$ref)
      (dissoc schema-map "extends"))
    schema-map))

(defn- load-and-process-schema
  [uri]
  (let [schema-map (mixin-parent
                     uri
                     (load-schema uri))]
    (postwalk
      (partial
        process-schema-form
        uri
        schema-map)
      schema-map)))

(defn- load-and-process [^URI uri]
  (let [schema-map (load-and-process-schema uri)
        fragment (.getFragment uri)]
    (if fragment
      (get-schema-fragment
        schema-map
        fragment)
      schema-map)))

(defn- init-cache-content
  [uri schema]
  (if-not schema
    {}
    {uri (json/parse-string schema)}))

(defonce ^{:doc "Supported output format"} standalone-output-as
  #{:string :map})

(defn make-standalone
  "Outputs a new schema where all transitive references have been resolved.
   Supported options:

   :output-as - a keyword that specifies the result format.
                default is :string
   :schema    - a string that contains the preloaded schema at the specified URI.
                keys must be strings!"
  [schema-uri & {:keys [output-as schema]
                 :or {output-as :string
                      schema nil}}]
  {:pre [(or
           (nil? schema)
           (string? schema))
         (or
           (instance? URI schema-uri)
           (instance? URL schema-uri)
           (string? schema-uri))
         (contains? standalone-output-as output-as)]
   :post [(or
            (string? %)
            (map? %))]}

  (let [uri (.normalize (any->uri schema-uri))]

    (assert (.isAbsolute uri)
      "Schema URI must be absolute")

    (log/debug
      "Making standalone:"
      schema-uri)

    (binding [*cache* (atom (init-cache-content uri schema))]
      (let [result (load-and-process uri)]
        (case output-as
          :string (json/generate-string result)
          :map result)))))
