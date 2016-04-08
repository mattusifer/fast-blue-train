(defproject fast-blue-train "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.374"]
                 [compojure "1.4.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [org.clojure/clojurescript "1.7.228"]
                 [cljs-http "0.1.39"]
                 [org.clojure/core.async "0.2.374"]
                 [prismatic/dommy "1.1.0"]
                 [im.chit/gyr "0.3.1"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-cljsbuild "1.1.2"]]
  :ring {:handler fast-blue-train.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}}
  :cljsbuild {
              :builds [{:source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/scripts/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})
