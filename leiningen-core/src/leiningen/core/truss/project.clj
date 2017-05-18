(ns leiningen.core.truss.project
  (:require [taoensso.truss            :as truss]
            [leiningen.core.truss.util :as util]))


(defn url?                     [string] (util/stregex-matches #"^(https?|ftp)://[^\s/$.?#]+\.?[^\s]*$"  string))
(defn email?                   [string] (util/stregex-matches #"\S+@\S+\.?\S+"                          string))
(defn semantic-version-string? [string] (util/stregex-matches #"(\d+)\.(\d+)\.(\d+)(-\w+)?(-SNAPSHOT)?" string))
(defn namespaced-string?       [string] (util/stregex-matches #"[^\s/]+/[^\s/]+" string))
(defn pedantic                 [val]    (truss/have [:el #{:abort :warn :ranges true false}]))
(defn signing                  [m]      (util/req-key :gpg-key util/non-blank-string? m))

;;; Mailing lists

(defn name?      [string] (util/non-blank-string? string))
(defn other-archives? [v]
  (truss/have? [:and vector? not-empty] v)
  (truss/have? url? :in v))
(defn subscribe? [string] (or (email? string) (url? string)))

(defn mailing-list [m]
  (util/>> m
    (truss/have? map?)
    (util/opt-key :name           name?)
    (util/opt-key :archive        url?)
    (util/opt-key :other-archives other-archives?)
    (util/opt-key :post           email?)
    (util/opt-key :subscribe      subscribe?)
    (util/opt-key :unsubscribe    subscribe?)))

(defn mailing-lists [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have mailing-list? :in v))


;;; Licenses

(defn distribution? [key] (truss/have? [:el #{:repo :manual}] key))
(defn license [m]
  (util/>> m
    (truss/have? map?)
    (util/opt-key :name         name?)
    (util/opt-key :url          url?)
    (util/opt-key :distribution distribution?)
    (util/opt-key :comments     util/non-blank-string?)))
(defn licenses [license-vec]
  (truss/have [:and vector? not-empty] license-vec)
  (truss/have license? :in license-vec))#'leiningen.core.truss.project/licenses


;;; Dependencies

(defn dependency-name? [str-or-symbol]
  (or (util/non-blank-string? str-or-symbol)
      (symbol? str-or-symbol)))
(defn optional?      [e] (util/boolean?          e))
(defn scope?         [e] (util/non-blank-string? e))
(defn classifier?    [e] (util/non-blank-string? e))
(defn native-prefix? [e] (string?                e))
(defn extension?     [e] (util/non-blank-string? e))
(defn artifact-id?   [e] (util/non-blank-string? e))
(defn group-id?      [e] (util/non-blank-string? e))
(defn version?       [e] (util/non-blank-string? e))

(defn exclusion-arguments? [kv-seq]
  (util/key-val-seq? kv-seq {:scope         scope?
                             :classifier    classifier?
                             :native-prefix native-prefix?
                             :extension     extension?}))

(defn exclusion-vector? [excl-vec]
  (truss/have? [:and vector? not-empty]    excl-vec)
  (truss/have? dependency-name?     (first excl-vec))
  (truss/have? exclusion-arguments? (rest  excl-vec)))

(defn exclusions [excl-vec]
  (truss/have [:and vector? not-empty] excl-vec)
  (truss/have [:or dependency-name? exclusion-vector?] :in excl-vec))

(def dependency-args-map {:optional      optional?
                          :scope         scope?
                          :classifier    classifier?
                          :native-prefix native-prefix?
                          :extension     extension?
                          :exclusions    exclusions})
(defn dependency-args? [kv-seq]
  (util/key-val-seq? kv-seq dependency-args-map))


(defn artifact [[name version :as all]]
  (truss/have dependency-name? name
  (truss/have version?         version))
  all)

(defn dependency-vector [[name version & args :as all]]
  (truss/have [:and vector? not-empty] all)
  (truss/have artifact (vector name version))
  (truss/have dependency-args? args)
  all)
(defn dependencies [deps]
  (truss/have [:and vector? not-empty] deps)
  (truss/have dependency-vector :in deps))


;;; Plugins

(defn plugin-args? [kv-seq]
  (util/key-val-seq? kv-seq (merge dependency-args-map
                                   {:middleware util/boolean?
                                    :hooks      util/boolean?})))

(defn plugin-vector [[name version & args :as all]]
  (truss/have [:and vector? not-empty] all)
  (truss/have artifact (vector name version))
  (truss/have plugin-args? args)
  all)
(defn plugins [plugin-vec]
  (truss/have [:and vector? not-empty] plugin-vec)
  (truss/have plugin-vector :in plugin-vec))


;;; Repositories

(defn checksum    [k] (truss/have [:el #{:fail :warn :ignore}]   k))
(defn update-enum [k] (truss/have [:el #{:always :daily :never}] k))
(defn releases    [m]
  (truss/have map? m)
  (util/opt-key :checksum checksum    m)
  (util/opt-key :update   update-enum m))
(defn password    [e] (truss/have [:or util/non-blank-string? [:el #{:env}]] e))
(defn creds       [e] (truss/have [:el #{:gpg}]))

(defn repository-info-map
  [m]
  (util/>> m
    (util/opt-key :url           url?)
    (util/opt-key :snapshots     util/boolean?)
    (util/opt-key :sign-releases util/boolean?)
    (util/opt-key :checksum      checksum)
    (util/opt-key :update        update-enum)
    (util/opt-key :releases      releases)
    (util/opt-key :username      util/non-blank-string?)
    (util/opt-key :password      password)
    (util/opt-key :creds         creds)
    (util/opt-key :signing       signing)))

(defn repository [[name repo-info :as all]]
  (truss/have [:and vector? not-empty] all)
  (truss/have util/non-blank-string? name)
  (truss/have [:or url? repository-info-map] repo-info))
(defn repositories [repo-vec]
  (truss/have [:and vector? not-empty] repo-vec)
  (truss/have repository :in repo-vec))


(defn validate-map
  "Validate that m is a valid Leiningen project map."
  [m]
  (util/>> m
    (truss/have map?)
    (util/req-key :description               util/non-blank-string?)
    (util/req-key :url                       url?)
    (util/req-key :mailing-list              mailing-list)
    (util/opt-key :mailing-lists             mailing-lists)
    (util/req-key :license                   license)
    (util/opt-key :licenses                  licenses)
    (util/req-key :min-lein-version          semantic-version-string?)
    (util/req-key :dependencies              dependencies)
    (util/req-key :managed-dependencies      dependencies)
    (util/req-key :pedantic?                 pedantic)
    (util/req-key :exclusions                exclusions)
    (util/req-key :plugins                   plugins)
    (util/req-key :repositories              repositories)
    (util/req-key :plugin-repositories       repositories)
    ;; (util/req-key :mirrors                   )
    ;; (util/req-key :local-repo                )
    ;; (util/req-key :update                    )
    ;; (util/req-key :checksum                  )
    ;; (util/req-key :offline?                  )
    ;; (util/req-key :deploy-repositories       )
    ;; (util/req-key :signing                   )
    ;; (util/req-key :certificates              )
    ;; (util/req-key :profiles                  )
    ;; (util/req-key :hooks                     )
    ;; (util/req-key :middleware                )
    ;; (util/req-key :implicit-middleware       )
    ;; (util/req-key :implicit-hooks            )
    ;; (util/req-key :main                      )
    ;; (util/req-key :aliases                   )
    ;; (util/req-key :release-tasks             )
    ;; (util/req-key :prep-tasks                )
    ;; (util/req-key :aot                       )
    ;; (util/req-key :injections                )
    ;; (util/req-key :java-agents               )
    ;; (util/req-key :javac-options             )
    ;; (util/req-key :warn-on-reflection        )
    ;; (util/req-key :global-vars               )
    ;; (util/req-key :java-cmd                  )
    ;; (util/req-key :jvm-opts                  )
    ;; (util/req-key :eval-in                   )
    ;; (util/req-key :bootclasspath             )
    ;; (util/req-key :source-paths              )
    ;; (util/req-key :java-source-paths         )
    ;; (util/req-key :test-paths                )
    ;; (util/req-key :resource-paths            )
    ;; (util/req-key :target-path               )
    ;; (util/req-key :compile-path              )
    ;; (util/req-key :native-path               )
    ;; (util/req-key :clean-targets             )
    ;; (util/req-key :clean-non-project-classes )
    ;; (util/req-key :checkout-deps-shares      )
    ;; (util/req-key :test-selectors            )
    ;; (util/req-key :monkeypatch-clojure-test  )
    ;; (util/req-key :repl-options              )
    ;; (util/req-key :jar-name                  )
    ;; (util/req-key :uberjar-name              )
    ;; (util/req-key :omit-source               )
    ;; (util/req-key :jar-exclusions            )
    ;; (util/req-key :jar-inclusions            )
    ;; (util/req-key :uberjar-exclusions        )
    ;; (util/req-key :auto-clean                )
    ;; (util/req-key :uberjar-merge-with        )
    ;; (util/req-key :filespecs                 )
    ;; (util/req-key :manifest                  )
    ;; (util/req-key :pom-location              )
    ;; (util/req-key :parent                    )
    ;; (util/req-key :extensions                )
    ;; (util/req-key :pom-plugins               )
    ;; (util/req-key :pom-addition              )
    ;; (util/req-key :scm                       )
    ;; (util/req-key :install-releases?         )
    ;; (util/req-key :deploy-branches           )
    ;; (util/req-key :classifiers               )
    ))
