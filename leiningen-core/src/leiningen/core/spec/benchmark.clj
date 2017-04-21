(ns leiningen.core.spec.benchmark
  (:require [clojure.spec                :as spec]
            [clojure.spec.gen            :as gen]
            [leiningen.core.project      :as proj]))

(defmacro time'
  "Evaluates expr and prints the time it took.  Returns the value of
 expr."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (/ (double (- (. System (nanoTime)) start#)) 1000000.0)))

(defn mean [coll]
  (let [sum (apply + coll)
        count (count coll)]
    (if (pos? count)
      (/ sum count)
            0)))

(defn median [coll]
  (let [sorted (sort coll)
        cnt (count sorted)
        halfway (quot cnt 2)]
    (if (odd? cnt)
      (nth sorted halfway)
      (let [bottom (dec halfway)
            bottom-val (nth sorted bottom)
            top-val (nth sorted halfway)]
                (mean [bottom-val top-val])))))

(defn benchmark-generation [keys samples]
  "Benchmark generation from the given keys over the given number of samples."
  (for [key keys]
    (let [times (for [i (range samples)]
                  (time' (gen/generate (spec/gen key))))
          sum   (apply + times)]
      (println "Benchmarked" key "for" sum "ms")
      (println (format "%s %25s %25s %25s\n%f %25f %25f %25f"
                       "min" "max" "mean" "median"
                       (apply min times) (apply max times) (mean times) (median times)))
      (println " "))))
