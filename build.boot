(set-env! :resource-paths #{"src"})

(set-env! :repositories
  #(conj % ["arachne-dev" {:url "http://maven.arachne-framework.org/artifactory/arachne-dev"}]))

(set-env! :dependencies '[[adzerk/boot-test "1.1.2"]])

(require '[arachne.buildtools :refer :all])


(read-project-edn!)


