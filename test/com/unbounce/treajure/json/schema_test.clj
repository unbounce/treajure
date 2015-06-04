(ns com.unbounce.treajure.json.schema-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [com.unbounce.treajure.json.schema :as schema]
            [cheshire.core :as json]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :refer [not-found]]
            [ring.middleware.defaults :refer [wrap-defaults]])
  (:import  [java.net URI URL InetSocketAddress]
             java.io.File
             com.fasterxml.jackson.databind.ObjectMapper
             com.github.fge.jsonschema.main.JsonSchemaFactory))

(def test-port
  (Integer/valueOf
    (System/getProperty
      "treajure.test.http.port"
      "8080")))

(defn test-url [path]
  (str
    "http://localhost:" test-port
    path))

(def file-server
  (wrap-defaults
    (fn -errorer [req]
      {:status 500
       :headers {}
       :body "n/a"})
    {:static
     {:resources "jsonschema/draft-3/fixtures"}}))

(defn- with-http-file-server [f]
  (let [jetty (run-jetty
                file-server
                {:port test-port :join? false})]
    (f)
    (.stop jetty)))

(use-fixtures :once with-http-file-server)


;; -- schema helpers --

(defonce json-schema-factory
  (JsonSchemaFactory/byDefault))

(defonce ^:private object-mapper
  (ObjectMapper.))

(defn- same-schemas?
  [schema-1 schema-2]
  (=
    (json/parse-string schema-1)
    (json/parse-string schema-2)))

(defn- rsc->str [rsc]
  (slurp (io/resource rsc)))

(defn- rsc->json-node [rsc]
  (.readTree
    object-mapper
    (io/resource rsc)))


;; -- tests --

(deftest make-standalone
  (testing "relative schema uri"
    (testing "string URI"
      (is (thrown? AssertionError
            (schema/make-standalone "./test.json"))))
    (testing "URI"
      (is (thrown? AssertionError
            (schema/make-standalone (URI. "test.json"))))))

  (testing "idempotent when no ref"
    (testing "file system"
      (is (same-schemas?
            (schema/make-standalone
              (io/resource "jsonschema/draft-3/fixtures/base.json"))
            (rsc->str "jsonschema/draft-3/fixtures/base.json"))))

    (testing "HTTP"
      (is (same-schemas?
            (schema/make-standalone
              (test-url "/base.json"))
            (rsc->str "jsonschema/draft-3/fixtures/base.json")))))

  (testing "fragment extraction"
    (testing "file system"
      (is (same-schemas?
            (schema/make-standalone
              (str
                (io/resource "jsonschema/draft-3/fixtures/common/defs.json")
                "#/date_range"))
            (rsc->str "jsonschema/draft-3/expectations/date_range_only.json"))))
    (testing "HTTP"
      (is (same-schemas?
            (schema/make-standalone
              (test-url "/common/defs.json#/date_range"))
            (rsc->str "jsonschema/draft-3/expectations/date_range_only.json")))))

  (testing "internal refs"
    (testing "file system"
      (is (same-schemas?
            (schema/make-standalone
              (io/resource "jsonschema/draft-3/fixtures/common/defs.json"))
            (rsc->str "jsonschema/draft-3/expectations/defs.json"))))
    (testing "HTTP"
      (is (same-schemas?
            (schema/make-standalone
              (test-url "/common/defs.json"))
            (rsc->str "jsonschema/draft-3/expectations/defs.json")))))

  (testing "external refs"
    (testing "file system"
      (is (same-schemas?
            (schema/make-standalone
              (io/resource "jsonschema/draft-3/fixtures/refs.json"))
            (rsc->str "jsonschema/draft-3/expectations/refs.json"))))
    (testing "HTTP"
      (is (same-schemas?
            (schema/make-standalone
              (test-url "/refs.json"))
            (rsc->str "jsonschema/draft-3/expectations/refs.json")))))

  (testing "single inheritance"
    (testing "file system"
      (is (same-schemas?
            (schema/make-standalone
              (io/resource "jsonschema/draft-3/fixtures/extends_base.json"))
            (rsc->str "jsonschema/draft-3/expectations/extends_base.json"))))
    (testing "HTTP"
      (is (same-schemas?
            (schema/make-standalone
              (test-url "/extends_base.json"))
            (rsc->str "jsonschema/draft-3/expectations/extends_base.json")))))

  (testing "multiple inheritance"
    (testing "file system"
      (is (same-schemas?
            (schema/make-standalone
              (io/resource "jsonschema/draft-3/fixtures/extends_extends_base.json"))
            (rsc->str "jsonschema/draft-3/expectations/extends_extends_base.json"))))
    (testing "HTTP"
      (is (same-schemas?
            (schema/make-standalone
              (test-url "/extends_extends_base.json"))
            (rsc->str "jsonschema/draft-3/expectations/extends_extends_base.json")))))

  (testing "inheritance and refs"
    (testing "file system"
      (is (same-schemas?
            (schema/make-standalone
              (io/resource "jsonschema/draft-3/fixtures/extends_base_with_refs.json"))
            (rsc->str "jsonschema/draft-3/expectations/extends_base_with_refs.json"))))
    (testing "HTTP"
      (is (same-schemas?
            (schema/make-standalone
              (test-url "/extends_base_with_refs.json"))
            (rsc->str "jsonschema/draft-3/expectations/extends_base_with_refs.json"))))))

(deftest standalone-schema
  (let [schema-str (schema/make-standalone
                     (test-url "/extends_base_with_refs.json"))
        schema-file (File/createTempFile
                      "trj-schema-"
                      ".json")
        _ (spit schema-file schema-str)
        schema (.getJsonSchema
                 json-schema-factory
                 (str (.toURI schema-file)))]

    (is (.validInstanceUnchecked
          schema
          (rsc->json-node "json/valid.json")))

    (is (not (.validInstanceUnchecked
               schema
               (rsc->json-node "json/invalid-string-value.json"))))

    (is (not (.validInstanceUnchecked
               schema
               (rsc->json-node "json/invalid-missing-required.json"))))

    (.deleteOnExit schema-file)))
