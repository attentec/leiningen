(ns leiningen.core.truss.util
  (:require [taoensso.truss :as truss]))



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
  [string] (not (str/blank? string)))
