(ns leiningen.core.pred-validation.util
  (:require [clojure.string :as str]))



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

(defn key-val-seq?
  ([kv-seq]
   (and (even? (count kv-seq))
        (every? keyword? (take-nth 2 kv-seq))))
  ([kv-seq validation-map]
   (and (key-val-seq? kv-seq)
        (every? identity
                (for [[k v] (partition 2 kv-seq)]
                  (if-let [pred (get validation-map k)]
                    (pred v)
                    true)))))) ; Change to false for closed map type.

(defn opt-key?
  [key predicate data]
  (if (contains? data key)
    (predicate (get data key))
    true))

;; Only exists for api-likeness to opt-key.
(defn req-key?
  [key predicate data]
  (predicate (get data key)))


;;; Macros

(defmacro stregex-matches
  "Constructs a form that returns the string if it matches, else a
  falsey value."
  [string-regex string]
  `(and
    (string? ~string)
    (re-matches ~string-regex ~string)
    ~string))

(defmacro and>>
  "Returns a form where x is inserted at the end of every given form
  and the whole sequence of forms are surrounded by an and-statement."
  [x & forms]
  `(and ~@(map (fn [f]
              (if (seq? f)
                `(~(first f) ~@(next f) ~x)
                `(~f ~x)))
            forms)))
