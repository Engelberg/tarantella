(ns tarantella.core
  (:import dancinglinks.DancingLink)
  (:use clojure.set)
  (:require [clojure.spec :as s]))

(defn- row-map->col-map [m]
  (apply merge-with into (for [[k v] m, i v] {i [k]})))

(defn- matrix->maps [m]
  (let [n-rows (count m),
        n-cols (count (m 0))
        row-map
        (into {} (for [row (range n-rows)]
                   [row (for [col (range n-cols)
                              :when (= 1 ((m row) col))]
                          col)])),
        col-map
        (into {} (for [col (range n-cols)]
                   [col (for [row (range n-rows)
                              :when (= 1 ((m row) col))]
                          row)]))]
    [row-map col-map]))

(defn- map->maps [m]
  [m (row-map->col-map m)])

(defn- seq->maps [s]
  (map->maps (into {} (map-indexed vector s))))

(defn- make-tapestry [[row-map col-map] options]
  (let [columns-intersected-by-selected-rows (mapcat row-map (:select-rows options)),
        set-columns (set columns-intersected-by-selected-rows)]
    (if (= (count columns-intersected-by-selected-rows)
           (count set-columns))
      (DancingLink/makeTapestry row-map col-map
                                (set (:optional-columns options))
                                (into set-columns
                                      (:ignore-columns options)))
      (throw (ex-info "You preselected rows for the solution with duplicate column entries"
                      {:select-rows (:select-rows options)})))))

;; Handy spec utilities
(defn assert-valid [spec data]
  (if (s/valid? spec data)
    true
    (throw (AssertionError. (s/explain-str spec data)
                            (ex-info "Spec Failure" (s/explain-data spec data))))))

(defn assert-conform [spec data]
  (let [conform (s/conform spec data)]
    (if (= conform ::s/invalid)
      (throw (AssertionError. (s/explain-str spec data)
                              (ex-info "Spec Failure" (s/explain-data spec data))))
      conform)))

(s/def ::same-size-rows (fn [matrix] (= 1 (count (into #{} (map count) matrix)))))
(s/def ::matrix-row (s/and (s/coll-of (s/int-in 0 2) []) vector?))
(s/def ::matrix (s/and (s/coll-of ::matrix-row [])
                       vector?
                       ::same-size-rows))

(s/def ::row-label ::s/any)
(defn all-different? [coll] (or (set? coll) (apply distinct? coll)))
(s/def ::row-labels (s/and coll? all-different?))
(s/def ::column-labels (s/and coll? all-different?))
(s/def ::row-map (s/map-of ::row-label ::column-labels))

(s/def ::row-seq (s/and (s/coll-of ::column-labels []) sequential?))

(s/def ::dancing-links-input (s/or ::matrix ::matrix ::row-map ::row-map ::row-seq ::row-seq))

(s/def ::optional-columns ::column-labels)
(s/def ::select-rows ::row-labels)
(s/def ::ignore-columns ::column-labels)
(s/def ::limit nat-int?)
(s/def ::timeout pos-int?)
(s/def ::dancing-links-options (s/keys* :opt-un [::optional-columns ::select-rows ::ignore-columns
                                                 ::limit ::timeout]))

(defn dancing-links
  "Can take input in one of three formats:
   - A matrix (vector of equal-length vectors) of 1s and 0s
   - A map of the form {row-label set-of-column-labels, ...}
   - A sequential collection of the form [set-of-column-labels-for-row-0 ...]

Returns a vector of all the solutions (each solution is a vector of row labels,
where the rows are implicitly labeled 0,1,... if no labels are specified).

Optional keywords:
   :optional-columns - A set of column labels where *at most one* 1 can be in that column
                       (as opposed to *exactly one* 1 like the standard columns)
   :ignore-columns   - A set of column labels you want to ignore
   :select-rows      - A set of rows that must be selected for the solution

   :limit            - An integer, stop early as soon as you find this many solutions
   :timeout          - A number of milliseconds, stop early when this time has elapsed                       "
  [m & {:as options}]
  (assert (every? #{:optional-columns :ignore-columns :select-rows :limit :timeout} (keys options))
          "Invalid optional keyword")
  (let [[input-type _] (assert-conform ::dancing-links-input m),         
        ^DancingLink tapestry (make-tapestry
                                ((case input-type
                                   ::matrix matrix->maps
                                   ::row-map map->maps
                                   ::row-seq  seq->maps)
                                  m)
                                options),
        solutions
        (cond
          (:timeout options) (.initSearchLimitTimeout tapestry (:limit options 0) (:timeout options))
          (:limit options) (.initSearchLimit tapestry (:limit options))
          :else (.initSearch tapestry)), 
        selected-rows (:select-rows options)]
    (into [] (comp (map #(concat selected-rows %)) (map vec)) solutions)))

(s/fdef dancing-links
        :args (s/cat :m ::dancing-links-input
                     :options ::dancing-links-options)
        :ret ::row-labels)

