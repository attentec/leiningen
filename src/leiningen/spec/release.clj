(ns leiningen.spec.release
  (:require [clojure.spec             :as spec]
            [clojure.spec.gen         :as gen]
            [clojure.spec.test        :as test]
            [leiningen.release        :as release]
            [leiningen.core.project   :as proj]
            [leiningen.core.spec.util :as util]))


(spec/def ::release/major     number?)
(spec/def ::release/minor     number?)
(spec/def ::release/patch     number?)
(spec/def ::release/qualifier string?)
(spec/def ::release/snapshot  #{"SNAPSHOT"})

(spec/def ::release/semantic-version-map
  (spec/keys :req-un [::release/major ::release/minor ::release/patch
                      ::release/qualifier ::release/snapshot]))

;;;; Function defenitions

(spec/fdef release/string->semantic-version
           :args (spec/cat :version-str ::proj/semantic-version-string)
           :ret  ::release/semantic-version-map)

(spec/fdef release/parse-semantic-version
           :args ::util/non-blank-string
           :ret  (spec/or :success ::release/semantic-version-map
                          :failure string?))

(spec/fdef release/version-map->string
           :args (spec/cat :version-map ::release/semantic-version-map)
           :ret  ::proj/semantic-version-string)
