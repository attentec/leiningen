(ns leiningen.core.schema.util
  (:require [clojure.string               :as str]
            [miner.strgen                 :as strgen]
            [schema.core                  :as schema]
            [schema-generators.generators :as gen]))

;;; State

(defonce generators (atom {}))



;;; Functions


(defn stregex! [string-regex]
  "Defines a schema which matches a string based on a given string
  regular expression. This the classical type of regex as in the
  clojure regex literal #\"\".

  Also registers a generator for the string-regex with
  util/generators."
  (swap! generators assoc string-regex (strgen/string-generator string-regex))
  string-regex)


;;; Data schemas


(schema/defschema non-blank-string
  (schema/constrained schema/Str (complement str/blank?)))
