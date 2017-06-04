(ns leiningen.core.truss.project
  (:require [taoensso.truss            :as truss]
            [leiningen.core.truss.util :as util]))


(defn url?                     [string] (util/stregex-matches #"^(https?|ftp)://[^\s/$.?#]+\.?[^\s]*$"  string))
(defn email?                   [string] (util/stregex-matches #"\S+@\S+\.?\S+"                          string))
(defn semantic-version-string? [string] (util/stregex-matches #"(\d+)\.(\d+)\.(\d+)(-\w+)?(-SNAPSHOT)?" string))
(defn namespaced-string?       [string] (util/stregex-matches #"[^\s/]+/[^\s/]+" string))
(defn pedantic                 [val]    (truss/have [:el #{:abort :warn :ranges true false}] val))
(defn signing                  [m]      (util/req-key :gpg-key util/non-blank-string? m))
(defn certificates [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have util/non-blank-string? :in v))
(defn hooks [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have symbol? :in v))
(defn injections [v] (truss/have [:and vector? not-empty] v))
(defn javac-options [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have util/non-blank-string? :in v))
(defn jvm-opts [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have util/non-blank-string? :in v))
(defn eval-in [kw] (truss/have [:el #{:subprocess :leiningen :nrepl}] kw))
(def path util/non-blank-string?)
(defn paths [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have util/non-blank-string? :in v))
(defn non-empty-vec-of-regexes [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have util/stregex? :in v))
(defn deploy-branches [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have util/non-blank-string? :in v))


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
  (truss/have mailing-list :in v))


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
  (truss/have license :in license-vec))#'leiningen.core.truss.project/licenses


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
  (truss/have? [:and vector? not-empty] excl-vec)
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
  (truss/have #(= (count %) 2) all)
  (truss/have dependency-name? name)
  (truss/have version?         version)
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
(defn password    [e] (truss/have [:or util/non-blank-string? keyword?] e))
(defn creds       [k] (truss/have [:el #{:gpg}] k))

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
  (truss/have [:and vector? not-empty #(= (count %) 2)] all)
  (truss/have util/non-blank-string? name)
  (truss/have [:or url? repository-info-map] repo-info))
(defn repositories [repo-vec]
  (truss/have [:and vector? not-empty] repo-vec)
  (truss/have repository :in repo-vec))


;;; Mirrors

(defn mirrors [m]
  (util/>> m
    (truss/have map?)
    (util/opt-key :name         name?)
    (util/opt-key :url          url?)
    (util/opt-key :repo-manager util/boolean?)))


;;; Profiles

(declare validate-map)
(defn profiles [m]
  (truss/have map? m)
  (truss/have keyword? :in (keys m))
  (truss/have validate-map :in (vals m))
  m)


;;; Aliases

(defn command-vector [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have [:or util/non-blank-string? keyword?] :in v))

(defn do-command [[do-str & rest :as all]]
  (truss/have [:and vector? not-empty] all)
  (truss/have [:el #{"do"}] do-str)
  (truss/have [:or util/non-blank-string? command-vector] :in rest))

(defn aliases [m]
  (truss/have map? m)
  (truss/have util/non-blank-string? :in (keys m))
  (truss/have [:or do-command command-vector] :in (vals m)))


;;; Release tasks

(defn release-tasks [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have [:or util/non-blank-string? command-vector] :in v))


;;; AOT

(defn aot [something]
  (if (vector? something)
    (truss/have [:or symbol? util/stregex?] :in something)
    (truss/have [:el #{:all}] something)))


;;; Java Agents

(defn java-agent-args? [kv-seq]
  (util/key-val-seq? kv-seq {:classifier classifier?
                             :options    util/non-blank-string?}))

(defn java-agent-vector [[name version & args :as all]]
  (truss/have [:and vector? not-empty] all)
  (truss/have artifact (vector name version))
  (truss/have java-agent-args? args)
  all)

(defn java-agents [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have java-agent-vector :in v))


;;; Global vars
;; See http://stackoverflow.com/questions/43452079
(defn clojure-global-var? [sym]
  (truss/have [:el #{
   '*print-namespace-maps* '*source-path* '*command-line-args*
   '*read-eval* '*verbose-defrecords* '*print-level* '*suppress-read*
    '*print-length* '*file* '*use-context-classloader* '*err*
    '*default-data-reader-fn* '*allow-unresolved-vars* '*print-meta*
    '*compile-files* '*math-context* '*data-readers* '*clojure-version*
    '*unchecked-math* '*out* '*warn-on-reflection* '*compile-path*
    '*in* '*ns* '*assert* '*print-readably* '*flush-on-newline*
    '*agent* '*fn-loader* '*compiler-options* '*print-dup*}] sym))

(defn global-vars [m]
  (truss/have map? m)
  (truss/have clojure-global-var? :in (keys m)))


;;; Clean-targets

(defn project-path [kws]
  (truss/have [:and vector? #(>= (count %) 2)] kws)
  (truss/have keyword? :in kws))

(defn clean-targets [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have [:or keyword? util/non-blank-string? project-path] :in v))


;;; Test selectors

;; NOTE: Function schema simpler than spec implementation.
(defn test-selectors [m]
  (truss/have map? m)
  (truss/have keyword? :in (keys m))
  (truss/have [:or keyword? ifn?] :in (vals m)))

;;; REPL options

(defn repl-options [m]
  (util/>> m
    (truss/have map?)
    (util/opt-key :prompt             ifn?)
    (util/opt-key :welcome            seq?)
    (util/opt-key :init-ns            util/simple-symbol?)
    (util/opt-key :init               seq?)
    (util/opt-key :caught             ifn?)
    (util/opt-key :skip-default-init  util/boolean?)
    (util/opt-key :host               util/non-blank-string?)
    (util/opt-key :port               (every-pred integer? pos? (partial > 65535)))
    (util/opt-key :timeout            (every-pred integer? #(not (neg? %))))
    (util/opt-key :nrepl-handler      seq?)
    (util/opt-key :nrepl-middleware   #(or (symbol? %) (ifn? %)))))


;;; Uberjar content management

(defn uberjar-merger-fns [v]
  (truss/have [:and vector? #(= (count %) 3)] v)
  (truss/have symbol? :in v))

(defn uberjar-merge-with [m]
  (truss/have map? m)
  (truss/have util/stregex? :in (keys m))
  (truss/have uberjar-merger-fns :in (vals m)))


;;; Filespecs

(defmulti  filespec-map :type)
(defmethod filespec-map :path  [m] (util/req-key :path  path  m))
(defmethod filespec-map :paths [m] (util/req-key :paths paths m))
(defmethod filespec-map :fn    [m] (util/req-key :fn    ifn?  m))
(defmethod filespec-map :bytes [m]
  (util/req-key :path  path  m)
  (util/req-key :bytes util/non-blank-string? m))

(defn filespecs [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have filespec-map :in v))


;;; Maven POM stuff

(defn manifest [m]
  (truss/have map? m)
  (truss/have [:or util/non-blank-string? keyword?] :in (keys m)))


(defn parent-args? [kv-seq]
  (util/key-val-seq? kv-seq {:relative-path util/non-blank-string?}))

(defn parent [[name version & args :as all]]
  (truss/have [:and vector? not-empty] all)
  (truss/have artifact (vector name version))
  (truss/have parent-args? args)
  all)


(defn extensions [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have artifact :in v))


(declare xml-vector)
(defn terminal-or-recursion? [s]
  (truss/have [:or string? xml-vector] :in s))

(defn map-or-terminal-or-recursions?
  [xml-vec]
  (let [data (if (map? (first xml-vec))
               (rest xml-vec)
               xml-vec)]
    (or (empty? data)
        (terminal-or-recursion? data))))

(defn xml-vector [[tag & rest :as all]]
  (truss/have [:and vector? not-empty] all)
  (truss/have keyword? tag)
  (truss/have map-or-terminal-or-recursions? rest)
  all)

(defn str-or-xml? [e]
  (or (util/non-blank-string? e)
      (xml-vector             e)))

(defn pom-plugin-options [m]
  (util/>> m
  (truss/have map?)
  (util/opt-key :configuration  str-or-xml?)
  (util/opt-key :extensions     str-or-xml?)
  (util/opt-key :executions     str-or-xml?)))

(defn pom-plugin [[name version options :as all]]
  (truss/have [:and vector? #(>= (count %) 2)] all)
  (truss/have artifact (vector name version))
  (when options
    (truss/have pom-plugin-options options))
  all)

(defn pom-plugins [v]
  (truss/have [:and vector? not-empty] v)
  (truss/have pom-plugin :in v))


;;; Source control management

(defn scm [m]
  (util/>> m
    (truss/have map?)
    (util/opt-key :name  name?)
    (util/opt-key :tag   util/non-blank-string?)
    (util/opt-key :url   url?)
    (util/opt-key :dir   util/non-blank-string?)))


;;; Classifiers

(defn classifiers [m]
  (truss/have map? m)
  (truss/have keyword? :in (keys m))
  (truss/have [:or keyword? map?] :in (vals m)))


;;; Whole project map

(defn validate-map
  "Validate that m is a valid Leiningen project map."
  [m]
  (util/>> m
    (truss/have map?)
    (util/opt-key :description               util/non-blank-string?)
    (util/opt-key :url                       url?)
    (util/opt-key :mailing-list              mailing-list)
    (util/opt-key :mailing-lists             mailing-lists)
    (util/opt-key :license                   license)
    (util/opt-key :licenses                  licenses)
    (util/opt-key :min-lein-version          semantic-version-string?)
    (util/opt-key :dependencies              dependencies)
    (util/opt-key :managed-dependencies      dependencies)
    (util/opt-key :pedantic?                 pedantic)
    (util/opt-key :exclusions                exclusions)
    (util/opt-key :plugins                   plugins)
    (util/opt-key :repositories              repositories)
    (util/opt-key :plugin-repositories       repositories)
    (util/opt-key :mirrors                   mirrors)
    (util/opt-key :local-repo                util/non-blank-string?)
    (util/opt-key :update                    update-enum)
    (util/opt-key :checksum                  checksum)
    (util/opt-key :offline?                  util/boolean?)
    (util/opt-key :deploy-repositories       repositories)
    (util/opt-key :signing                   signing)
    (util/opt-key :certificates              certificates)
    (util/opt-key :profiles                  profiles)
    (util/opt-key :hooks                     hooks)
    (util/opt-key :middleware                hooks)
    (util/opt-key :implicit-middleware       util/boolean?)
    (util/opt-key :implicit-hooks            util/boolean?)
    (util/opt-key :main                      symbol?)
    (util/opt-key :aliases                   aliases)
    (util/opt-key :release-tasks             release-tasks)
    (util/opt-key :prep-tasks                release-tasks)
    (util/opt-key :aot                       aot)
    (util/opt-key :injections                injections)
    (util/opt-key :java-agents               java-agents)
    (util/opt-key :javac-options             javac-options)
    (util/opt-key :warn-on-reflection        util/boolean?)
    (util/opt-key :global-vars               global-vars)
    (util/opt-key :java-cmd                  util/non-blank-string?)
    (util/opt-key :jvm-opts                  jvm-opts)
    (util/opt-key :eval-in                   eval-in)
    (util/opt-key :bootclasspath             util/boolean?)
    (util/opt-key :source-paths              paths)
    (util/opt-key :java-source-paths         paths)
    (util/opt-key :test-paths                paths)
    (util/opt-key :resource-paths            paths)
    (util/opt-key :target-path               path)
    (util/opt-key :compile-path              path)
    (util/opt-key :native-path               path)
    (util/opt-key :clean-targets             clean-targets)
    (util/opt-key :clean-non-project-classes util/boolean?)
    ;; NOTE: Equally imprecise as the schema impl. Goes for all mentions.
    (util/opt-key :checkout-deps-shares      ifn?)
    (util/opt-key :test-selectors            test-selectors)
    (util/opt-key :monkeypatch-clojure-test  util/boolean?)
    (util/opt-key :repl-options              repl-options)
    (util/opt-key :jar-name                  util/non-blank-string?)
    (util/opt-key :uberjar-name              util/non-blank-string?)
    (util/opt-key :omit-source               util/boolean?)
    (util/opt-key :jar-exclusions            non-empty-vec-of-regexes)
    (util/opt-key :jar-inclusions            non-empty-vec-of-regexes)
    (util/opt-key :uberjar-exclusions        non-empty-vec-of-regexes)
    (util/opt-key :auto-clean                util/boolean?)
    (util/opt-key :uberjar-merge-with        uberjar-merge-with)
    (util/opt-key :filespecs                 filespecs)
    (util/opt-key :manifest                  manifest)
    (util/opt-key :pom-location              util/non-blank-string?)
    (util/opt-key :parent                    parent)
    (util/opt-key :extensions                extensions)
    (util/opt-key :pom-plugins               pom-plugins)
    (util/opt-key :pom-addition              xml-vector)
    (util/opt-key :scm                       scm)
    (util/opt-key :install-releases?         util/boolean?)
    (util/opt-key :deploy-branches           deploy-branches)
    (util/opt-key :classifiers               classifiers)
    ))

(defn validate-map-noexcept
  [m]
  (try
    (when (validate-map m)
      nil)
    (catch clojure.lang.ExceptionInfo e
      (.getMessage e))))
