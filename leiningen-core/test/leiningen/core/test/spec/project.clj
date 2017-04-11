(ns leiningen.core.test.spec.project
  (:require [clojure.spec.test             :as test]
            [clojure.test                  :refer [deftest]]
            [leiningen.core.project        :as project]
            ;; Load the specs into the registry
            [leiningen.core.spec.project   :as project-spec]
            [leiningen.core.test.spec.util :as util]))


;;; Generative tests for functions

(deftest test-artifact-map
  (util/check (test/check `project/artifact-map)))

(deftest test-dependency-map
  (util/check (test/check `project/dependency-map)))

(deftest test-dependency-vec
  (util/check (test/check `project/dependency-vec)))

(deftest test-exclusion-map
  (util/check (test/check `project/exclusion-map)))

(deftest test-exclusion-vec
  (util/check (test/check `project/exclusion-vec)))

(deftest test-defproject
  (util/check (test/check `project/defproject)))
