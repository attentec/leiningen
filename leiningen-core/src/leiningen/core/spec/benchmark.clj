(ns leiningen.core.spec.benchmark
  (:require [clojure.spec.alpha          :as spec]
            [clojure.spec.gen.alpha      :as gen]
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
      (let [bottom     (dec halfway)
            bottom-val (nth sorted bottom)
            top-val    (nth sorted halfway)]
        (mean [bottom-val top-val])))))


(def square #(* % %))

(defn std-dev
  [a]
  (let [mn (mean a)]
    (Math/sqrt
     (/ (reduce #(+ %1 (square (- %2 mn))) 0 a)
        (dec (count a))))))


(defn benchmark-fn
  "Benchmark the given function(s) a given number of times."
  [samples & fns]
  (for [fn fns]
    (let [times (for [i (range samples)]
                  (time' (fn)))
          sum   (apply + times)]
      (println "Benchmarked" fn "for" sum "ms")
      (println (format "%s %25s %25s %25s %25s\n%f %25f %25f %25f %25f"
                       "min" "max" "mean" "median" "std-dev"
                       (apply min times) (apply max times) (mean times) (median times) (std-dev times)))
      (println " "))))

(defn benchmark-generation
  "Benchmark generation from the given keys over the given number of samples."
  [keys samples]
  (println (format "%15s %15s %15s %15s %15s %15s"
                   "total" "min" "max" "mean" "median" "std-dev"))
  (for [key keys]
    (let [times (for [i (range samples)]
                  (time' (gen/generate (spec/gen key))))
          sum   (apply + times)]
      (println (format "%15.5f %15.5f %15.5f %15.5f %15.5f %15.5f"
                       sum (apply min times) (apply max times) (mean times) (median times) (std-dev times)) key))))
