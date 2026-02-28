(defproject fathom "0.1.0-SNAPSHOT"
  :description "An inference engine in Clojure"
  :url "https://github.com/yourusername/fathom"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.1"]]}}
  :aliases {"test" ["run" "-m" "clojure.main"]})
