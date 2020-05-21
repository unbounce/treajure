(defproject com.unbounce/treajure "1.0.13-SNAPSHOT"
  :description "A treasure trove of Clojure goodness."
  :url "https://www.github.com/unbounce/treajure"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"
            :comments "Copyright (c) 2015-2020 Unbounce Marketing Solutions Inc."}

  :profiles {:dev {:plugins [[lein-kibit "0.1.2"]
                             [jonase/eastwood "0.2.3"]]

                   :dependencies [[byte-streams "0.2.4"]
                                  [org.clojure/tools.logging "1.1.0"]
                                  [me.raynes/fs "1.4.6"]
                                  [ring/ring-core "1.8.1"]
                                  [cheshire "5.10.0"]
                                  [ring/ring-jetty-adapter "1.8.1"]
                                  [ring/ring-defaults "0.3.2"]
                                  [com.github.fge/json-schema-validator "2.2.6"]]
                   :resource-paths ["resources" "test-resources"]}

             :test {:resource-paths ["resources" "test-resources"]}}

  :dependencies
  [
   [org.clojure/clojure "1.10.1"]
   [bwo/monads "0.2.2"]
   [bwo/macroparser "0.0.7c"]
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
