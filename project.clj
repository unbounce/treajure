(defproject com.unbounce/treajure "1.0.5-SNAPSHOT"
  :description "A treasure trove of Clojure goodness."
  :url "https://www.github.com/unbounce/treajure"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"
            :comments "Copyright (c) 2015 Unbounce Marketing Solutions Inc."}

  :profiles {:dev {:plugins [[lein-kibit "0.1.2"]
                             [jonase/eastwood "0.2.1"]]

                   :dependencies [[byte-streams "0.2.0"]
                                  [me.raynes/fs "1.4.6"]
                                  [ring/ring-core "1.3.2"]
                                  [cheshire "5.5.0"]
                                  [ring/ring-jetty-adapter "1.3.2"]
                                  [ring/ring-defaults "0.1.5"]
                                  [com.github.fge/json-schema-validator "2.2.6"]]}

             :test {:resource-paths ["test-resources"]}}

  :dependencies
  [
   [org.clojure/clojure "1.6.0"]
  ]

  :release-tasks [["vcs" "assert-committed"]
                  ["clean"]
                  ["kibit"]
                  ["eastwood"]
                  ["test"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :deploy-repositories [["releases" :clojars]]
)
