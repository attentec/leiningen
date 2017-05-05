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
(def signing                 {:gpg-key util/non-blank-string})
(def certificates            (schema/constrained [util/non-blank-string] not-empty))
(declare project-map-non-recursive)
(def profiles                {schema/Keyword project-map-non-recursive})
(def hooks                   (schema/constrained [schema/Symbol] not-empty))
(def middleware              (schema/constrained [schema/Symbol] not-empty))
(def javac-options           (schema/constrained [util/non-blank-string] not-empty))
(def jvm-opts                (schema/constrained [util/non-blank-string] not-empty))
(def eval-in                 (schema/enum :subprocess :leiningen :nrepl))


;;; Mailing lists

(def name-schema    util/non-blank-string)
(def other-archives [url])
(def subscribe      (schema/cond-pre email url))
(def unsubscribe    (schema/cond-pre email url))
(def mailing-list
  {(schema/optional-key :name)           name-schema
   (schema/optional-key :archive)        url
   (schema/optional-key :other-archives) other-archives
   (schema/optional-key :post)           email
   (schema/optional-key :subscribe)      subscribe
   (schema/optional-key :unsubscribe)    unsubscribe})
(def mailing-lists
  (schema/constrained [mailing-list] not-empty))


;;; Licenses

(def distribution (schema/enum :repo :manual))
(def license
  {(schema/optional-key :name)         name-schema
   (schema/optional-key :url)          url
   (schema/optional-key :distribution) distribution
   (schema/optional-key :comments)     util/non-blank-string})
(def licenses
  (schema/constrained [license] not-empty))


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
(def exclusions
  (schema/constrained [exclusion] not-empty))

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
  (schema/constrained [dependency-vector] not-empty))

(def managed-dependencies
  (schema/constrained [dependency-vector] not-empty))


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
  (schema/constrained [plugin-vector] not-empty))


;;; Repositories

(def checksum    (schema/enum :fail :warn :ignore))
(def update-enum (schema/enum :always :daily :never))
(def releases    {(schema/optional-key :checksum) checksum
                  (schema/optional-key :update)   update-enum})
(def password    (schema/cond-pre util/non-blank-string
                                  (schema/enum :env)))
(def creds       (schema/enum :gpg))

(def repository-info-map
  {(schema/optional-key :url)           url
   (schema/optional-key :snapshots)     schema/Bool
   (schema/optional-key :sign-releases) schema/Bool
   (schema/optional-key :checksum)      checksum
   (schema/optional-key :update)        update-enum
   (schema/optional-key :releases)      releases
   (schema/optional-key :username)      util/non-blank-string
   (schema/optional-key :password)      password
   (schema/optional-key :creds)         creds
   (schema/optional-key :signing)       signing})

(def repository
  [(schema/one util/non-blank-string "name")
   (schema/one (schema/cond-pre url repository-info-map) "url or info-map")])
(def repositories
  [repository])


;;; Mirrors

(def mirrors
  {(schema/cond-pre schema/Str schema/Regex) {(schema/optional-key :name)          name-schema
                                              (schema/optional-key :url)           url
                                              (schema/optional-key :repo-manager) schema/Bool}})


;;; Aliases

(def command-vector
  (schema/constrained
   [(schema/cond-pre util/non-blank-string schema/Keyword)]
   not-empty))

(def do-command
  [(schema/one (schema/enum "do") "do")
   (schema/cond-pre schema/Str command-vector)])

(def aliases
  {util/non-blank-string (schema/conditional
                          #(= (first %) "do") do-command
                          :else               command-vector)})


(def release-tasks
  (schema/constrained
   [(schema/cond-pre schema/Str command-vector)]
   not-empty))


(def aot
  (schema/cond-pre (schema/enum :all)
                   [(schema/cond-pre schema/Symbol schema/Regex)]))


;;; Java agents

(defn java-agent-args? [kv-seq]
  (util/key-val-seq? kv-seq {:classifier classifier
                             :options    schema/Str}))
(def java-agent-args
  (schema/constrained schema/Any java-agent-args?))

(defn java-agent-vector? [dep-vec]
  ((util/pair-rest-cat-fn artifact java-agent-args) dep-vec))
(def java-agents
  (schema/constrained
   [(schema/constrained [schema/Any] java-agent-vector?)]
   not-empty))

;;; Global vars
;; See http://stackoverflow.com/questions/43452079
(def clojure-global-vars
  (schema/enum
   '*print-namespace-maps* '*source-path* '*command-line-args*
   '*read-eval* '*verbose-defrecords* '*print-level* '*suppress-read*
    '*print-length* '*file* '*use-context-classloader* '*err*
    '*default-data-reader-fn* '*allow-unresolved-vars* '*print-meta*
    '*compile-files* '*math-context* '*data-readers* '*clojure-version*
    '*unchecked-math* '*out* '*warn-on-reflection* '*compile-path*
    '*in* '*ns* '*assert* '*print-readably* '*flush-on-newline*
    '*agent* '*fn-loader* '*compiler-options* '*print-dup*))

(def global-vars
  {clojure-global-vars schema/Any})


;;; Project maps and permutations there of.

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
   (schema/optional-key :repositories)               repositories
   (schema/optional-key :plugin-repositories)        repositories
   (schema/optional-key :mirrors)                    mirrors
   (schema/optional-key :local-repo)                 util/non-blank-string
   (schema/optional-key :update)                     update-enum
   (schema/optional-key :checksum)                   checksum
   (schema/optional-key :offline?)                   schema/Bool
   (schema/optional-key :deploy-repositories)        repositories
   (schema/optional-key :signing)                    signing
   (schema/optional-key :certificates)               certificates
   (schema/optional-key :profiles)                   profiles
   (schema/optional-key :hooks)                      hooks
   (schema/optional-key :middleware)                 middleware
   (schema/optional-key :implicit-middleware)        schema/Bool
   (schema/optional-key :implicit-hooks)             schema/Bool
   (schema/optional-key :main)                       schema/Symbol
   (schema/optional-key :aliases)                    aliases
   (schema/optional-key :release-tasks)              release-tasks
   (schema/optional-key :prep-tasks)                 release-tasks
   (schema/optional-key :aot)                        aot
   (schema/optional-key :injections)                 [schema/Any] ; TODO: Too lax
   (schema/optional-key :java-agents)                java-agents
   (schema/optional-key :javac-options)              javac-options
   (schema/optional-key :warn-on-reflection)         schema/Bool
   (schema/optional-key :global-vars)                global-vars
   (schema/optional-key :java-cmd)                   util/non-blank-string
   (schema/optional-key :jvm-opts)                   jvm-opts
   (schema/optional-key :eval-in)                    eval-in
   (schema/optional-key :bootclasspath)              schema/Bool
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
   ;; Make the map schema open.
   ;schema/Keyword schema/Any
   })

(defschema project-map-non-recursive (dissoc project-map :filespecs :profiles))


; (gen/generate project-argument-keys @util/generators)
