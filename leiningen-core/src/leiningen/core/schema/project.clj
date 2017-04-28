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


;;; Mailing lists

(def name           util/non-blank-string)
(def other-archives [url])
(def subscribe      (schema/cond-pre email url))
(def unsubscribe    (schema/cond-pre email url))
(def mailing-list
  {(schema/optional-key :name)           name
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



(defschema project-argument-keys
  {(schema/optional-key :description)                util/non-blank-string
   (schema/optional-key :url)                        url
   (schema/optional-key :mailing-list)               mailing-list
   (schema/optional-key :mailing-lists)              mailing-lists
   (schema/optional-key :license)                    license
   (schema/optional-key :licenses)                   licenses
   (schema/optional-key :min-lein-version)           semantic-version-string
   ;; (schema/optional-key :dependencies)
   ;; (schema/optional-key :managed-dependencies)
   ;; (schema/optional-key :pedantic?)
   ;; (schema/optional-key :exclusions)
   ;; (schema/optional-key :plugins)
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
