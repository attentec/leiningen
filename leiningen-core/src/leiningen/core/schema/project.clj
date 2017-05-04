(ns leiningen.core.schema.project
  (:require [schema.core                  :as schema :refer [defschema]]
            [schema-generators.generators :as gen]
            [miner.strgen                 :as strgen]
            [clojure.test.check.generators :as genn]
            [leiningen.core.project       :as proj]
            [leiningen.core.schema.util   :as util]))


;;; Minor keys in project-argument-keys from top to bottom.

;; Regexes aren't accepted by defschema, see: https://github.com/plumatic/schema/issues/391
(def url                     (util/stregex! #"^(https?|ftp)://[^\s/$.?#]+\.?[^\s]*$"))
(def email                   (util/stregex! #"\S+@\S+\.?\S+"))
(def semantic-version-string (util/stregex! #"(\d+)\.(\d+)\.(\d+)(-\w+)?(-SNAPSHOT)?"))
(def namespaced-string       (util/stregex! #"[^\s/]+/[^\s/]+"))
(def pedantic?               (schema/enum :abort :warn :ranges true false))


;;; Mailing lists

(def name           util/non-blank-string)
(def other-archives [url])
(def subscribe      (schema/cond-pre email url))
(def unsubscribe    (schema/cond-pre email url))
(def mailing-list
  {(schema/optional-key :name)           name
   (schema/optional-key :archive)        url
   (schema/optional-key :other-archives) other-archives
   (schema/optional-key :post)           email
   (schema/optional-key :subscribe)      subscribe
   (schema/optional-key :unsubscribe)    unsubscribe})
(def mailing-lists [mailing-list])


;;; Licenses

(def distribution (schema/enum :repo :manual))
(def license
  {(schema/optional-key :name)         name
   (schema/optional-key :url)          url
   (schema/optional-key :distribution) distribution
   (schema/optional-key :comments)     util/non-blank-string})
(def licenses [license])


;;; Dependencies

(def dependency-name
  (schema/cond-pre namespaced-string util/non-blank-string
                   util/qualified-symbol schema/Symbol))

(def optional      (schema/pred util/boolean?))
(def scope         util/non-blank-string)
(def classifier    util/non-blank-string)
(def native-prefix (schema/pred string?))
(def extension     util/non-blank-string)
(def artifact-id   util/non-blank-string)
(def group-id      util/non-blank-string)
(def version       util/non-blank-string)

;; TODO: Provide generator for everything that uses util/key-val-seq.
(defn exclusion-arguments? [kv-seq]
  (util/key-val-seq? kv-seq {:scope         scope
                             :classifier    classifier
                             :native-prefix native-prefix
                             :extension     extension}))
(def exclusion-args
  (schema/constrained schema/Any exclusion-arguments?))


(defn exclusion-vector? [excl-vec]
  ((util/first-rest-cat-fn dependency-name exclusion-args) excl-vec))
(def exclusion-vector
  (schema/constrained [schema/Any] exclusion-vector?))

(def exclusion
  (schema/cond-pre dependency-name exclusion-vector))
(def exclusions [exclusion])

(def dependency-name-map
  {:artifact-id artifact-id
   :group-id    group-id})

(def dependency-args-map {:optional      optional
                          :scope         scope
                          :classifier    classifier
                          :native-prefix native-prefix
                          :extension     extension
                          :exclusions    exclusions})
(defn dependency-args? [kv-seq]
  (util/key-val-seq? kv-seq dependency-args-map))
(def dependency-args
  (schema/constrained schema/Any dependency-args?))

;; TODO: Perhaps we can attach a generator here which pulls in artifacts from:
;; https://clojars.org/repo/all-jars.clj
(def artifact
  [(schema/one dependency-name "name") (schema/one version "version")])

(defn dependency-vector? [dep-vec]
  ((util/pair-rest-cat-fn artifact dependency-args) dep-vec))
(def dependency-vector
  (schema/constrained [schema/Any] dependency-vector?))

(def dependency-map
  {:artifact-id artifact-id
   :group-id    group-id
   :version     version})

(def exclusion-map
  {:artifact-id artifact-id :group-id group-id})

(def dependencies
  [dependency-vector])

(def managed-dependencies
  [dependency-vector])


;;; Plugins


(defn plugin-args? [kv-seq]
  (util/key-val-seq? kv-seq (merge dependency-args-map
                                   {:middleware schema/Bool
                                    :hooks      schema/Bool})))
(def plugin-args
  (schema/constrained schema/Any plugin-args?))

(defn plugin-vector? [dep-vec]
  ((util/pair-rest-cat-fn artifact plugin-args) dep-vec))
(def plugin-vector
  (schema/constrained [schema/Any] plugin-vector?))

(def plugins
  [plugin-vector])



(defschema project-map
  {(schema/optional-key :description)                util/non-blank-string
   (schema/optional-key :url)                        url
   (schema/optional-key :mailing-list)               mailing-list
   (schema/optional-key :mailing-lists)              mailing-lists
   (schema/optional-key :license)                    license
   (schema/optional-key :licenses)                   licenses
   (schema/optional-key :min-lein-version)           semantic-version-string
   (schema/optional-key :dependencies)               dependencies
   (schema/optional-key :managed-dependencies)       dependencies
   (schema/optional-key :pedantic?)                  pedantic?
   (schema/optional-key :exclusions)                 exclusions
   (schema/optional-key :plugins)                    plugins
   ;; (schema/optional-key :repositories)
   ;; (schema/optional-key :plugin-repositories)
   ;; (schema/optional-key :mirrors)
   ;; (schema/optional-key :local-repo)
   ;; (schema/optional-key :update)
   ;; (schema/optional-key :checksum)
   ;; (schema/optional-key :offline?)
   ;; (schema/optional-key :deploy-repositories)
   ;; (schema/optional-key :signing)
   ;; (schema/optional-key :certificates)
   ;; (schema/optional-key :profiles)
   ;; (schema/optional-key :hooks)
   ;; (schema/optional-key :middleware)
   ;; (schema/optional-key :implicit-middleware)
   ;; (schema/optional-key :implicit-hooks)
   ;; (schema/optional-key :main)
   ;; (schema/optional-key :aliases)
   ;; (schema/optional-key :release-tasks)
   ;; (schema/optional-key :prep-tasks)
   ;; (schema/optional-key :aot)
   ;; (schema/optional-key :injections)
   ;; (schema/optional-key :java-agents)
   ;; (schema/optional-key :javac-options)
   ;; (schema/optional-key :warn-on-reflection)
   ;; (schema/optional-key :global-vars)
   ;; (schema/optional-key :java-cmd)
   ;; (schema/optional-key :jvm-opts)
   ;; (schema/optional-key :eval-in)
   ;; (schema/optional-key :bootclasspath)
   ;; (schema/optional-key :source-paths)
   ;; (schema/optional-key :java-source-paths)
   ;; (schema/optional-key :test-paths)
   ;; (schema/optional-key :resource-paths)
   ;; (schema/optional-key :target-path)
   ;; (schema/optional-key :compile-path)
   ;; (schema/optional-key :native-path)
   ;; (schema/optional-key :clean-targets)
   ;; (schema/optional-key :clean-non-project-classes)
   ;; (schema/optional-key :checkout-deps-shares)
   ;; (schema/optional-key :test-selectors)
   ;; (schema/optional-key :monkeypatch-clojure-test)
   ;; (schema/optional-key :repl-options)
   ;; (schema/optional-key :jar-name)
   ;; (schema/optional-key :uberjar-name)
   ;; (schema/optional-key :omit-source)
   ;; (schema/optional-key :jar-exclusions)
   ;; (schema/optional-key :uberjar-exclusions)
   ;; (schema/optional-key :auto-clean)
   ;; (schema/optional-key :uberjar-merge-with)
   ;; (schema/optional-key :filespecs)
   ;; (schema/optional-key :manifest)
   ;; (schema/optional-key :pom-location)
   ;; (schema/optional-key :parent)
   ;; (schema/optional-key :extensions)
   ;; (schema/optional-key :pom-plugins)
   ;; (schema/optional-key :pom-addition)
   ;; (schema/optional-key :scm)
   ;; (schema/optional-key :install-releases?)
   ;; (schema/optional-key :deploy-branches)
   ;; (schema/optional-key :classifiers)
   })

; (gen/generate project-argument-keys @util/generators)
