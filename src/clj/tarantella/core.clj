(ns tarantella.core
  (:import io.github.engelberg.dancinglinks.DancingLink)
  (:refer-clojure :exclude [cond])
  (:require [clojure.spec.alpha :as s]
            [better-cond.core :refer [cond defnc defnc-]]))

;; Functions to convert high-level input into format required by Java implementation

(defn- row-map->col-map [m]
  (apply merge-with into (for [[k v] m, i v] {i [k]})))

(defn- matrix->row-col-maps [m]
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

(defn- row-map->row-col-maps [m]
  [m (row-map->col-map m)])

(defn- row-seq->row-col-maps [s]
  (row-map->row-col-maps (into {} (map-indexed vector s))))

(defn- make-tapestry [[row-map col-map] options]
  (let [columns-intersected-by-selected-rows (mapcat row-map (:select-rows options)),
        set-columns (set columns-intersected-by-selected-rows)]
    (if (= (count columns-intersected-by-selected-rows)
           (count set-columns))
      (DancingLink/makeTapestry row-map col-map
                                (set (:optional-columns options))
                                (into set-columns
                                      (:forbid-columns options))
                                (boolean (:shuffle options)))
      (throw (ex-info "You selected rows for the solution with duplicate column entries"
                      {:select-rows (:select-rows options)})))))

;; Specs
;;
;; Most of these specs don't currently generate, but when instrumentation is
;; enabled for dancing-links, these specs allow for comprehensive input checking
;; and improved error messages for erroneous inputs.
;; It is recommended that you turn on instrumentation if you are new to using this library.

(defn- same-size-rows? [matrix] (= 1 (count (into #{} (map count) matrix))))
(s/def ::matrix (s/and (s/coll-of (s/coll-of (s/int-in 0 2) :kind vector?) 
                                  :kind vector?)
                       same-size-rows?))

(defn- all-different? [x] (or (set? x) (empty? x) (apply distinct? x)))

(s/def ::row-label any?)
(s/def ::row-labels all-different?)
(s/def ::column-label any?)
(s/def ::column-labels all-different?)

(s/def ::row-map (s/map-of ::row-label ::column-labels))
(s/def ::row-seq (s/coll-of ::column-labels :kind sequential? :into []))

(defn- row-type [row]
  (if (set? row) ::row-seq
      (loop [row (seq row), seen (transient #{})]
        (cond
          (not row) nil ; unsure of type
          :let [item (first row)]
          (not (number? item)) ::row-seq
          (and (not (== item 0)) (not (== item 1))) ::row-seq
          (seen item) ::matrix
          :let [seen (conj! seen item)]
          (recur (next row) seen)))))

(defn- dancing-links-input-type [m]
  (cond
    (map? m) ::row-map
    :let [input-type (first (keep row-type m))]
    input-type input-type
    ;; Warning: if we reach this point, the input-type is ambiguous which means
    ;; it is a vector of vectors with two or fewer 0's and 1's, no duplicates in a row,
    ;; for example, [[0 1] [1 0] [0 1] [0 1]].
    ;; This is a silly thing to use dancing-links on in either interpretation, 
    ;; but in this ambiguous case, let's go ahead and interpret it as a matrix.
    ;; Alternatively, we could:
                                        ;    (throw (ex-info "Cannot determine dancing-links input type"
                                        ;                    {:input m}))))
    ::matrix))

(defmulti dancing-links-input-spec
  "dancing-links supports three input formats. See specs for:
   :tarantella.core/matrix
   :tarantella.core/row-map
   :tarantella.core/row-seq"
  dancing-links-input-type)
(defmethod dancing-links-input-spec ::matrix [_] ::matrix)
(defmethod dancing-links-input-spec ::row-map [_] ::row-map)
(defmethod dancing-links-input-spec ::row-seq [_] ::row-seq)

(s/def ::dancing-links-input (s/multi-spec dancing-links-input-spec ::input))

(s/def ::optional-columns ::column-labels)
(s/def ::select-rows ::row-labels)
(s/def ::forbid-columns ::column-labels)
(s/def ::shuffle boolean?)
(s/def ::limit pos-int?)
(s/def ::timeout pos-int?)
(s/def ::dancing-links-options
  (s/& (s/keys* :opt-un [::optional-columns ::select-rows ::forbid-columns ::shuffle ::limit ::timeout])
       (fn [m] (every? #{:optional-columns :select-rows :forbid-columns :shuffle :limit :timeout} (keys m)))))

(s/fdef dancing-links
  :args (s/cat :m ::dancing-links-input
               :options ::dancing-links-options)
  :ret (s/coll-of ::row-labels))


;; The API implementation

(defn- row-col-maps [m]
  (let [input-type (dancing-links-input-type m)]
    ((case input-type
       ::matrix matrix->row-col-maps
       ::row-map row-map->row-col-maps
       ::row-seq  row-seq->row-col-maps)
     m)))

(defn- lazy-search [^DancingLink tapestry selected-rows]
  (let [lazy-search-step
        (fn lazy-search-step []
          (lazy-seq
           (when-let [sol (.searchOne tapestry)]
             (cons (with-meta (vec (concat selected-rows sol))
                     {:steps (.-steps tapestry),
                      :backtrack-steps (.-backtrackSteps tapestry),
                      :nodes (.-nodes tapestry)
                      :decision-nodes (.-decisionNodes tapestry),
                      :dead-ends (.-deadends tapestry)})
                   (lazy-search-step)))))]
    (lazy-search-step)))

(defnc dancing-links
  "Can take input in one of three formats:
   - A matrix (vector of equal-length vectors) of 1s and 0s
   - A map of the form {row-label set-of-column-labels, ...}
   - A sequential collection of the form [set-of-column-labels-for-row-0 ...]

  Returns a vector of all the solutions (each solution is a vector of row labels,
  where the rows are implicitly labeled 0,1,... if no labels are specified).

  Optional keywords:
   :optional-columns - A set of column labels where *at most one* 1 can be in that column
                       (as opposed to *exactly one* 1 like the standard columns)
   :forbid-columns   - A set of column labels where *no* 1 can be in that column
                       (as opposed to *exactly one* 1 like the standard columns)
   :select-rows      - A set of row labels that must be selected for the solution
   :shuffle          - Boolean. Randomize search among equally promising alternatives

   :lazy             - Boolean. Return a lazy sequence

   :limit            - A positive integer, stop early as soon as you find this many solutions
   :timeout          - A number of milliseconds, stop early when this time has elapsed

  :limit and :timeout are ignored if you have selected `:lazy true`.
  If :limit or :timeout is specified, metadata will be attached to the return vector of the form
  {:search-ended-due-to-limit true/false, :search-ended-due-to-timeout true/false}"
  [m & {:keys [limit timeout lazy select-rows] :as options}]
  :let [^DancingLink tapestry (make-tapestry
                               (row-col-maps m)
                               options)]
  lazy (do (.initSearchOne tapestry) (lazy-search tapestry select-rows)),
  :let [solutions
        (cond
          timeout (.initSearchLimitTimeout tapestry (:limit options 0) timeout)
          limit (.initSearchLimit tapestry limit)
          :else (.initSearch tapestry)), 
        vec-solutions (into [] (comp (map #(concat select-rows %)) (map vec)) solutions)]
  (or limit timeout)
  (with-meta vec-solutions {:search-ended-due-to-limit (.limitFlag tapestry),
                            :search-ended-due-to-timeout (.timeoutFlag tapestry)
                            :nodes (.-nodes tapestry),
                            :decision-nodes (.-decisionNodes tapestry)
                            :dead-ends (.-deadends tapestry)}),
  :else (with-meta vec-solutions {:nodes (.-nodes tapestry),
                                  :decision-nodes (.-decisionNodes tapestry)
                                  :dead-ends (.-deadends tapestry)}))
