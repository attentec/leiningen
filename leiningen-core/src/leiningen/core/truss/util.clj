(ns leiningen.core.truss.util
  (:require [taoensso.truss :as truss]
            [clojure.string :as str]))



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

(defn non-blank-string?
  [string]
  (and (string? string)
       (not (str/blank? string))))

(defn stregex?
  [x] (instance? java.util.regex.Pattern x))


;;; Functions

;; TODO: Perhaps convert to macro and put truss in it.
(defn key-val-seq?
  ([kv-seq]
   (and (even? (count kv-seq))
        (every? keyword? (take-nth 2 kv-seq))))
  ([kv-seq validation-map]
   (and (key-val-seq? kv-seq)
        (every? identity
                (for [[k v] (partition 2 kv-seq)]
                  (when-let [pred (get validation-map k)]
                    (pred v)))))))


;;; Macros

(defmacro opt-key
  [key predicate data]
  `(do (when (contains? ~data ~key)
         (truss/have ~predicate (get ~data ~key)))
       ~data))

;; Only exists for api-likeness to opt-key.
(defmacro req-key
  [key predicate data]
  `(do (truss/have [:ks>= #{~key}] ~data)
       (truss/have ~predicate (get ~data ~key))
       ~data))

(defmacro stregex-matches
  "Constructs a form that returns the string if it matches, else a
  falsey value."
  [string-regex string]
  `(and
    (string? ~string)
    (re-matches ~string-regex ~string)
    ~string))

(defmacro >>
  "Calls all of the functions with x supplied at the back of the given
  arguments. The forms are evaluated in order. Returns x."
  [x & forms]
  `(do
     ~@(map (fn [f]
              (if (seq? f)
                `(~(first f) ~@(next f) ~x)
                `(~f ~x)))
            forms)
     ~x))
