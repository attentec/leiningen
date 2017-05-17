(ns leiningen.core.truss.project
  (:require [taoensso.truss            :as truss]
            [leiningen.core.truss.util :as util]))


(defn url?   [string] (util/stregex-matches #"^(https?|ftp)://[^\s/$.?#]+\.?[^\s]*$" string))
(defn email? [string] (util/stregex-matches #"\S+@\S+\.?\S+"                         string))


;;; Mailing lists

(defn name?      [string] (util/non-blank-string? string))
(defn other-archives? [v]
  (truss/have? [:and vector? not-empty] v)
  (truss/have? url? :in v))
(defn subscribe? [string] (or (email? string) (url? string)))

(defn mailing-list? [m]
  (truss/have? map? m)
  (util/opt-key :name           name? m)
  (util/opt-key :archive        url? m)
  (util/opt-key :other-archives other-archives? m)
  (util/opt-key :post           email? m)
  (util/opt-key :subscribe      subscribe? m)
  (util/opt-key :unsubscribe    subscribe? m))

(defn mailing-lists? [v]
  (truss/have? [:and vector? not-empty] v)
  (truss/have? mailing-list? :in v))


;;; Licenses

(defn distribution? [key] (truss/have? [:el #{:repo :manual}] key))
(defn license? [m]
  (truss/have? map? m)
  (util/opt-key :name         name? m)
  (util/opt-key :url          url? m)
  (util/opt-key :distribution distribution? m)
  (util/opt-key :comments     util/non-blank-string? m))
(defn licenses? [v]
  (truss/have? [:and vector? not-empty] v)
  (truss/have? license? :in v))




(defn validate-map
  "Validate that m is a valid Leiningen project map."
  [m]
  (truss/have map? m)

  (util/req-key :description   util/non-blank-string? m)
  (util/req-key :url           url? m)
  (util/req-key :mailing-list  mailing-list? m)
  (util/opt-key :mailing-lists mailing-lists? m)
  (util/req-key :license       license?  m)
  (util/opt-key :licenses      licenses? m)
  ;; (util/req-key :min-lein-version m)
  ;; (util/req-key :dependencies m)
  ;; (util/req-key :managed-dependencies m)
  ;; (util/req-key :pedantic? m)
  ;; (util/req-key :exclusions m)
  ;; (util/req-key :plugins m)
  ;; (util/req-key :repositories m)
  ;; (util/req-key :plugin-repositories m)
  ;; (util/req-key :mirrors m)
  ;; (util/req-key :local-repo m)
  ;; (util/req-key :update m)
  ;; (util/req-key :checksum m)
  ;; (util/req-key :offline? m)
  ;; (util/req-key :deploy-repositories m)
  ;; (util/req-key :signing m)
  ;; (util/req-key :certificates m)
  ;; (util/req-key :profiles m)
  ;; (util/req-key :hooks m)
  ;; (util/req-key :middleware m)
  ;; (util/req-key :implicit-middleware m)
  ;; (util/req-key :implicit-hooks m)
  ;; (util/req-key :main m)
  ;; (util/req-key :aliases m)
  ;; (util/req-key :release-tasks m)
  ;; (util/req-key :prep-tasks m)
  ;; (util/req-key :aot m)
  ;; (util/req-key :injections m)
  ;; (util/req-key :java-agents m)
  ;; (util/req-key :javac-options m)
  ;; (util/req-key :warn-on-reflection m)
  ;; (util/req-key :global-vars m)
  ;; (util/req-key :java-cmd m)
  ;; (util/req-key :jvm-opts m)
  ;; (util/req-key :eval-in m)
  ;; (util/req-key :bootclasspath m)
  ;; (util/req-key :source-paths m)
  ;; (util/req-key :java-source-paths m)
  ;; (util/req-key :test-paths m)
  ;; (util/req-key :resource-paths m)
  ;; (util/req-key :target-path m)
  ;; (util/req-key :compile-path m)
  ;; (util/req-key :native-path m)
  ;; (util/req-key :clean-targets m)
  ;; (util/req-key :clean-non-project-classes m)
  ;; (util/req-key :checkout-deps-shares m)
  ;; (util/req-key :test-selectors m)
  ;; (util/req-key :monkeypatch-clojure-test m)
  ;; (util/req-key :repl-options m)
  ;; (util/req-key :jar-name m)
  ;; (util/req-key :uberjar-name m)
  ;; (util/req-key :omit-source m)
  ;; (util/req-key :jar-exclusions m)
  ;; (util/req-key :jar-inclusions m)
  ;; (util/req-key :uberjar-exclusions m)
  ;; (util/req-key :auto-clean m)
  ;; (util/req-key :uberjar-merge-with m)
  ;; (util/req-key :filespecs m)
  ;; (util/req-key :manifest m)
  ;; (util/req-key :pom-location m)
  ;; (util/req-key :parent m)
  ;; (util/req-key :extensions m)
  ;; (util/req-key :pom-plugins m)
  ;; (util/req-key :pom-addition m)
  ;; (util/req-key :scm m)
  ;; (util/req-key :install-releases? m)
  ;; (util/req-key :deploy-branches m)
  ;; (util/req-key :classifiers m)
  )
