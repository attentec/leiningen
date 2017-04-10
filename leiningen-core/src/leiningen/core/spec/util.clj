(ns leiningen.core.spec.util
  (:require
   [clojure.spec           :as spec]
   [clojure.spec.gen       :as gen]))

(defmacro vcat
  "Takes key+pred pairs, e.g.

  (vcat :e even? :o odd?)

  Returns a regex op that matches vectors, returning a map containing
  the keys of each pred and the corresponding value. The attached
  generator produces vectors."
  [& key-pred-forms]
  `(spec/with-gen (spec/and vector? (spec/cat ~@key-pred-forms))
     #(gen/fmap vec (spec/gen (spec/cat ~@key-pred-forms)))))
