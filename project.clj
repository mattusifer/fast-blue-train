(defproject fast-blue-train "0.1.0-SNAPSHOT"
  :description "Travel planning web app"
  :url "http://fast-blue-train.herokuapp.com"
  :license {:name "Eclipse Public License v1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "2.1.0"]
                 [org.clojure/core.async "0.2.374"]
                 [compojure "1.4.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [cljs-http "0.1.39"]
                 [cljs-ajax "0.5.4"]
                 [org.clojure/core.async "0.2.374"]
                 [prismatic/dommy "1.1.0"]
                 [im.chit/gyr "0.3.1"]
                 [environ "1.0.0"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-cljsbuild "1.1.2"]
            [environ/environ.lein "0.3.1"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "fast-blue-train-standalone.jar"
  :ring {:handler fast-blue-train.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}}
  :cljsbuild {
              :builds [{:source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/scripts/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})
