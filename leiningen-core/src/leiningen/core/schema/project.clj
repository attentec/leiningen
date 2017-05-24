(ns leiningen.core.schema.project
  (:require [schema.core                      :as schema :refer [defschema]]
            [schema-generators.generators     :as gen]
            [schema.experimental.abstract-map :as schema-typed-map]
            [miner.strgen                     :as strgen]
            [clojure.test.check.generators    :as genn]
            [leiningen.core.project           :as proj]
            [leiningen.core.schema.util       :as util]))

;; TODO: Constrain a bunch of sequences to only be vectors.


;;; Minor keys in project-argument-keys from top to bottom.

;; Regexes aren't accepted by defschema, see: https://github.com/plumatic/schema/issues/391
(def url                     (util/stregex! #"^(https?|ftp)://[^\s/$.?#]+\.?[^\s]*$"))
(def email                   (util/stregex! #"\S+@\S+\.?\S+"))
(def semantic-version-string (util/stregex! #"(\d+)\.(\d+)\.(\d+)(-\w+)?(-SNAPSHOT)?"))
(def namespaced-string       (util/stregex! #"[^\s/]+/[^\s/]+"))
(def pedantic?               (schema/enum :abort :warn :ranges true false))
(def signing                 {:gpg-key util/non-blank-string})
(def certificates            (schema/constrained [util/non-blank-string] util/non-empty-vec?))
(declare project-map-non-recursive)
(def profiles                {schema/Keyword (schema/recursive #'project-map-non-recursive)})
(def hooks                   (schema/constrained [schema/Symbol] util/non-empty-vec?))
(def middleware              (schema/constrained [schema/Symbol] util/non-empty-vec?))
(def javac-options           (schema/constrained [util/non-blank-string] util/non-empty-vec?))
(def jvm-opts                (schema/constrained [util/non-blank-string] util/non-empty-vec?))
(def eval-in                 (schema/enum :subprocess :leiningen :nrepl))
(def path                    util/non-blank-string)
(def paths                   (schema/constrained [util/non-blank-string] util/non-empty-vec?))
(def deploy-branches         (schema/constrained [util/non-blank-string] util/non-empty-vec?))
(def non-empty-vec-of-regexes (schema/constrained [schema/Regex] util/non-empty-vec?))

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
   (schema/optional-key :unsubscribe)    unsubscribe
   schema/Keyword                        schema/Any})
(def mailing-lists
  (schema/constrained [mailing-list] util/non-empty-vec?))


;;; Licenses

(def distribution (schema/enum :repo :manual))
(def license
  {(schema/optional-key :name)         name-schema
   (schema/optional-key :url)          url
   (schema/optional-key :distribution) distribution
   (schema/optional-key :comments)     util/non-blank-string
   schema/Keyword                      schema/Any})
(def licenses
  (schema/constrained [license] util/non-empty-vec?))


;;; Dependencies

(def dependency-name
  (schema/cond-pre namespaced-string
                   util/non-blank-string
                   util/qualified-symbol
                   schema/Symbol))

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
  (schema/constrained [exclusion] util/non-empty-vec?))

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
  (schema/constrained [dependency-vector] util/non-empty-vec?))

(def managed-dependencies
  (schema/constrained [dependency-vector] util/non-empty-vec?))


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
  (schema/constrained [plugin-vector] util/non-empty-vec?))


;;; Repositories

(def checksum    (schema/enum :fail :warn :ignore))
(def update-enum (schema/enum :always :daily :never))
(def releases    {(schema/optional-key :checksum) checksum
                  (schema/optional-key :update)   update-enum
                  schema/Keyword                  schema/Any})
(def password    (schema/cond-pre util/non-blank-string schema/Keyword))
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
   (schema/optional-key :signing)       signing
   schema/Keyword                       schema/Any})

(def repository
  [(schema/one util/non-blank-string "name")
   (schema/one (schema/cond-pre url repository-info-map) "url or info-map")])
(def repositories
  (schema/constrained [repository] util/non-empty-vec?))


;;; Mirrors

(def mirrors
  {(schema/cond-pre util/non-blank-string schema/Regex) {(schema/optional-key :name)         name-schema
                                                         (schema/optional-key :url)          url
                                                         (schema/optional-key :repo-manager) schema/Bool}})


;;; Aliases

(def command-vector
  (schema/constrained
   [(schema/cond-pre util/non-blank-string schema/Keyword)]
   util/non-empty-vec?))

(def do-command
  [(schema/one (schema/enum "do") "do")
   (schema/cond-pre util/non-blank-string command-vector)])

(def aliases
  {util/non-blank-string (schema/conditional
                          #(= (first %) "do") do-command
                          :else               command-vector)})

;;; Release tasks

(def release-tasks
  (schema/constrained
   [(schema/cond-pre util/non-blank-string command-vector)]
   util/non-empty-vec?))


;;; AOT

(def aot
  (schema/cond-pre (schema/enum :all)
                   [(schema/cond-pre schema/Symbol schema/Regex)]))


;;; Java agents

(defn java-agent-args? [kv-seq]
  (util/key-val-seq? kv-seq {:classifier classifier
                             :options    util/non-blank-string}))
(def java-agent-args
  (schema/constrained schema/Any java-agent-args?))

(defn java-agent-vector? [dep-vec]
  ((util/pair-rest-cat-fn artifact java-agent-args) dep-vec))
(def java-agents
  (schema/constrained
   [(schema/constrained [schema/Any] java-agent-vector?)]
   util/non-empty-vec?))

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


;;; Clean-targets
;; A path into the project map.
(defn more-than-two-elements?
  [v] (>= (count v) 2))
(def project-path
  (schema/constrained [schema/Keyword] more-than-two-elements?))
(def clean-targets
  [(schema/cond-pre schema/Keyword util/non-blank-string project-path)])


;;; Checkout dependency shares

;; NOTE: This is way less precise than the spec implementation.
(def checkout-deps-shares
  (schema/constrained [(schema/pred ifn?)] util/non-empty-vec?))


;;; Test selectors

;; NOTE: Function schema simpler than spec implementation.
(def test-selectors
  {schema/Keyword (schema/cond-pre schema/Keyword
                                   (schema/pred ifn?))})

;;; REPL options

;; NOTE: Also simplified as there are no function definitions

(def repl-options
  {(schema/optional-key :prompt)            (schema/pred ifn?)
   (schema/optional-key :welcome)           (schema/pred seq?)
   (schema/optional-key :init-ns)           util/simple-symbol
   (schema/optional-key :init)              (schema/pred seq?)
   (schema/optional-key :caught)            (schema/pred ifn?)
   (schema/optional-key :skip-default-init) schema/Bool
   (schema/optional-key :host)              util/non-blank-string
   (schema/optional-key :port)              (schema/constrained util/positive-integer
                                                                (partial > 65535))
   (schema/optional-key :timeout)           util/natural-number
   (schema/optional-key :nrepl-handler)     (schema/pred seq?)
   (schema/optional-key :nrepl-middleware)  (schema/cond-pre schema/Symbol
                                                             (schema/pred ifn?))
   schema/Keyword                           schema/Any})

;;; Uberjar content management
;; NOTE: Yet another inadequate fn schema
(def uberjar-merge-with
  {(schema/cond-pre util/non-blank-string schema/Regex) [(schema/one schema/Symbol "Input-stream -> Datum")
                                                         (schema/one schema/Symbol "Datum-merger")
                                                         (schema/one schema/Symbol "Datum-printer")]})


;;; Filespecs

(def filespec
  (schema-typed-map/abstract-map-schema :type {}))
(def filespecs
  (schema/constrained [filespec] util/non-empty-vec?))

(schema-typed-map/extend-schema path-filespec  filespec [:path]  {:path path})
(schema-typed-map/extend-schema paths-filespec filespec [:paths] {:paths paths})
(schema-typed-map/extend-schema bytes-filespec filespec [:bytes] {:path path :bytes util/non-blank-string})
(schema-typed-map/extend-schema bytes-filespec filespec [:fn]    {:fn (schema/pred ifn?)})


;;; Maven POM stuff

(def manifest
  {(schema/cond-pre schema/Keyword util/non-blank-string) schema/Any})

(defn parent-args? [kv-seq]
  (util/key-val-seq? kv-seq {:relative-path util/non-blank-string}))
(def parent-args
  (schema/constrained schema/Any parent-args?))
(defn parent-vector?
  [dep-vec]
  ((util/pair-rest-cat-fn artifact parent-args) dep-vec))
(def parent
  (schema/constrained [schema/Any] parent-vector?))

(declare xml-as-vec)
(def terminal-or-recursions
  [(schema/cond-pre util/non-blank-string
                    (schema/recursive #'xml-as-vec))])

(defn map-or-terminal-or-recursions?
  [xml-vec]
  (let [data (if (map? (first xml-vec))
               (rest xml-vec)
               xml-vec)]
    (or (empty? data)
        (nil? (schema/check terminal-or-recursions data)))))

(def map-or-terminal-or-recursions
  (schema/pred map-or-terminal-or-recursions?))

(defn xml-vector? [xml-vec]
  ((util/first-rest-cat-fn schema/Keyword map-or-terminal-or-recursions) xml-vec))
(def xml-as-vec
  (schema/constrained [schema/Any] xml-vector?))

(def pom-plugin-options
  {(schema/optional-key :configuration) (schema/cond-pre util/non-blank-string xml-as-vec)
   (schema/optional-key :extensions)    (schema/cond-pre util/non-blank-string xml-as-vec)
   (schema/optional-key :executions)    (schema/cond-pre util/non-blank-string xml-as-vec)
   schema/Keyword                       schema/Any})
(def pom-plugins
  [[(schema/one dependency-name "name")
    (schema/one version "version")
    (schema/optional pom-plugin-options "options")]])


;;; Source control management

(def scm
  {(schema/optional-key :name) name-schema
   (schema/optional-key :tag)  util/non-blank-string
   (schema/optional-key :url)  url
   (schema/optional-key :dir)  util/non-blank-string
   schema/Keyword              schema/Any})


;;; Classifiers

(def classifiers
  {schema/Keyword (schema/cond-pre schema/Keyword {schema/Any schema/Any})})


;;; Project maps and permutations there of.

(defschema project-map
  {(schema/optional-key :description)               util/non-blank-string
   (schema/optional-key :url)                       url
   (schema/optional-key :mailing-list)              mailing-list
   (schema/optional-key :mailing-lists)             mailing-lists
   (schema/optional-key :license)                   license
   (schema/optional-key :licenses)                  licenses
   (schema/optional-key :min-lein-version)          semantic-version-string
   (schema/optional-key :dependencies)              dependencies
   (schema/optional-key :managed-dependencies)      dependencies
   (schema/optional-key :pedantic?)                 pedantic?
   (schema/optional-key :exclusions)                exclusions
   (schema/optional-key :plugins)                   plugins
   (schema/optional-key :repositories)              repositories
   (schema/optional-key :plugin-repositories)       repositories
   (schema/optional-key :mirrors)                   mirrors
   (schema/optional-key :local-repo)                util/non-blank-string
   (schema/optional-key :update)                    update-enum
   (schema/optional-key :checksum)                  checksum
   (schema/optional-key :offline?)                  schema/Bool
   (schema/optional-key :deploy-repositories)       repositories
   (schema/optional-key :signing)                   signing
   (schema/optional-key :certificates)              certificates
   (schema/optional-key :profiles)                  profiles
   (schema/optional-key :hooks)                     hooks
   (schema/optional-key :middleware)                middleware
   (schema/optional-key :implicit-middleware)       schema/Bool
   (schema/optional-key :implicit-hooks)            schema/Bool
   (schema/optional-key :main)                      schema/Symbol
   (schema/optional-key :aliases)                   aliases
   (schema/optional-key :release-tasks)             release-tasks
   (schema/optional-key :prep-tasks)                release-tasks
   (schema/optional-key :aot)                       aot
   (schema/optional-key :injections)                [schema/Any] ; TODO: Too lax
   (schema/optional-key :java-agents)               java-agents
   (schema/optional-key :javac-options)             javac-options
   (schema/optional-key :warn-on-reflection)        schema/Bool
   (schema/optional-key :global-vars)               global-vars
   (schema/optional-key :java-cmd)                  util/non-blank-string
   (schema/optional-key :jvm-opts)                  jvm-opts
   (schema/optional-key :eval-in)                   eval-in
   (schema/optional-key :bootclasspath)             schema/Bool
   (schema/optional-key :source-paths)              paths
   (schema/optional-key :java-source-paths)         paths
   (schema/optional-key :test-paths)                paths
   (schema/optional-key :resource-paths)            paths
   (schema/optional-key :target-path)               path
   (schema/optional-key :compile-path)              path
   (schema/optional-key :native-path)               path
   (schema/optional-key :clean-targets)             clean-targets
   (schema/optional-key :clean-non-project-classes) schema/Bool
   (schema/optional-key :checkout-deps-shares)      checkout-deps-shares
   (schema/optional-key :test-selectors)            test-selectors
   (schema/optional-key :monkeypatch-clojure-test)  schema/Bool
   (schema/optional-key :repl-options)              repl-options
   (schema/optional-key :jar-name)                  util/non-blank-string
   (schema/optional-key :uberjar-name)              util/non-blank-string
   (schema/optional-key :omit-source)               schema/Bool
   (schema/optional-key :jar-exclusions)            non-empty-vec-of-regexes
   (schema/optional-key :jar-inclusions)            non-empty-vec-of-regexes
   (schema/optional-key :uberjar-exclusions)        non-empty-vec-of-regexes
   (schema/optional-key :auto-clean)                schema/Bool
   (schema/optional-key :uberjar-merge-with)        uberjar-merge-with
   (schema/optional-key :filespecs)                 filespecs
   (schema/optional-key :manifest)                  manifest
   (schema/optional-key :pom-location)              util/non-blank-string
   (schema/optional-key :parent)                    parent
   (schema/optional-key :extensions)                [artifact]
   (schema/optional-key :pom-plugins)               pom-plugins
   (schema/optional-key :pom-addition)              xml-as-vec
   (schema/optional-key :scm)                       scm
   (schema/optional-key :install-releases?)         schema/Bool
   (schema/optional-key :deploy-branches)           deploy-branches
   (schema/optional-key :classifiers)               classifiers
   ;; Make the map schema open.
   schema/Keyword                                   schema/Any
   })

(defschema project-map-non-recursive (dissoc project-map :filespecs :profiles :checkout-deps-shares))


                                        ; (gen/generate project-argument-keys @util/generators)
