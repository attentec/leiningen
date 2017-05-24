(ns leiningen.core.schema.util
  (:require [clojure.string                :as str]
            [miner.strgen                  :as strgen]
            [schema.core                   :as schema]
            [schema-generators.generators  :as gen]
            [clojure.test.check.generators :as tc-gen]))

;;; State

(defonce generators (atom {}))



;;; Functions


(defn stregex!
  "Defines a schema which matches a string based on a given string
  regular expression. This the classical type of regex as in the
  clojure regex literal #\"\".

  Also registers a generator for the string-regex with
  util/generators."
  [string-regex]
  (swap! generators assoc string-regex (strgen/string-generator string-regex))
  string-regex)

;;; Predicates

(defn simple-symbol?
  "Return true if x is a symbol without a namespace."
  [x] (and (symbol? x) (nil? (namespace x))))

(defn qualified-symbol?
  "Return true if x is a symbol with a namespace."
  [x] (and (symbol? x) (namespace x) true))

(defn boolean?
  "Return true if x is a Boolean."
  [x] (instance? Boolean x))

(defn key-val-seq?
  ([kv-seq]
   (and (even? (count kv-seq))
        (every? keyword? (take-nth 2 kv-seq))))
  ([kv-seq validation-map]
   (and (key-val-seq? kv-seq)
        (every? nil?
                (for [[k v] (partition 2 kv-seq)]
                  (if-let [schema (get validation-map k)]
                    (schema/check schema v)
                    :schema/invalid))))))

(defn non-blank-string? [string]
  (not (str/blank? string)))

(defn non-empty-vec? [v]
  (and (vector? v) (not-empty v)))


;;; Regex replacements

(defn cat-fn
  "Concatinate schemas together to form one continous sequence."
  [schemas]
  (fn [data] (every? nil? (map schema/check schemas data))))

(defn first-rest-cat-fn
  "Returns a function for accepting a concatination of one element of
  first-schema and something fulfilling rest-schema for the rest of
  the sequence."
  [first-schema rest-schema]
  (fn [data]
    (and (nil? (schema/check first-schema (first data)))
         (nil? (schema/check rest-schema  (rest  data))))))

(defn pair-rest-cat-fn
  "Returns a function for matching the first two elements in a
  sequence according to pair-schema and the rest to rest-chema."
  [pair-schema rest-schema]
  (fn [data]
    (and (nil? (schema/check pair-schema [(first data) (second data)]))
         (nil? (schema/check rest-schema  (next (rest  data)))))))


;;; Data schemas

(schema/defschema non-blank-string
  (schema/constrained schema/Str non-blank-string?))

(schema/defschema qualified-symbol
  (let [schema (schema/pred qualified-symbol?)]
    (swap! generators assoc schema tc-gen/symbol-ns)
    schema))

(schema/defschema simple-symbol
  (schema/pred simple-symbol?))

(schema/defschema positive-integer
  (schema/constrained schema/Int pos?))

(schema/defschema natural-number
  (schema/constrained schema/Int #(not (neg? %))))
