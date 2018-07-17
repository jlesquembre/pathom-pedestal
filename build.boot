(def project 'pathom-pedestal)
(def version "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "1.9.0"]
                            [io.pedestal/pedestal.service "0.5.4"]
                            [io.pedestal/pedestal.route   "0.5.4"]
                            [io.pedestal/pedestal.jetty   "0.5.4"]
                            [hickory "0.7.1"]
                            [com.wsscode/pathom "2.0.9"]

                            [adzerk/boot-test "1.2.0" :scope "test"]
                            [adzerk/bootlaces "0.1.13" :scope "test"]])

(task-options!
 pom {:project     project
      :version     version
      :description "Pedestal infrastructure supporting Pathom "
      :url         "https://github.com/jlesquembre/pathom-pedestal"
      :scm         {:url "https://github.com/jlesquembre/pathom-pedestal"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}}
 aot {:namespace '#{pathom.pedestal}}
 jar {:main 'pathom.pedestal})

(deftask build
  "Build and install the project locally."
  []
  (comp (pom) (jar) (install)))

(deftask release
 "Build uberjar"
 []
 (comp (aot)
       (pom)
       (uber)
       (jar)
       (target :dir #{"target"})))

(require '[adzerk.boot-test :refer [test]])
(require '[adzerk.bootlaces :refer :all])
(bootlaces! version)
