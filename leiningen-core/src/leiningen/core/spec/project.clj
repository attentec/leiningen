(ns leiningen.core.spec.project
  (:require [clojure.spec           :as spec]
            [clojure.spec.gen       :as gen]
            [clojure.spec.test      :as test]
            [clojure.string         :as str]
            [miner.strgen           :as strgen]
            [leiningen.core.project :as proj]))

(spec/def ::proj/non-blank-string
  (spec/and string? #(not (str/blank? %))))


(spec/def ::proj/namespaced-string
  (let [qualified-regexp #"[^\s/]+/[^\s/]+"]
    (spec/with-gen
      (spec/and string? #(re-matches qualified-regexp %))
      #(strgen/string-generator qualified-regexp))))

(spec/def ::proj/lib-name (spec/or
   :namespaced-string ::proj/namespaced-string
   :bare-string       ::proj/non-blank-string
   :namespaced-symbol qualified-symbol?
   :bare-symbol       simple-symbol?))

(spec/def ::proj/artifact-id ::proj/non-blank-string)
(spec/def ::proj/group-id    ::proj/non-blank-string)
(spec/def ::proj/lib-name-map
  (spec/keys :req [::proj/artifact-id
                   ::proj/group-id]))

(spec/def ::proj/artifact
  (spec/& ::proj/lib-name ::proj/version (spec/* ::proj/dependency-item-args)))



;;; Function defenitions

(spec/fdef proj/artifact-map
           :args (spec/cat :lib-name ::proj/lib-name)
           :fn  #(let [in (-> % :args :lib-name second)]
                   (str/includes? in (-> % :ret ::proj/artifact-id))
                   (str/includes? in (-> % :ret ::proj/group-id)))
           :ret  ::proj/lib-name-map)
