(ns tarantella.core-test
  (:use clojure.test)
  (:require [tarantella.core :as t]))

;; I use this library in my own work, but cannot open-source my tests.
;; I would welcome pull requests with tests and examples.

;; I would also welcome a test suite making use of either test.check or clojure.spec 
;; In the meantime, I've provided some simple randomized testing below.

(defn covers? 
  "Tests that a covering produced by dancing-links in fact covers a given input"
  [dancing-links-input covering]
  (let [[row-map col-map] (#'t/row-col-maps dancing-links-input),
        selected-rows (map row-map covering),
        covered-columns (apply concat selected-rows)]
    (= (count (distinct covered-columns)) (count col-map))))
    
(defn valid-output?
  "Tests that all coverings produced by dancing-links cover the input"
  [dancing-links-input dancing-links-output]
  (every? (partial covers? dancing-links-input) 
          dancing-links-output))

(defn random-matrix [height width prob-1]
  (vec (for [i (range height)]
         (vec (for [j (range width)]
                (if (< (rand) prob-1) 1 0))))))

(deftest random-coverings
  (dotimes [i 100]
    (let [m (random-matrix 30 10 0.2)
          output (t/dancing-links m :limit 100 :timeout 3000)]
      (is (valid-output? m output)))))

(deftest random-coverings-shuffle
  (dotimes [i 100]
    (let [m (random-matrix 30 10 0.2)
          output (t/dancing-links m :limit 100 :timeout 3000 :shuffle true)]
      (is (valid-output? m output)))))

(deftest classic-test-case
  (is (= (map set [[0 3 4]])
         (map set (t/dancing-links 
                    [[0    0    1    0    1    1    0]
                     [1    0    0    1    0    0    1]
                     [0    1    1    0    0    1    0]
                     [1    0    0    1    0    0    0]
                     [0    1    0    0    0    0    1]
                     [0    0    0    1    1    0    1]])))))

(deftest example-with-names
  (is (= (set (map set [[:Alice :Charles] [:Bob :David]]))
         (set (map set 
                   (t/dancing-links
                     {:Alice [:chocolate :strawberry]
                      :Bob [:chocolate :vanilla]
                      :Charles [:vanilla]
                      :David [:strawberry]}))))))
