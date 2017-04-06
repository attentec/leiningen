(ns leiningen.core.test.spec.project
  (:require [clojure.spec           :as spec]
            [clojure.spec.test      :as test]
            [clojure.pprint         :as pprint]
            [clojure.test           :refer [deftest is]]
            [leiningen.core.project :as project]))


;;; Helper functions

(defn summarize-results [test-check-result]
  (map (comp #(pprint/write % :stream nil)
             test/abbrev-result) test-check-result))

(defn check [test-check-result]
  (is (nil? (-> test-check-result first :failure))
      (summarize-results test-check-result)))


;;; Generative tests for functions

(deftest test-artifact-map
  (check (test/check `project/artifact-map)))

(deftest test-dependency-map
  (check (test/check `project/dependency-map)))

(deftest test-dependency-vec
  (check (test/check `project/dependency-vec)))
