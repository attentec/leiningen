(ns leiningen.core.spec.project
  (:require [clojure.spec             :as spec]
            [clojure.spec.gen         :as gen]
            [clojure.spec.test        :as test]
            [clojure.string           :as str]
            [leiningen.core.project   :as proj]
            [leiningen.core.spec.util :as util]))

(spec/def ::non-blank-string
  (spec/and string? #(not (str/blank? %))))

(spec/def ::namespaced-string
  (util/stregex #"[^\s/]+/[^\s/]+"))

;;; Whole project map or defproject argument list.

(def project-argument-keys
  [::proj/description
   ::proj/url
   ::mailing-list
   ::mailing-lists
   ; ::license
   ; ::licenses
   ; ::min-lein-version
   ::proj/dependencies
   ; ::managed-dependencies
   ; ::pedantic?
   ; ::exclusions
   ; ::plugins
   ; ::repositories
   ; ::plugin-repositories
   ; ::mirrors
   ; ::local-repo
   ; ::update
   ; ::checksum
   ; ::offline?
   ; ::signing
   ; ::certificates
   ; ::profiles
   ; ::hooks
   ; ::middleware
   ; ::implicit-middleware
   ; ::implicit-hooks
   ; ::main
   ; ::aliases
   ; ::release-tasks
   ; ::prep-tasks
   ; ::aot
   ; ::injections
   ; ::java-agents
   ; ::javac-options
   ; ::warn-on-reflection
   ; ::global-vars
   ; ::java-cmd
   ; ::jvm-opts
   ; ::eval-in
   ; ::bootclasspath
   ; ::source-paths
   ; ::java-source-paths
   ; ::test-paths
   ; ::resource-paths
   ; ::target-path
   ; ::compile-path
   ; ::native-path
   ; ::clean-targets
   ; ::clean-non-project-classes
   ; ::checkout-deps-shares
   ; ::test-selectors
   ; ::monkeypatch-clojure-test
   ; ::repl-options
   ; ::jar-name
   ; ::uberjar-name
   ; ::omit-source
   ; ::jar-exclusions
   ; ::uberjar-exclusions
   ; ::auto-clean
   ; ::uberjar-merge-with
   ; ::scm
   ; ::validate
   ])

(spec/def ::proj/project-args
  (eval `(spec/keys* :opt-un ~project-argument-keys
                     :req-un [::proj/description])))

(spec/def ::proj/project-map
  (eval `(spec/keys :opt-un ~project-argument-keys
                    :req-un [::proj/description])))


;;; Simple top level project fields.

(spec/def ::proj/description ::non-blank-string)

;; Source, diegoperini: https://mathiasbynens.be/demo/url-regex
(spec/def ::proj/url
  (util/stregex #"^(https?|ftp)://[^\s/$.?#].[^\s]*$"))

;; Won't match email adresses like me@google where the company owns a tld.
(spec/def ::proj/email
  (util/stregex #"/\S+@\S+\.\S+/"))


;;; Mailing lists

(spec/def ::proj/name           ::non-blank-string)
(spec/def ::proj/archive        ::proj/url)
(spec/def ::proj/other-archives (spec/coll-of ::proj/url :min-count 1))
(spec/def ::proj/post           ::proj/email)
(spec/def ::proj/subscribe      (spec/or ::proj/email ::proj/url))
(spec/def ::proj/unsubscribe    (spec/or ::proj/email ::proj/url))

(spec/def ::proj/mailing-list
  (spec/keys :opt-un [::proj/name ::proj/archive ::proj/other-archives
                      ::proj/post ::proj/subscribe ::proj/unsubscribe]))

(spec/def ::proj/mailing-lists (spec/coll-of ::mailing-list :min-count 1))
(spec/exercise ::proj/mailing-list)


;;; Dependencies

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


(spec/def ::proj/dependencies
  (spec/coll-of ::proj/dependency-vector :kind vector?))



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
