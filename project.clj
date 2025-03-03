(defproject net.clojars.macielti/sinapse "0.1.0-2"

  :description "Sinapse is an Integrant component to deal with async messaging, when the messages are intent to be
  consumed internal by the same service that produced the messages."

  :url "https://github.com/macielti/sinapse"

  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.12.0"]
                 [prismatic/schema "1.4.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [io.pedestal/pedestal.interceptor "0.7.2"]
                 [net.clojars.macielti/common-clj "43.74.74"]
                 [integrant "0.13.1"]]

  :profiles {:dev {:plugins        [[lein-shell "0.5.0"]
                                    [com.github.liquidz/antq "RELEASE"]
                                    [com.github.clojure-lsp/lein-clojure-lsp "1.4.17"]]

                   :resource-paths ["resources"]

                   :test-paths     ["test/unit" "test/integration" "test/helpers"]

                   :dependencies   [[hashp "0.2.2"]]

                   :injections     [(require 'hashp.core)]

                   :aliases        {"clean-ns"     ["clojure-lsp" "clean-ns" "--dry"] ;; check if namespaces are clean
                                    "format"       ["clojure-lsp" "format" "--dry"] ;; check if namespaces are formatted
                                    "diagnostics"  ["clojure-lsp" "diagnostics"] ;; check if project has any diagnostics (clj-kondo findings)
                                    "lint"         ["do" ["clean-ns"] ["format"] ["diagnostics"]] ;; check all above
                                    "clean-ns-fix" ["clojure-lsp" "clean-ns"] ;; Fix namespaces not clean
                                    "format-fix"   ["clojure-lsp" "format"] ;; Fix namespaces not formatted
                                    "lint-fix"     ["do" ["clean-ns-fix"] ["format-fix"]]}}}) ;; Fix both
