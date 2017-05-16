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


;;; Macros

(defmacro opt-key
  [key predicate data]
  `(when (contains? ~data ~key)
     (truss/have ~predicate (get ~data ~key)))
  data)

(defmacro req-key
  [key predicate data]
  `(truss/have ~predicate (get ~data ~key))
  data)

(defmacro stregex-matches?
  [string-regex string]
  `(and
    (truss/have? string? ~string)
    (re-matches ~string-regex ~string)))

(defmacro multi-pred?
  [data & predicates]
  (conj
   (for [pred predicates]
     `(truss/have? ~pred ~data))
   'and))


(defmacro doto>
  "Evaluates x then calls all of the methods and functions with the
  value of x supplied at the back of the given arguments.  The forms
  are evaluated in order.  Returns x."
  [x & forms]
    (let [gx (gensym)]
      `(let [~gx ~x]
         ~@(map (fn [f]
                  (if (seq? f)
                    `(~(first f) ~@(next f) ~gx)
                    `(~f ~gx)))
                forms)
         ~gx)))
