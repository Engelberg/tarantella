(ns tarantella.sudoku
  (:use clojure.test)
  (:require [tarantella.core :as t]))

;; The rules of suduko
(def sudoku-constraints
  (into {} (for [i (range 9), j (range 9), n (range 1 10)]
             [[[i j] n]
              #{[i j] [:row i n] [:col j n] [:sector (quot i 3) (quot j 3) n]}])))


(defn board->filled-cells [board]
  (for [i (range 9), j (range 9)
        :let [n (get-in board [i j])]
        :when (integer? n)]
    [[i j] n]))

(defn filled-cells->board [cells]
  (let [m (into {} cells)]
    (mapv vec (for [i (range 9)]
                (for [j (range 9)]
                  (get m [i j] '-))))))

(defn solve-sudoku [board]
  (->> (t/dancing-links sudoku-constraints
                        :select-rows (board->filled-cells board)
                        :limit 1)
       first
       filled-cells->board))

(def sample-puzzle
  '[[- - -   - - -   - - -]
    [- - -   - - 3   - 8 5]
    [- - 1   - 2 -   - - -]

    [- - -   5 - 7   - - -]
    [- - 4   - - -   1 - -]
    [- 9 -   - - -   - - -]

    [5 - -   - - -   - 7 3]
    [- - 2   - 1 -   - - -]
    [- - -   - 4 -   - - 9]])

(def sample-solution
  [[9 8 7   6 5 4   3 2 1]
   [2 4 6   1 7 3   9 8 5]
   [3 5 1   9 2 8   7 4 6]

   [1 2 8   5 3 7   6 9 4]
   [6 3 4   8 9 2   1 5 7]
   [7 9 5   4 6 1   8 3 2]

   [5 1 9   2 8 6   4 7 3]
   [4 7 2   3 1 9   5 6 8]
   [8 6 3   7 4 5   2 1 9]])

(def hardest-sudoku-puzzle
  '[[8 - -  - - -  - - -]
    [- - 3  6 - -  - - -]
    [- 7 -  - 9 -  2 - -]
    
    [- 5 -  - - 7  - - -]
    [- - -  - 4 5  7 - -]
    [- - -  1 - -  - 3 -]
    
    [- - 1  - - -  - 6 8]
    [- - 8  5 - -  - 1 -]
    [- 9 -  - - -  4 - -]])

(def hardest-sudoku-solution
  [[8 1 2  7 5 3  6 4 9]
   [9 4 3  6 8 2  1 7 5]
   [6 7 5  4 9 1  2 8 3]
   
   [1 5 4  2 3 7  8 9 6]
   [3 6 9  8 4 5  7 2 1]
   [2 8 7  1 6 9  5 3 4]
   
   [5 2 1  9 7 4  3 6 8]
   [4 3 8  5 2 6  9 1 7]
   [7 9 6  3 1 8  4 5 2]])

(deftest sudoku-test
  (is (= (solve-sudoku sample-puzzle) sample-solution))
  (is (= (solve-sudoku hardest-sudoku-puzzle) hardest-sudoku-solution)))

(defn make-sudoku []
  (let [rand-solution (shuffle (first (t/dancing-links sudoku-constraints
                                                       :shuffle true
                                                       :limit 1)))]
    (first (for [k (range 17 81),
                 :let [clues (take k rand-solution),
                       sols (t/dancing-links sudoku-constraints
                                             :select-rows clues
                                             :limit 2)]
                 :when (= (count sols) 1)]
             (filled-cells->board clues)))))

(defn measure-hardness [puzzle]
  (->> (t/dancing-links sudoku-constraints
                        :select-rows (board->filled-cells puzzle)
                        :lazy true)
       first meta))

(defn make-hard-sudokus []
  (filter (fn [puzzle] (pos? (:decision-nodes (measure-hardness puzzle))))
          (repeatedly make-sudoku)))

