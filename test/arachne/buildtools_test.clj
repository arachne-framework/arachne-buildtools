(ns arachne.buildtools-test
  (:require [clojure.test :refer [deftest is]]))


(deftest my-unit-test
  (println "running a unit test...")
  (is (= 1 1)))

(deftest ^:integration my-integration-test
  (println "running an integration test...")
  (is (= 1 1)))