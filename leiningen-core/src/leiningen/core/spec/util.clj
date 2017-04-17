(ns leiningen.core.spec.util
  (:require
   [clojure.spec           :as spec]
   [clojure.spec.gen       :as gen]
   [clojure.string         :as str]
   [miner.strgen           :as strgen]))

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

(defn key-xor?
  "Returs true if coll exclusively contains one of two keys."
  [coll a-key b-key]
  (let [a (contains? coll a-key)
        b (contains? coll b-key)]
    (or (and a (not b))
        (and b (not a)))))

(spec/def ::non-blank-string
  (spec/and string? #(not (str/blank? %))))

(spec/def ::namespaced-string
  (stregex #"[^\s/]+/[^\s/]+"))

(spec/def ::natural-number
  (spec/int-in 0 Integer/MAX_VALUE))

;; Matches regular expressions e.g. #\"\", not strings.
(spec/def ::stregex
  (spec/with-gen
    #(instance? java.util.regex.Pattern %)
    #(gen/fmap re-pattern (spec/gen string?))))
