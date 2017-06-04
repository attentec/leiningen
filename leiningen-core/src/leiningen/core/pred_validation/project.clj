(ns leiningen.core.pred-validation.project
  (:require [leiningen.core.pred-validation.util :as util]))


(defn url?                     [string] (util/stregex-matches #"^(https?|ftp)://[^\s/$.?#]+\.?[^\s]*$"  string))
(defn email?                   [string] (util/stregex-matches #"\S+@\S+\.?\S+"                          string))
(defn semantic-version-string? [string] (util/stregex-matches #"(\d+)\.(\d+)\.(\d+)(-\w+)?(-SNAPSHOT)?" string))
(defn namespaced-string?       [string] (util/stregex-matches #"[^\s/]+/[^\s/]+" string))
(defn pedantic?                [val]    (contains? #{:abort :warn :ranges true false} val))
(defn signing?                 [m]      (util/req-key? :gpg-key util/non-blank-string? m))

(defn certificates? [v]
  (and ((every-pred vector? not-empty) v)
       (every? util/non-blank-string?  v)))
(defn hooks? [v]
  (and ((every-pred vector? not-empty) v)
       (every? symbol? v)))
(defn injections? [v] ((every-pred vector? not-empty) v))
(defn javac-options? [v]
  (and ((every-pred vector? not-empty) v)
       (every? util/non-blank-string?  v)))
(defn jvm-opts? [v]
  (and ((every-pred vector? not-empty) v)
       (every? util/non-blank-string?  v)))
(defn eval-in? [kw] (contains? #{:subprocess :leiningen :nrepl} kw))
(def path? util/non-blank-string?)
(defn paths? [v]
  (and ((every-pred vector? not-empty) v)
       (every? util/non-blank-string?  v)))
(defn non-empty-vec-of-regexes? [v]
  (and ((every-pred vector? not-empty) v)
       (every? util/stregex? v)))
(defn deploy-branches? [v]
  (and ((every-pred vector? not-empty) v)
       (every? util/non-blank-string?  v)))



;;; Mailing lists

(defn name?      [string] (util/non-blank-string? string))
(defn other-archives? [v]
  (and ((every-pred vector? not-empty) v)
       (every? url? v)))
(defn subscribe? [string] (or (email? string) (url? string)))

(defn mailing-list? [m]
  (util/and>> m
    map?
    (util/opt-key? :name           name?)
    (util/opt-key? :archive        url?)
    (util/opt-key? :other-archives other-archives?)
    (util/opt-key? :post           email?)
    (util/opt-key? :subscribe      subscribe?)
    (util/opt-key? :unsubscribe    subscribe?)))


(defn mailing-lists? [v]
  (and ((every-pred vector? not-empty) v)
       (every? mailing-list? v)))


;;; Licenses

(defn distribution? [key] (contains? #{:repo :manual} key))
(defn license? [m]
  (util/and>> m
    map?
    (util/opt-key? :name         name?)
    (util/opt-key? :url          url?)
    (util/opt-key? :distribution distribution?)
    (util/opt-key? :comments     util/non-blank-string?)))
(defn licenses? [license-vec]
  (and ((every-pred vector? not-empty) license-vec)
       (every? license? license-vec)))


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
  (and ((every-pred vector? not-empty #(= (count %) 2)) excl-vec)
       (dependency-name?     (first excl-vec))
       (exclusion-arguments? (rest  excl-vec))))

(defn exclusions? [excl-vec]
  (and ((every-pred vector? not-empty) excl-vec)
       (every? (some-fn dependency-name? exclusion-vector?) excl-vec)))

(def dependency-args-map {:optional      optional?
                          :scope         scope?
                          :classifier    classifier?
                          :native-prefix native-prefix?
                          :extension     extension?
                          :exclusions    exclusions?})
(defn dependency-args? [kv-seq]
  (util/key-val-seq? kv-seq dependency-args-map))

(defn artifact? [[name version :as all]]
  (and (= (count all) 2)
       (dependency-name? name)
       (version?         version)))

(defn dependency-vector? [[name version & args :as all]]
  (and ((every-pred vector? not-empty) all)
       (artifact? (vector name version))
       (dependency-args? args)))
(defn dependencies? [deps]
  (and ((every-pred vector? not-empty) deps)
       (every? dependency-vector? deps)))


;;; Plugins

(defn plugin-args? [kv-seq]
  (util/key-val-seq? kv-seq (merge dependency-args-map
                                   {:middleware util/boolean?
                                    :hooks      util/boolean?})))

(defn plugin-vector? [[name version & args :as all]]
  (and ((every-pred vector? not-empty #(= (count %) 2)) all)
       (artifact? (vector name version))
       (plugin-args? args)))
(defn plugins? [plugin-vec]
  (and ((every-pred vector? not-empty) plugin-vec)
       (every? plugin-vector? plugin-vec)))


;;; Repositories

(defn checksum?    [k] (contains? #{:fail :warn :ignore}   k))
(defn update-enum? [k] (contains? #{:always :daily :never} k))
(defn releases?    [m]
  (util/and>>
    map?
    (util/opt-key? :checksum checksum?)
    (util/opt-key? :update   update-enum?)))
(defn password?    [e] ((some-fn util/non-blank-string? keyword?) e))
(defn creds?       [e] (contains? #{:gpg} e))

(defn repository-info-map?
  [m]
  (util/and>> m
    map?
    (util/opt-key? :url           url?)
    (util/opt-key? :snapshots     util/boolean?)
    (util/opt-key? :sign-releases util/boolean?)
    (util/opt-key? :checksum      checksum?)
    (util/opt-key? :update        update-enum?)
    (util/opt-key? :releases      releases?)
    (util/opt-key? :username      util/non-blank-string?)
    (util/opt-key? :password      password?)
    (util/opt-key? :creds         creds?)
    (util/opt-key? :signing       signing?)))

(defn repository? [[name repo-info :as all]]
  (and ((every-pred vector? not-empty #(= (count %) 2)) all)
       (util/non-blank-string? name)
       ((some-fn url? repository-info-map?) repo-info)))
(defn repositories? [repo-vec]
  (and ((every-pred vector? not-empty) repo-vec)
       (every? repository? repo-vec)))


;;; Mirrors

(defn mirrors? [m]
  (util/and>> m
    map?
    (util/opt-key? :name         name?)
    (util/opt-key? :url          url?)
    (util/opt-key? :repo-manager util/boolean?)))


;;; Profiles

(declare valid-map?)
(defn profiles? [m]
  (and (map? m)
       (every? keyword? (keys m))
       (every? valid-map? (vals m))))


;;; Aliases

(defn command-vector? [v]
  (and ((every-pred vector? not-empty) v)
       (every? (some-fn util/non-blank-string? keyword?) v)))

(defn do-command? [[do-str & rest :as all]]
  (and ((every-pred vector? not-empty) all)
       (contains? #{"do"} do-str)
       (every? (some-fn util/non-blank-string? command-vector?) rest)))

(defn aliases? [m]
  (and (map? m)
       (every? util/non-blank-string? (keys m))
       (every? (some-fn do-command? command-vector?) (vals m))))


;;; Release tasks

(defn release-tasks? [v]
  (and ((every-pred vector? not-empty) v)
       (every? (some-fn util/non-blank-string? command-vector?) v)))


;;; AOT

(defn aot? [something]
  (if (vector? something)
    (every? (some-fn symbol? util/stregex?) something)
    (contains? #{:all} something)))


;;; Java Agents

(defn java-agent-args? [kv-seq]
  (util/key-val-seq? kv-seq {:classifier classifier?
                             :options    util/non-blank-string?}))

(defn java-agent-vector? [[name version & args :as all]]
  (and ((every-pred vector? not-empty) all)
       (artifact? (vector name version))
       (java-agent-args? args)))

(defn java-agents? [v]
  (and ((every-pred vector? not-empty) v)
       (every? java-agent-vector? v)))


;;; Global vars
;; See http://stackoverflow.com/questions/43452079
(defn clojure-global-var? [sym]
  (contains?
   #{'*print-namespace-maps* '*source-path* '*command-line-args*
     '*read-eval* '*verbose-defrecords* '*print-level* '*suppress-read*
     '*print-length* '*file* '*use-context-classloader* '*err*
     '*default-data-reader-fn* '*allow-unresolved-vars* '*print-meta*
     '*compile-files* '*math-context* '*data-readers* '*clojure-version*
     '*unchecked-math* '*out* '*warn-on-reflection* '*compile-path*
     '*in* '*ns* '*assert* '*print-readably* '*flush-on-newline*
     '*agent* '*fn-loader* '*compiler-options* '*print-dup*} sym))

(defn global-vars? [m]
  (and (map? m)
       (every? clojure-global-var? (keys m))))


;;; Clean-targets

(defn project-path? [kws]
  (and ((every-pred vector? #(>= (count %) 2)) kws)
       (every? keyword? kws)))

(defn clean-targets? [v]
  (and ((every-pred vector? not-empty) v)
       (every? (some-fn keyword? util/non-blank-string? project-path?) v)))


;;; Test selectors

(defn test-selectors? [m]
  (and (map? m)
       (every? keyword? (keys m))
       (every? (some-fn keyword? ifn?) (vals m))))

;;; REPL options

(defn repl-options? [m]
  (util/and>> m
    map?
    (util/opt-key? :prompt             ifn?)
    (util/opt-key? :welcome            seq?)
    (util/opt-key? :init-ns            util/simple-symbol?)
    (util/opt-key? :init               seq?)
    (util/opt-key? :caught             ifn?)
    (util/opt-key? :skip-default-init  util/boolean?)
    (util/opt-key? :host               util/non-blank-string?)
    (util/opt-key? :port               (every-pred integer? pos? (partial > 65535)))
    (util/opt-key? :timeout            (every-pred integer? #(not (neg? %))))
    (util/opt-key? :nrepl-handler      seq?)
    (util/opt-key? :nrepl-middleware   #(or (symbol? %) (ifn? %)))))


;;; Uberjar content management

(defn uberjar-merger-fns? [v]
  (and ((every-pred vector? #(= (count %) 3)) v)
       (every? symbol? v)))

(defn uberjar-merge-with? [m]
  (and (map? m)
       (every? util/stregex? (keys m))
       (every? uberjar-merger-fns? (vals m))))


;;; Filespecs

(defmulti  filespec-map? :type)
(defmethod filespec-map? :path  [m] (util/req-key? :path  path?  m))
(defmethod filespec-map? :paths [m] (util/req-key? :paths paths? m))
(defmethod filespec-map? :fn    [m] (util/req-key? :fn    ifn?  m))
(defmethod filespec-map? :bytes [m]
  (and (util/req-key? :path  path?  m)
       (util/req-key? :bytes util/non-blank-string? m)))

(defn filespecs? [v]
  (and ((every-pred vector? not-empty) v)
       (every? filespec-map? v)))


;;; Maven POM stuff

(defn manifest? [m]
  (and (map? m)
       (every? (some-fn util/non-blank-string? keyword?) (keys m))))


(defn parent-args? [kv-seq]
  (util/key-val-seq? kv-seq {:relative-path util/non-blank-string?}))

(defn parent? [[name version & args :as all]]
  (and ((every-pred vector? not-empty) all)
       (artifact? (vector name version))
       (parent-args? args)))


(defn extensions? [v]
  (and ((every-pred vector? not-empty) v)
       (every? artifact? v)))


(declare xml-vector)
(defn terminal-or-recursion? [s]
  (every? (some-fn string? xml-vector) s))

(defn map-or-terminal-or-recursions?
  [xml-vec]
  (let [data (if (map? (first xml-vec))
               (rest xml-vec)
               xml-vec)]
    (or (empty? data)
        (terminal-or-recursion? data))))

(defn xml-vector? [[tag & rest :as all]]
  (and ((every-pred vector? not-empty) all)
       (keyword? tag)
       (map-or-terminal-or-recursions? rest)))

(defn str-or-xml? [e]
  (or (util/non-blank-string? e)
      (xml-vector?            e)))

(defn pom-plugin-options? [m]
  (util/and>> m
    map?
    (util/opt-key? :configuration  str-or-xml?)
    (util/opt-key? :extensions     str-or-xml?)
    (util/opt-key? :executions     str-or-xml?)))

(defn pom-plugin? [[name version options :as all]]
  (and ((every-pred vector? #(>= (count %) 2)) all)
       (artifact? (vector name version))
       (when options
         (pom-plugin-options? options))))

(defn pom-plugins? [v]
  (and ((every-pred vector? not-empty) v)
       (every? pom-plugin? v)))


;;; Source control management

(defn scm? [m]
  (util/and>> m
    map?
    (util/opt-key? :name name?)
    (util/opt-key? :tag  util/non-blank-string?)
    (util/opt-key? :url  url?)
    (util/opt-key? :dir  util/non-blank-string?)))


;;; Classifiers

(defn classifiers? [m]
  (and (map? m)
       (every? keyword? (keys m))
       (every? (some-fn keyword? map?) (vals m))))



;;; Whole project map

(defn valid-map?
  "Validate that m is a valid Leiningen project map."
  [m]
  (util/and>> m
    map?
    (util/opt-key? :description               util/non-blank-string?)
    (util/opt-key? :url                       url?)
    (util/opt-key? :mailing-list              mailing-list?)
    (util/opt-key? :mailing-lists             mailing-lists?)
    (util/opt-key? :license                   license?)
    (util/opt-key? :licenses                  licenses?)
    (util/opt-key? :min-lein-version          semantic-version-string?)
    (util/opt-key? :dependencies              dependencies?)
    (util/opt-key? :managed-dependencies      dependencies?)
    (util/opt-key? :pedantic?                 pedantic?)
    (util/opt-key? :exclusions                exclusions?)
    (util/opt-key? :plugins                   plugins?)
    (util/opt-key? :repositories              repositories?)
    (util/opt-key? :plugin-repositories       repositories?)
    (util/opt-key? :mirrors                   mirrors?)
    (util/opt-key? :local-repo                util/non-blank-string?)
    (util/opt-key? :update                    update-enum?)
    (util/opt-key? :checksum                  checksum?)
    (util/opt-key? :offline?                  util/boolean?)
    (util/opt-key? :deploy-repositories       repositories?)
    (util/opt-key? :signing                   signing?)
    (util/opt-key? :certificates              certificates?)
    (util/opt-key? :profiles                  profiles?)
    (util/opt-key? :hooks                     hooks?)
    (util/opt-key? :middleware                hooks?)
    (util/opt-key? :implicit-middleware       util/boolean?)
    (util/opt-key? :implicit-hooks            util/boolean?)
    (util/opt-key? :main                      symbol?)
    (util/opt-key? :aliases                   aliases?)
    (util/opt-key? :release-tasks             release-tasks?)
    (util/opt-key? :prep-tasks                release-tasks?)
    (util/opt-key? :aot                       aot?)
    (util/opt-key? :injections                injections?)
    (util/opt-key? :java-agents               java-agents?)
    (util/opt-key? :javac-options             javac-options?)
    (util/opt-key? :warn-on-reflection        util/boolean?)
    (util/opt-key? :global-vars               global-vars?)
    (util/opt-key? :java-cmd                  util/non-blank-string?)
    (util/opt-key? :jvm-opts                  jvm-opts?)
    (util/opt-key? :eval-in                   eval-in?)
    (util/opt-key? :bootclasspath             util/boolean?)
    (util/opt-key? :source-paths              paths?)
    (util/opt-key? :java-source-paths         paths?)
    (util/opt-key? :test-paths                paths?)
    (util/opt-key? :resource-paths            paths?)
    (util/opt-key? :target-path               path?)
    (util/opt-key? :compile-path              path?)
    (util/opt-key? :native-path               path?)
    (util/opt-key? :clean-targets             clean-targets?)
    (util/opt-key? :clean-non-project-classes util/boolean?)
    ;; NOTE: Equally imprecise as the schema impl. Goes for all mentions.
    (util/opt-key? :checkout-deps-shares      ifn?)
    (util/opt-key? :test-selectors            test-selectors?)
    (util/opt-key? :monkeypatch-clojure-test  util/boolean?)
    (util/opt-key? :repl-options              repl-options?)
    (util/opt-key? :jar-name                  util/non-blank-string?)
    (util/opt-key? :uberjar-name              util/non-blank-string?)
    (util/opt-key? :omit-source               util/boolean?)
    (util/opt-key? :jar-exclusions            non-empty-vec-of-regexes?)
    (util/opt-key? :jar-inclusions            non-empty-vec-of-regexes?)
    (util/opt-key? :uberjar-exclusions        non-empty-vec-of-regexes?)
    (util/opt-key? :auto-clean                util/boolean?)
    (util/opt-key? :uberjar-merge-with        uberjar-merge-with?)
    (util/opt-key? :filespecs                 filespecs?)
    (util/opt-key? :manifest                  manifest?)
    (util/opt-key? :pom-location              util/non-blank-string?)
    (util/opt-key? :parent                    parent?)
    (util/opt-key? :extensions                extensions?)
    (util/opt-key? :pom-plugins               pom-plugins?)
    (util/opt-key? :pom-addition              xml-vector?)
    (util/opt-key? :scm                       scm?)
    (util/opt-key? :install-releases?         util/boolean?)
    (util/opt-key? :deploy-branches           deploy-branches?)
    (util/opt-key? :classifiers               classifiers?)
    ))


;; The below declarations exist to have a 1-1
;; symbol-in-project-map to keyword relation in this ns. They don't
;; have to exist in order for the validation to work.


(def description               util/non-blank-string?)
(def url                       url?)
(def min-lein-version          semantic-version-string?)
(def managed-dependencies      dependencies?)
(def pedantic?                 pedantic?)
(def plugin-repositories       repositories?)
(def local-repo                util/non-blank-string?)
(def update                    update-enum?)
(def offline?                  util/boolean?)
(def deploy-repositories       repositories?)
(def middleware                hooks?)
(def implicit-middleware       util/boolean?)
(def implicit-hooks            util/boolean?)
(def main                      symbol?)
(def prep-tasks                release-tasks?)
(def warn-on-reflection        util/boolean?)
(def java-cmd                  util/non-blank-string?)
(def bootclasspath             util/boolean?)
(def source-paths              paths?)
(def java-source-paths         paths?)
(def test-paths                paths?)
(def resource-paths            paths?)
(def target-path               path?)
(def compile-path              path?)
(def native-path               path?)
(def clean-non-project-classes util/boolean?)
(def checkout-deps-shares      ifn?)
(def monkeypatch-clojure-test  util/boolean?)
(def jar-name                  util/non-blank-string?)
(def uberjar-name              util/non-blank-string?)
(def omit-source               util/boolean?)
(def jar-exclusions            non-empty-vec-of-regexes?)
(def jar-inclusions            non-empty-vec-of-regexes?)
(def uberjar-exclusions        non-empty-vec-of-regexes?)
(def auto-clean                util/boolean?)
(def pom-location              util/non-blank-string?)
(def pom-addition              xml-vector?)
(def install-releases?         util/boolean?)
