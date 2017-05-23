(ns leiningen.core.test.spec.util
  (:require [clojure.pprint          :as pprint]
            [clojure.spec.test.alpha :as test]
            [clojure.test            :refer [is]]))

;;; Helper functions

(defn summarize-results [test-check-result]
  (map (comp #(pprint/write % :stream nil)
             test/abbrev-result) test-check-result))

(defn check [test-check-result]
  (is (nil? (-> test-check-result first :failure))
      (summarize-results test-check-result)))
