(defproject com.unbounce/treajure "1.0.2-SNAPSHOT"
  :description "A treasure trove of Clojure goodness."
  :url "https://www.github.com/unbounce/treajure"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"
            :comments "Copyright (c) 2015 Unbounce Marketing Solutions Inc."}

  :profiles {:dev {:plugins [[lein-kibit "0.1.2"]
                             [jonase/eastwood "0.2.1"]]

                   :dependencies [[byte-streams "0.2.0-alpha8"]
                                  [me.raynes/fs "1.4.6"]
                                  [ring/ring-core "1.3.2"]]}}

  :dependencies
  [
   [org.clojure/clojure "1.6.0"]
  ])
