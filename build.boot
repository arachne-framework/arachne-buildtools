(set-env! :resource-paths #{"src"})

(set-env! :dependencies '[[adzerk/boot-test "1.1.2"]])

(require '[arachne.buildtools :refer :all])
(read-project-edn!)


