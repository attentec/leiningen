(ns leiningen.test.spec.release
  (:require [clojure.test                  :refer [deftest]]
            [clojure.spec.test             :as test]
            [leiningen.release             :as release]
            [leiningen.spec.release        :as release-spec]
            [leiningen.core.test.spec.util :as util]))

(deftest test-string->semantic-version
  (util/check (test/check `release/string->semantic-version)))

;; The parse-semantic-version function kills the JVM on
;; failure. Should it really do that?
;; (deftest test-parse-semantic-version
;;   (util/check (test/check `release/parse-semantic-version)))

;; If you relax the spec for the string parser you'll find
;; some errors like:
;;
;; {:major 0 :minor 0 :patch 1.0 :qualifier "" :snapshot "SNAPSHOT"}
;; produces a bad version string: "0.0.1.0--SNAPSHOT".
(deftest test-version-map->string
  (util/check (test/check `release/version-map->string)))
