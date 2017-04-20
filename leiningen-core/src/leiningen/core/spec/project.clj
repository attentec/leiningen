(ns leiningen.core.spec.project
  (:require [clojure.spec             :as spec]
            [clojure.spec.gen         :as gen]
            [clojure.spec.test        :as test]
            [clojure.string           :as str]
            [leiningen.core.project   :as proj]
            [leiningen.core.spec.util :as util]))

;;; Whole project map or defproject argument list.
(def project-argument-keys
  [::proj/description
   ::proj/url
   ::proj/mailing-list
   ::proj/mailing-lists
   ::proj/license
   ::proj/licenses
   ::proj/min-lein-version
   ::proj/dependencies
   ::proj/managed-dependencies
   ::proj/pedantic?
   ::proj/exclusions
   ::proj/plugins
   ::proj/repositories
   ::proj/plugin-repositories
   ::proj/mirrors
   ::proj/local-repo
   ::proj/update
   ::proj/checksum
   ::proj/offline?
   ::proj/deploy-repositories
   ::proj/signing
   ::proj/certificates
   ::proj/profiles
   ::proj/hooks
   ::proj/middleware
   ::proj/implicit-middleware
   ::proj/implicit-hooks
   ::proj/main
   ::proj/aliases
   ::proj/release-tasks
   ::proj/prep-tasks
   ::proj/aot
   ::proj/injections
   ::proj/java-agents
   ::proj/javac-options
   ::proj/warn-on-reflection
   ::proj/global-vars
   ::proj/java-cmd
   ::proj/jvm-opts
   ::proj/eval-in
   ::proj/bootclasspath
   ::proj/source-paths
   ::proj/java-source-paths
   ::proj/test-paths
   ::proj/resource-paths
   ::proj/target-path
   ::proj/compile-path
   ::proj/native-path
   ::proj/clean-targets
   ::proj/clean-non-project-classes
   ::proj/checkout-deps-shares
   ::proj/test-selectors
   ::proj/monkeypatch-clojure-test
   ::proj/repl-options
   ::proj/jar-name
   ::proj/uberjar-name
   ::proj/omit-source
   ::proj/jar-exclusions
   ::proj/uberjar-exclusions
   ::proj/auto-clean
   ::proj/uberjar-merge-with
   ::proj/filespecs
   ; ::proj/manifest
   ; ::proj/pom-location
   ; ::proj/parent
   ; ::proj/extensions
   ; ::proj/pom-plugins
   ; ::proj/pom-addition
   ::proj/scm
   ; ::proj/install-releases
   ; ::proj/deplay-branches
   ; ::proj/classifiers
   ])


;; TODO: Remove required keyword, it's only there so that
;; `spec/exercise` doesn't barf. See SO question:
;;http://stackoverflow.com/questions/43339543/
;; TODO: Add xor requirement for singularis and pluralis of
;; mailing-lists and licenses with util/key-xor?. Currently impossible
;; due to the above mentioned bug.
(spec/def ::proj/project-args
  (eval `(spec/keys* :opt-un ~project-argument-keys
                     :req-un [::proj/description])))

(spec/def ::proj/project-map
  (eval `(spec/keys :opt-un ~project-argument-keys
                    :req-un [::proj/description])))


;;;; Minor keys in project-argument-keys from top to bottom.

