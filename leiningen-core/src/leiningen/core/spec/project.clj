(ns leiningen.core.spec.project
  (:require [clojure.spec             :as spec]
            [clojure.spec.gen         :as gen]
            [clojure.spec.test        :as test]
            [clojure.string           :as str]
            [miner.strgen             :as strgen]
            [leiningen.core.project   :as proj]
            [leiningen.core.spec.util :as util]))

(spec/def ::non-blank-string
  (spec/and string? #(not (str/blank? %))))

(spec/def ::namespaced-string
  (let [qualified-regexp #"[^\s/]+/[^\s/]+"]
    (spec/with-gen
      (spec/and string? #(re-matches qualified-regexp %))
      #(strgen/string-generator qualified-regexp))))

(spec/def ::proj/dependency-name
  (spec/alt
   :namespaced-string ::namespaced-string
   :bare-string       ::non-blank-string
   :namespaced-symbol qualified-symbol?
   :bare-symbol       simple-symbol?))

(spec/def ::proj/artifact-id ::non-blank-string)
(spec/def ::proj/group-id    ::non-blank-string)
(spec/def ::proj/dependency-name-map
  (spec/keys :req [::proj/artifact-id ::proj/group-id]))

(spec/def ::proj/exclusion
  (spec/or
   :plain-name ::proj/dependency-name
   :vector     ::proj/exclusion-vector))

(spec/def ::proj/exclusion-vector
  (util/vcat :dep-name  ::proj/dependency-name
             :arguments ::proj/exclusion-args))

(spec/def ::proj/optional      boolean?)
(spec/def ::proj/scope         ::non-blank-string)
(spec/def ::proj/classifier    ::non-blank-string)
(spec/def ::proj/native-prefix ::non-blank-string)
(spec/def ::proj/extension     ::non-blank-string)
(spec/def ::proj/exclusions    (spec/coll-of ::proj/exclusion :gen-max 2))
(spec/def ::proj/dependency-args
  (spec/keys*
   :opt-un [::proj/optional ::proj/scope ::proj/classifier
            ::proj/native-prefix ::proj/extension ::proj/exclusions]))
(spec/def ::proj/exclusion-args
  (spec/keys*
   :opt-un [::proj/scope ::proj/classifier
            ::proj/native-prefix ::proj/extension]))

(spec/def ::proj/version ::non-blank-string)

(spec/def ::proj/dependency-vector
  (util/vcat :name      ::proj/dependency-name
             :version   ::proj/version
             :arguments ::proj/dependency-args))

(spec/def ::proj/dependency-map
  (spec/keys :req [::proj/artifact-id ::proj/group-id ::proj/version]))

(spec/def ::proj/exclusion-map
  (spec/keys :req [::proj/artifact-id ::proj/group-id]))


;;; Function defenitions

(spec/fdef proj/artifact-map
           :args (spec/cat :dep-name ::proj/dependency-name)
           :fn  #(let [in (-> % :args :dep-name second)]
                   (str/includes? in (-> % :ret ::proj/artifact-id))
                   (str/includes? in (-> % :ret ::proj/group-id)))
           :ret  ::proj/dependency-name-map)

(spec/fdef proj/dependency-map
           :args (spec/cat :dependency-vec ::proj/dependency-vector)
           :ret  ::proj/dependency-map)

(spec/fdef proj/dependency-vec
           :args (spec/cat :dependency-map ::proj/dependency-map)
           :ret  ::proj/dependency-vector)

(spec/fdef proj/exclusion-map
           :args (spec/cat :exclusion ::proj/exclusion)
           :ret ::proj/exclusion-map)

(spec/fdef proj/exclusion-vec
           :args (spec/cat :exclusion ::proj/exclusion-map)
           :ret ::proj/exclusion)
