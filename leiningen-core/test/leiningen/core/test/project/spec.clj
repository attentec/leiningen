(ns leiningen.core.test.project.spec
  (:require [clojure.spec           :as spec]
            [clojure.spec.test      :as test]
            [clojure.pprint         :as pprint]
            [clojure.test           :refer [deftest is]
            [leiningen.core.project :as project]]))
(clojure.test/run-tests)


;;; Helper functions

(defn summarize-results [test-check-result]
  (map (comp #(pprint/write % :stream nil) test/abbrev-result) test-check-result))

(defn check [test-check-result]
  (is (nil? (-> test-check-result first :failure))
      (summarize-results test-check-result)))


;;; Generative tests for functions

(deftest test-artifact-map
  (check (test/check `project/artifact-map)))




(remove-ns 'leiningen.core.test.project.spec)
