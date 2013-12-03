(defproject snake ""

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [[org.clojure/clojure "1.5.1"]

                 [ring/ring-core "1.2.0"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.4"]

                 [prismatic/dommy "0.1.1"]

                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [org.clojure/clojurescript "0.0-2080"]
                 [org.clojure/tools.reader "0.8.0"]

                 [jarohen/chord "0.2.1"]]

  :plugins [[jarohen/lein-frodo "0.2.3"]
            [lein-cljsbuild "1.0.0"]
            [lein-pdo "0.1.1"]]

  :frodo/config-resource "snake-config.edn"

  :source-paths ["src/clojure"
                 "src/cljx"]

  :resource-paths ["resources" "target/resources"]

  :cljsbuild {:crossovers [snake.board]
              :builds [{:source-paths ["src/cljs"]
                        :crossover-path "src/cljx"
                        :compiler {:output-to "target/resources/js/snake.js"
                                   :output-dir "target/resources/js/"
                                   :optimizations :whitespace
                                   :pretty-print true
;                                   :source-map "target/resources/js/snake.js.map"
                                   }}

                       {:source-paths ["src/cljs"]
                        :id "prod"
                        :crossover-path "src/cljx"
                        :compiler {:output-to "target/resources/js/snake.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :externs ["externs/jquery.js"]}}]}

  :aliases {"dev" ["pdo" "cljsbuild" "auto," "frodo"]
            "start" ["do" "cljsbuild" "once" "prod," "trampoline" "frodo"]})