(spec/def ::proj/description   ::util/non-blank-string)
;; Source, diegoperini: https://mathiasbynens.be/demo/url-regex
;; TODO: Replace with java.net.URL for acceptance or perhaps with
;; https://github.com/SparkFund/useful-specs/
(spec/def ::proj/url           (util/stregex #"^(https?|ftp)://[^\s/$.?#].[^\s]*$"))
;; Won't match email adresses like me@google where the company owns a tld.
(spec/def ::proj/email         (util/stregex #"/\S+@\S+\.\S+/"))
(spec/def ::proj/pedantic?     #{:abort :warn :ranges true false})
(spec/def ::proj/local-repo    ::util/non-blank-string)
(spec/def ::proj/offline?      boolean?)
(spec/def ::proj/signing       (spec/map-of #{:gpg-key} ::util/non-blank-string))
(spec/def ::proj/certificates  (spec/coll-of ::util/non-blank-string :kind vector? :min-count 1))
(spec/def ::proj/hooks         (spec/coll-of symbol? :kind vector? :min-count 1))
(spec/def ::proj/middleware    (spec/coll-of symbol? :kind vector? :min-count 1))
(spec/def ::proj/implicit-hooks boolean?)
(spec/def ::proj/implicit-middleware boolean?)
(spec/def ::proj/main          symbol?)
;; TODO: Injections spec is too simple.
(spec/def ::proj/injections    (spec/coll-of any? :kind vector? :gen-max 3))
(spec/def ::proj/javac-options (spec/coll-of ::util/non-blank-string :kind vector? :min-count 1))
(spec/def ::proj/warn-on-reflection  boolean?)
(spec/def ::proj/java-cmd      ::util/non-blank-string)
(spec/def ::proj/jvm-opts      (spec/coll-of ::util/non-blank-string :kind vector? :min-count 1))
(spec/def ::proj/eval-in       #{:subprocess :leiningen :nrepl})
(spec/def ::proj/bootclasspath boolean?)
(spec/def ::proj/clean-non-project-classes boolean?)
(spec/def ::proj/monkeypatch-clojure-test boolean?)
(spec/def ::proj/jar-name      ::util/non-blank-string)
(spec/def ::proj/uberjar-name  ::util/non-blank-string)
(spec/def ::proj/omit-source   boolean?)
(spec/def ::proj/jar-exclusions (spec/coll-of ::util/stregex :kind vector? :min-count 1))
(spec/def ::proj/uberjar-exclusions (spec/coll-of ::util/stregex :kind vector? :min-count 1))
(spec/def ::proj/auto-clean     boolean?)


;;; Mailing lists

(spec/def ::proj/name           ::util/non-blank-string)
(spec/def ::proj/archive        ::proj/url)
(spec/def ::proj/other-archives (spec/coll-of ::proj/url :min-count 1 :gen-max 3))
(spec/def ::proj/post           ::proj/email)
(spec/def ::proj/subscribe      (spec/or ::proj/email ::proj/url))
(spec/def ::proj/unsubscribe    (spec/or ::proj/email ::proj/url))

(spec/def ::proj/mailing-list
  (spec/keys :opt-un [::proj/name ::proj/archive ::proj/other-archives
                      ::proj/post ::proj/subscribe ::proj/unsubscribe]))

(spec/def ::proj/mailing-lists (spec/coll-of ::proj/mailing-list :min-count 1 :gen-max 3))


;;; Licenses

(spec/def ::proj/distribution #{:repo :manual})
(spec/def ::proj/comments     ::util/non-blank-string)

(spec/def ::proj/license
  (spec/keys :opt-un [::proj/name ::proj/url ::proj/distribution ::proj/comments]))

(spec/def ::proj/licenses
  (spec/coll-of ::proj/license :min-count 1 :gen-max 3))


;;; Leiningen version

(spec/def ::proj/semantic-version-string
  (util/stregex #"(\d+)\.(\d+)\.(\d+)(-\w+)?(-SNAPSHOT)?"))

(spec/def ::proj/min-lein-version ::proj/semantic-version-string)


;;; Dependencies

(spec/def ::proj/dependency-name
  (spec/alt
   :namespaced-string ::util/namespaced-string
   :bare-string       ::util/non-blank-string
   :namespaced-symbol qualified-symbol?
   :bare-symbol       simple-symbol?))

(spec/def ::proj/artifact-id ::util/non-blank-string)
(spec/def ::proj/group-id    ::util/non-blank-string)
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
(spec/def ::proj/scope         ::util/non-blank-string)
(spec/def ::proj/classifier    ::util/non-blank-string)
(spec/def ::proj/native-prefix ::util/non-blank-string)
(spec/def ::proj/extension     ::util/non-blank-string)
(spec/def ::proj/exclusions    (spec/coll-of ::proj/exclusion :gen-max 2 :kind vector? :min-count 1))
(spec/def ::proj/dependency-args
  (spec/keys*
   :opt-un [::proj/optional ::proj/scope ::proj/classifier
            ::proj/native-prefix ::proj/extension ::proj/exclusions]))
(spec/def ::proj/exclusion-args
  (spec/keys*
   :opt-un [::proj/scope ::proj/classifier
            ::proj/native-prefix ::proj/extension]))

(spec/def ::proj/version ::util/non-blank-string)

(spec/def ::proj/dependency-vector
  (util/vcat :name      ::proj/dependency-name
             :version   ::proj/version
             :arguments ::proj/dependency-args))

(spec/def ::proj/dependency-map
  (spec/keys :req [::proj/artifact-id ::proj/group-id ::proj/version]))

(spec/def ::proj/exclusion-map
  (spec/keys :req [::proj/artifact-id ::proj/group-id]))

(spec/def ::proj/dependencies
  (spec/coll-of ::proj/dependency-vector :kind vector? :min-count 1))

(spec/def ::proj/managed-dependencies
  (spec/coll-of ::proj/dependency-vector :kind vector? :min-count 1))


;;; Plugins

(spec/def :leiningen.core.project.plugin/middleware boolean?)
(spec/def ::proj/hooks boolean?)

;; TODO: This duplicates the whole ::proj/dependency-args. See SO question:
;; http://stackoverflow.com/questions/43388710/ and Alex Miller on irc:
;; "The easiest way to do what you're talking about is to repeat the valid option keys."
(spec/def ::proj/plugin-args
  (spec/keys* :opt-un [::proj/optional ::proj/scope ::proj/classifier
                       ::proj/native-prefix ::proj/extension ::proj/exclusions
                       :leiningen.core.project.plugin/middleware ::proj/hooks]))

(spec/def ::proj/plugin-vector
  (util/vcat :name      ::proj/dependency-name
             :version   ::proj/version
             :arguments ::proj/plugin-args))

(spec/def ::proj/plugins
  (spec/coll-of ::proj/plugin-vector :gen-max 3 :kind vector? :min-count 1))


;;; Repositories

(spec/def ::proj/snapshots     boolean?)
(spec/def ::proj/sign-releases boolean?)
(spec/def ::proj/checksum      #{:fail :warn :ignore})
(spec/def ::proj/update        #{:always :daily :never})
(spec/def ::proj/releases      #{:checksum :fail :update :always})
(spec/def ::proj/username      ::util/non-blank-string)
(spec/def ::proj/password      ::util/non-blank-string)
(spec/def ::proj/creds         #{:gpg})

(spec/def ::proj/repository-info-map
  (spec/keys :opt-un [::proj/url ::proj/snapshots ::proj/sign-releases
                      ::proj/checksum ::proj/update ::proj/releases
                      ::proj/username ::proj/password ::proj/creds]))

(spec/def ::proj/repositories
  (spec/coll-of (spec/cat :name ::util/non-blank-string
                          :info (spec/alt :url ::proj/url
                                          :map ::proj/repository-info-map))
                :gen-max 3 :kind vector? :min-count 1))

(spec/def ::proj/plugin-repositories ::proj/repositories)
(spec/def ::proj/deploy-repositories ::proj/repositories)


;;; Mirrors

(spec/def ::proj/repo-manager  boolean?)

(spec/def ::proj/mirrors
  (spec/map-of (spec/or :regex ::util/stregex
                        :string ::util/non-blank-string)
               (spec/keys :opt-un [::proj/name ::proj/url ::proj/repo-manager])
               :gen-max 3))


;;; Profiles

(spec/def ::proj/profiles
  (spec/map-of keyword?
               (eval `(spec/keys ;; Prevent infinite recursion by removing oneself.
                       :opt-un ~(remove #{::proj/profiles} project-argument-keys)))
               :gen-max 3))


;;; Aliases

(spec/def ::proj/command-vector
  (spec/coll-of ::util/non-blank-string :kind vector? :min-count 1))

(spec/def ::proj/do-command
  (util/vcat :do          #{"do"}
             :command-seq (spec/+ (spec/or :str        ::util/non-blank-string
                                           :cmd-vector ::proj/command-vector))))

(spec/def ::proj/aliases
  (spec/map-of ::util/non-blank-string
               (spec/or :command-vector ::proj/command-vector
                        :do-command     ::proj/do-command)
               :gen-max 3))


;;; Tasks

(spec/def ::proj/release-tasks
  (spec/coll-of ::proj/command-vector :kind vector? :min-count 1 :gen-max 3))

(spec/def ::proj/prep-tasks ::proj/release-tasks)


;;; AOT, ahead of time compilation

(spec/def ::proj/aot
  (spec/or :all      #{:all}
           :sequence (spec/coll-of (spec/or :regex  ::util/stregex
                                            :symbol symbol?)
                                   :kind vector? :gen-max 3)))

;;; Java agents

(spec/def ::proj/options ::util/non-blank-string)
(spec/def ::proj/java-agent-args
  (spec/keys* :opt-un [::proj/classifier ::proj/options]))

(spec/def ::proj/java-agents
  (spec/coll-of
   (util/vcat :name      ::proj/dependency-name
              :version   ::proj/version
              :arguments ::proj/java-agent-args)
   :kind vector? :gen-max 3 :min-count 1))


;;; Global vars
;; See http://stackoverflow.com/questions/43452079
(spec/def ::proj/clojure-global-vars
  #{'*print-namespace-maps* '*source-path* '*command-line-args*
    '*read-eval* '*verbose-defrecords* '*print-level* '*suppress-read*
    '*print-length* '*file* '*use-context-classloader* '*err*
    '*default-data-reader-fn* '*allow-unresolved-vars* '*print-meta*
    '*compile-files* '*math-context* '*data-readers* '*clojure-version*
    '*unchecked-math* '*out* '*warn-on-reflection* '*compile-path*
    '*in* '*ns* '*assert* '*print-readably* '*flush-on-newline*
    '*agent* '*fn-loader* '*compiler-options* '*print-dup*})

;; TODO: Make more precise. See http://stackoverflow.com/questions/43453776
(spec/def ::proj/global-vars
  (spec/map-of ::proj/clojure-global-vars
               any?))


;;; Paths

(spec/def ::proj/source-paths      (spec/coll-of ::util/non-blank-string :kind vector? :min-count 1))
(spec/def ::proj/java-source-paths (spec/coll-of ::util/non-blank-string :kind vector? :min-count 1))
(spec/def ::proj/test-paths        (spec/coll-of ::util/non-blank-string :kind vector? :min-count 1))
(spec/def ::proj/resource-paths    (spec/coll-of ::util/non-blank-string :kind vector? :min-count 1))
(spec/def ::proj/target-path       ::util/non-blank-string)
(spec/def ::proj/compile-path      ::util/non-blank-string)
(spec/def ::proj/native-path       ::util/non-blank-string)

(spec/def ::proj/clean-targets
  (spec/coll-of (spec/alt :proj-key  keyword?
                          :path      ::util/non-blank-string
                          :proj-path (spec/coll-of keyword?
                                      :kind vector? :min-count 2))
                :kind vector? :min-count 1))

(spec/def ::proj/checkout-deps-shares
  ::proj/clean-targets)


;;; Test selectors

(spec/def ::proj/test-selectors
  (spec/map-of keyword? (spec/or :keyword keyword?
                                 :code    ::util/predicate)))


;;; REPL options

(spec/fdef ::ns->str
           :args (spec/cat :arg ::util/namespace)
           :ret  string?)
(spec/fdef ::exception-printer
           :args (spec/+ ::util/exception)
           :ret nil?)
(spec/def ::proj/prompt            ::ns->str)
(spec/def ::proj/welcome           ::util/nullary-fn)
(spec/def ::proj/init-ns           ::util/namespace-symbol)
(spec/def ::proj/init              ::util/nullary-fn)
(spec/def ::proj/caught            ::exception-printer)
(spec/def ::proj/skip-default-init boolean?)
(spec/def ::proj/host              ::util/non-blank-string)
(spec/def ::proj/port              (spec/and ::util/positive-integer (partial > 65535)))
(spec/def ::proj/timeout           nat-int?)
(spec/def ::proj/nrepl-handler     ::util/nullary-fn)
(spec/def ::proj/nrepl-middleware  (spec/or ::util/unary-fn qualified-symbol?))

(spec/def ::proj/repl-options
  (spec/keys :opt-un [::proj/prompt ::proj/welcome ::proj/init-ns
                      ::proj/init   ::proj/caught  ::proj/skip-default-init
                      ::proj/host   ::proj/port    ::proj/timeout
                      ::proj/nrepl-handler         ::proj/nrepl-middleware]))

;;; Uberjar content management

(spec/def ::proj/uberjar-merge-with
  (spec/map-of (spec/or :regex  ::util/stregex
                        :string string?)
               (spec/cat :input-stream->datum symbol?
                         :datum-merger        symbol?
                         :datum-printer       symbol?)))

;;; Filespecs

(spec/fdef ::filespec-fn
           :args (spec/cat :project-map :filespec-no-fn/project-map)
           :ret  :filespec-no-fn/filespec)

(spec/def ::proj/path    ::util/non-blank-string)
(spec/def ::proj/paths   (spec/coll-of ::util/non-blank-string :kind vector? :min-count 1))
(spec/def ::proj/bytes   ::util/non-blank-string)
(spec/def :filespec/fn   ::filespec-fn)
(spec/def :filespec/type #{:path :paths :bytes :fn})

(defmulti  filespec-type :type)
(defmethod filespec-type :path  [_] (spec/keys :req-un [:filespec/type ::proj/path]))
(defmethod filespec-type :paths [_] (spec/keys :req-un [:filespec/type ::proj/paths]))
(defmethod filespec-type :bytes [_] (spec/keys :req-un [:filespec/type ::proj/path ::proj/bytes]))
(defmethod filespec-type :fn    [_] (spec/keys :req-un [:filespec/type :filespec/fn]))

(spec/def ::proj/filespec (spec/multi-spec filespec-type :type))
(spec/def ::proj/filespecs
  (spec/coll-of ::proj/filespec :kind vector? :min-count 1 :gen-max 3))

;; The below repetition exists to avoid infinite loops in spec
(spec/def ::proj/project-map-no-filespec
  (eval `(spec/keys :opt-un ~(remove #{::proj/filespecs} project-argument-keys)
                    :req-un [::proj/description])))

(spec/def :filespec-no-fn/type  #{:path :paths :bytes})

(defmulti  filespec-type-no-fn :type)
(defmethod filespec-type-no-fn :path  [_] (spec/keys :req-un [:filespec-no-fn/type ::proj/path]))
(defmethod filespec-type-no-fn :paths [_] (spec/keys :req-un [:filespec-no-fn/type ::proj/paths]))
(defmethod filespec-type-no-fn :bytes [_] (spec/keys :req-un [:filespec-no-fn/type ::proj/path ::proj/bytes]))

(spec/def :filespec-no-fn/filespec  (spec/multi-spec filespec-type-no-fn :type))


;;; Source control management

(spec/def ::proj/tag ::util/non-blank-string)
(spec/def ::proj/dir ::util/non-blank-string)
(spec/def ::proj/scm
  (spec/keys :opt-un [::proj/name ::proj/tag ::proj/url ::proj/dir]))


;;;; Function defenitions


;;; Dependencies

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


;;; Big picture

(spec/fdef proj/defproject
          :args (spec/cat :project-name symbol?
                          :version      ::proj/version
                          :arguments    ::proj/project-args)
          :ret symbol?)


;; (spec/exercise-fn `proj/defproject)
