(ns leiningen.test.spec.release
  (:require [clojure.test                  :refer [deftest]]
            [clojure.spec.test             :as test]
            [leiningen.release             :as release]
            [leiningen.spec.release        :as release-spec]
            [leiningen.core.test.spec.util :as util]))

(deftest test-string->semantic-version
  (util/check (test/check `release/string->semantic-version)))

(deftest test-parse-semantic-version
  (util/check (test/check `release/parse-semantic-version)))

(deftest test-version-map->string
  (util/check (test/check `release/version-map->string)))

(clojure.test/run-tests)
