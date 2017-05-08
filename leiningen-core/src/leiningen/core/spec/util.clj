(ns leiningen.core.spec.util
  (:require
   [clojure.spec           :as spec]
   [clojure.spec.gen       :as gen]
   [clojure.string         :as str]
   [miner.strgen           :as strgen]))


;;; Macros

(defmacro vcat
  "Takes key+pred pairs, e.g.

  (vcat :e even? :o odd?)

  Returns a regex op that matches vectors, returning a map containing
  the keys of each pred and the corresponding value. The attached
  generator produces vectors."
  [& key-pred-forms]
  `(spec/with-gen
     (spec/and  vector?       (spec/cat ~@key-pred-forms))
     #(gen/fmap vec (spec/gen (spec/cat ~@key-pred-forms)))))


(defmacro stregex
  "Defines a spec which matches a string based on a given string
  regular expression. This the classical type of regex as in the
  clojure regex literal #\"\""
  [string-regex]
  `(spec/with-gen
     (spec/and string? #(re-matches ~string-regex %))
     #(strgen/string-generator ~string-regex)))


;;; Functions

(defn key-xor?
  "Returs true if coll exclusively contains one of two keys."
  [coll a-key b-key]
  (let [a (contains? coll a-key)
        b (contains? coll b-key)]
    (or (and a (not b))
        (and b (not a)))))


;;; Data specs

(spec/def ::non-blank-string
  (spec/and string? (complement str/blank?)))

(spec/def ::namespaced-string
  (stregex #"[^\s/]+/[^\s/]+"))

(spec/def ::positive-integer
  (spec/and integer? pos?))

;; Matches regular expressions, e.g. #"", not strings.
(spec/def ::stregex
  (spec/with-gen
    #(instance? java.util.regex.Pattern %)
    #(gen/fmap re-pattern (spec/gen string?))))

;; Generates from namespaces in the running jvm.
(spec/def ::namespace
  (spec/with-gen
    #(instance? clojure.lang.Namespace %)
    #(gen/elements (all-ns))))

;; Only matches namespaces in the currently running jvm.
(spec/def ::namespace-symbol
  (spec/with-gen
    simple-symbol?
    #(gen/fmap ns-name (gen/elements (all-ns)))))

(spec/def ::exception
  (spec/with-gen
    #(instance? java.lang.Exception %)
    #(gen/fmap (fn [str] (java.lang.Exception. str)) (spec/gen string?))))


;;; Function specs

(spec/fdef ::predicate
           :args (spec/cat :arg any?)
           :ret  boolean?)

(spec/fdef ::nullary-fn
           :args (spec/cat)
           :ret  any?)

(spec/fdef ::unary-fn
           :args (spec/cat :first any?)
           :ret  any?)

(spec/fdef ::binary-fn
           :args (spec/cat :first any? :second any?)
           :ret  any?)
