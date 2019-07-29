# Tarantella

Tarantella is "a rapid whirling dance, so named because it was thought to be a cure for tarantism, a psychological illness characterized by an extreme impulse to dance, prevalent in southern Italy from the 15th to the 17th century, and widely believed at the time to have been caused by the bite of a tarantula. The victim danced the tarantella until exhausted."

Tarantella is also an implementation of Knuth's Dancing Links algorithm, a brute-force backtracking search for solving exact cover problems, made blisteringly fast through the use of a very clever data structure -- a network of circular, doubly-linked lists.  The name of the algorithm comes from the way the links disconnect and reconnect from one another as the algorithm runs.  In tarantella, the low-level implementation of the algorithm is written in Java for speed, with a high-level interface written in Clojure.

You can learn about how tarantella works by watching my talk [Solving Problems Declaratively](https://youtu.be/TA9DBG8x-ys).

## Usage

[tarantella "1.1.1"]

The API exposes a single function, `tarantella.core/dancing-links` which can take exact cover problems in three different input formats.  Here is the doc string:

```
Can take input in one of three formats:
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
  {:search-ended-due-to-limit true/false, :search-ended-due-to-timeout true/false}
```

You can instrument the function with clojure.spec for full input checking (although keep in mind that clojure.spec is still in alpha and subject to change).

## What is an exact cover problem?

The basic idea of an exact cover problem is that you have a matrix of 1s and 0s. You're trying to select a subset of the rows of the matrix such that each column contains exactly one 1. Each such subset is called a "covering".

A typical example is the following matrix -- let's give it the name `m`:

```clojure
(def m [[0    0    1    0    1    1    0]
        [1    0    0    1    0    0    1]
        [0    1    1    0    0    1    0]
        [1    0    0    1    0    0    0]
        [0    1    0    0    0    0    1]
        [0    0    0    1    1    0    1]]
```

We can run `dancing-links` on it to find a list of all possible coverings:

```clojure
=> (dancing-links m)
[[3 0 4]]
```

We see that there is exactly one covering, namely rows 0, 3, and 4. If you visually inspect those rows of matrix m, you'll see that indeed, among those rows there is exactly one 1 in every column.

```
Row 0: [0    0    1    0    1    1    0]
Row 3: [1    0    0    1    0    0    0]
Row 4: [0    1    0    0    0    0    1]
```

Note that the coverings returned by the dancing-links algorithm don't list the rows in ascending order; rather, they are listed in the order in which the algorithm discovered them.

## Sparse Matrix Format
It is not uncommon for the matrices we are operating on to be relatively sparse, so writing out the full matrix can be unwieldy. One of the matrix formats that tarantella supports is a sparse matrix representation where for each row we simply specify the columns that have 1s. You can represent the collection of columns with a vector, but you need to make sure you don't accidentally list the same column twice. Therefore, stylistically I prefer to represent the collection of columns with a set, which ensures no duplicates.

```clojure
(def m [ #{2 4 5}
         #{0 3 6}
         #{1 2 5}
         #{0 3}
         #{1 6}
         #{3 4 6} ])
```

This is the same matrix as before, represented differently.

```clojure
=> (dancing-links m)
[[3 0 4]]
```

The columns don't have to be numbers; you can use any Clojure data you like as the labels. So, for example, let's imagine we want to label our columns `:a` through `:g` rather than 0-6.

```clojure
(def m [ #{:c :e :f}
         #{:a :d :g}
         #{:b :c :f}
         #{:a :d}
         #{:b :g}
         #{:d :e :g} ])
```

This is equivalent to the above matrix, and will yield the exact same result when run through the dancing-links algorithm, although the different labels cause the algorithm to discover the rows in a different order.

```clojure
=> (dancing-links m)
[[0 4 3]]
```

## Labeled rows

In an exact cover problem, you can think of rows as the possible choices, and the columns as the constraints. Since the rows are the possible choices, often we want to give those choices meaningful names. We can do this by representing the matrix as a map from row labels to sets of column labels.

Let's say we want to label our sample matrix's rows as `:A` through `:F` instead of 0-5.

```clojure
(def m {:A #{:c :e :f},
        :B #{:a :d :g},
        :C #{:b :c :f},
        :D #{:a :d},
        :E #{:b :g},
        :F #{:d :e :g}})
```

Now, dancing-links will report the answer in terms of these row labels.

```clojure
=> (dancing-links m)
[[:A :E :D]]
```

This is probably the most versatile of the three dancing-links input formats.

## Optional keyword arguments

There are a few keyword arguments that can alter the search in useful ways. Here are usage examples:

* `:optional-columns #{0 4}` - This example would treat columns 0 and 4 as "at most one 1s" constraints. In other words, the final covering will have at most one 1 in column 0 and at most one 1 in column 4.

* `:forbid-columns #{0 4}` - This example would treat columns 0 and 4 as "at most zero 1s" constraints. In other words, dancing-links won't allow selecting any row that has a 1 in those columns. Another way of thinking about it is that it's like deleting those columns and any row which has a 1 in those columns.

* `:select-rows #{:A :D}` - This example would begin the search process by selecting rows `:A` and `:D`. So `:A` and `:D` will be in every single solution. (If you :select-rows with 1s in columns that have been forbidden by :forbid-columns, :select-rows will take precedence).

* `:shuffle true` - This flag causes the dancing-links algorithm to shuffle the matrix rows and columns before searching, which has the effect of causing the algorithm to choose randomly among equally promising alternatives at each stage of the search. Thus, it will discover solutions in a random order.

* `:limit 8` - This example would limit the output of dancing-links to the first 8 solutions. If you use this keyword, then the vector of solutions returned by dancing-links will have metadata on it `{:search-ended-due-to-limit true/false}` which will tell you whether the search was terminated by reaching the limit, or whether the search terminated after finding all the solutions.

* `:timeout 3000` - This example would terminate the dancing-links search after 3000 ms, at which point it will return a vector of all the solutions it has found so far. If you use this keyword, then the vector of solutions returned by dancing-links will have metadata on it `{:search-ended-due-to-timeout true/false}` which will tell you whether the search was terminated by reaching the timeout, or whether the search terminated after finding all the solutions. You can use the `:limit` and `:timeout` flags together, and whichever is reached first will cause the search to terminate.

* `:lazy true` - This flag causes the dancing-links algorithm to return a lazy sequence of solutions, generated one at a time as needed, rather than returning a fully realized vector of solutions. If you select this flag, :limit and :timeout are ignored. Those optional arguments aren't needed in lazy mode because you can control the generation of solutions by accessing only as much of the lazy sequence as you need. Laziness has its advantages, but do be aware that you'll be paying approximately a 50% performance penalty. One advantage of lazy mode is that it doesn't put pressure on Java's call stack like the regular depth-first recursive search. So, if you ever blow your call stack during `dancing-links`, that would be a good reason to switch to `:lazy true`.

## Metadata

The list of solutions returned by `dancing-links` has some attached metadata. As described above, if you've selected :limit or :timeout, there will be some metadata letting you know how whether those criteria caused the search to be terminated. Also, there will be metadata with some statistics about the search tree.

```clojure
=> (meta (dancing-links m))
{:nodes 6, :decision-nodes 1, :dead-ends 1}
```

This is telling us that the search tree contained 6 nodes. At 1 of those nodes, it faced a decision, but the rest of the nodes were forced choices. 1 of the nodes was a "leaf" node that was a dead-end, not a solution, i.e., a point in the search where it had a column still to satisfy but couldn't with the rows available.

The `:lazy true` option causes the metadata to be handled differently. It's not possible to put the metadata on the lazy sequence of the solutions because the search hasn't even begun at the time the sequence is created. Therefore, in lazy mode, the statistics metadata is attached to each solution in the lazy sequence, and represents the cumulative statistics up to the point where that solution was discovered.

```clojure
=> (meta (dancing-links m :lazy true))
nil
=> (meta (first (dancing-links m :lazy true)))
{:steps 4,
 :backtrack-steps 0,
 :nodes 4,
 :decision-nodes 1,
 :dead-ends 0}
```

There are a couple things to notice here. First, lazy mode is driven by a state machine version of the dancing links algorithm that can be stopped and restarted. So, in lazy mode, there are a couple extra stats associated with the state machine. `:steps` tells you how many steps the state machine took up to that solution, and `:backtrack-steps` tells you how many of those steps were backtracking/unwinding decisions that were made previously. The absolute numbers here aren't terribly critical, but may be useful to compare puzzles and solutions with one another.

The second thing to notice here is that the :nodes, :decision-nodes, and :dead-ends values are smaller than what we got in non-lazy mode. That's because this metadata is associated with the first solution (the only solution for this particular matrix!), so it is only giving us the nodes/decision-nodes/dead-ends found up to the point where this solution was discovered. The metadata we saw before was attached to the overall vector of solutions returned by `dancing-links` and reflected the nodes/decision-nodes/dead-ends for the *entire search process*, including the time spent after finding the first solution to exhaustively prove that there were no other solutions.

## Why should I care?

The thing that makes tarantella so powerful is that a wide assortment of puzzles can be expressed using this matrix modeling system (basically, any puzzle that can be expressed in terms of "exactly one" or "at most one" constraints). And if you have a puzzle that can be expressed in this way, the Dancing Links algorithm is a blazingly fast way to brute force search for solutions.

## Sudoku solver

Sudoku is probably the most famous "exactly one" constraint puzzle, so it's a great demonstration of Dancing Links.

The rules of Sudoku can be expressed as a matrix. Each row represents a possible choice of placing a given digit in a given cell. We'll use the row label `[[i j] n]` to mean we put the digit n in (row i,col j). Our row/col numbers i,j will range from 0-8 and our digits to place range from 1-9.

Each column represents a constraint. We'll use the column label `[i j]` for the constraint that there is exactly one digit allowed in (row i,col j). We'll use the column label `[:row i n]` for the constraint that row i must contain exactly one n. `[:col j n]` is the constraint that col j must contain exactly one n. `[:sector 0-2 0-2 n]` is the constraint that each of the 9 sectors must contain exactly one n.

The row-labeled sparse matrix representation lets us express these rules in just a few lines of code. We simply build a map that associates each cell-digit placement with the constraints it satisfies.

```clojure
;; The rules of sudoku
(def sudoku-constraints
  (into {} (for [i (range 9), j (range 9), n (range 1 10)]
             [[[i j] n]
              #{[i j] [:row i n] [:col j n] [:sector (quot i 3) (quot j 3) n]}])))
```

Amazing! This is all we need to use tarantella to solve sudoku puzzles, along with a list of initial cell/digit assignments (this list will be our `:select-rows` optional argument, which will cause dancing-links to kick off the search by selecting the rows of the sudoku-constraints matrix associated with those cell/digit assignments).

We want to be able to have a convenient way of entering our sudoku puzzles into Clojure, so we make a quick converter function to take a vector-of-vectors representation of a partially filled board and return the initial cell/digit assignments.

```clojure
(defn board->filled-cells [board]
  (for [i (range 9), j (range 9)
        :let [n (get-in board [i j])]
        :when (integer? n)]
    [[i j] n]))

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

=> (board->filled-cells sample-puzzle)
([[1 5] 3]
 [[1 7] 8]
 [[1 8] 5]
 [[2 2] 1]
 [[2 4] 2]
 [[3 3] 5]
 [[3 5] 7]
 [[4 2] 4]
 [[4 6] 1]
 [[5 1] 9]
 [[6 0] 5]
 [[6 7] 7]
 [[6 8] 3]
 [[7 2] 2]
 [[7 4] 1]
 [[8 4] 4]
 [[8 8] 9])
```

Our solver will return a solution as a list of these cell/digit assignments, so we'll also want a function that converts this back into a readable Sudoku grid.

```clojure
(defn filled-cells->board [cells]
  (let [m (into {} cells)]
    (mapv vec (for [i (range 9)]
                (for [j (range 9)]
                  (get m [i j] '-))))))

(defn solve-sudoku [board]
  (->> (dancing-links sudoku-constraints
                      :select-rows (board->filled-cells board)
                      :limit 1)
       first
       filled-cells->board))
       
=> (solve-sudoku sample-puzzle)
[[9 8 7 6 5 4 3 2 1]
 [2 4 6 1 7 3 9 8 5]
 [3 5 1 9 2 8 7 4 6]
 [1 2 8 5 3 7 6 9 4]
 [6 3 4 8 9 2 1 5 7]
 [7 9 5 4 6 1 8 3 2]
 [5 1 9 2 8 6 4 7 3]
 [4 7 2 3 1 9 5 6 8]
 [8 6 3 7 4 5 2 1 9]]
```

We were able to express the Sudoku constraint system in literally a few lines, and then were able to use the dancing-links algorithm as our solver, in conjunction with a couple short helper functions to convert in and out of a more readable format.

Best of all, this short implementation is one of the fastest Sudoku solvers around.

## Sudoku generator

Interestingly, our solver has some generative capabilities as well.  We can use the matrix in conjunction with the `:shuffle true` flag to generate a random legally-filled-in Sudoku grid.

```clojure
=> (filled-cells->board (first (dancing-links sudoku-constraints :shuffle true :limit 1)))
[[5 9 1 2 7 3 6 8 4]
 [8 6 7 1 4 5 2 3 9]
 [4 2 3 8 6 9 7 1 5]
 [3 7 9 6 1 8 5 4 2]
 [1 8 2 4 5 7 9 6 3]
 [6 4 5 9 3 2 8 7 1]
 [7 3 4 5 2 6 1 9 8]
 [2 1 8 7 9 4 3 5 6]
 [9 5 6 3 8 1 4 2 7]]
=> (filled-cells->board (first (dancing-links sudoku-constraints :shuffle true :limit 1)))
[[3 9 5 6 1 8 4 2 7]
 [7 2 8 3 4 5 6 1 9]
 [1 4 6 9 2 7 8 5 3]
 [9 8 4 5 3 2 1 7 6]
 [2 6 3 8 7 1 5 9 4]
 [5 1 7 4 6 9 2 3 8]
 [4 7 2 1 9 6 3 8 5]
 [6 5 9 2 8 3 7 4 1]
 [8 3 1 7 5 4 9 6 2]]
```

So, we can shuffle up the cell assignments, and take the initial k cell assignments and see whether it has a unique solution. We keep increasing k until we have a unique solution.

```clojure
(defn make-sudoku []
  (let [rand-solution (shuffle (first (t/dancing-links sudoku-constraints
                                                       :shuffle true
                                                       :limit 1)))]
    (first (for [k (range 17 81), ;; Unique sudokus have at least 17 starting assignments
                 :let [clues (take k rand-solution),
                       sols (t/dancing-links sudoku-constraints
                                             :select-rows clues
                                             :limit 2)]
                 :when (= (count sols) 1)]
             (filled-cells->board clues)))))

=> (make-sudoku)
[[3 - - 5 - 1 - 4 -]
 [- - - - 3 9 8 6 7]
 [8 - 4 6 - - 1 - -]
 [1 8 - - - - 5 - -]
 [6 - 5 8 9 - 4 - 1]
 [- - 2 - - 6 3 9 -]
 [4 - - - - - 9 8 -]
 [- - 3 - 1 8 - 5 4]
 [9 - - 3 7 4 6 - 2]]
```

We can use the metadata the solver provides to get an idea on how hard our puzzles are.

```clojure
(defn measure-hardness [puzzle]
  (meta (t/dancing-links sudoku-constraints
                        :select-rows (board->filled-cells puzzle))))

=> (measure-hardness sample-puzzle)
{:nodes 65, :decision-nodes 0, :dead-ends 0}
=> (measure-hardness hardest-sudoku-puzzle)
{:nodes 1302, :decision-nodes 92, :dead-ends 92}
```

Most randomly generated Sudoku puzzles have no decisions to be made, and no dead-ends; the solver can methodically find the solution applying one constraint at a time without backtracking. If we want to create hard sudokus, we'll need to find ones with decision nodes.

```clojure
(defn make-hard-sudokus []
  (filter (fn [puzzle] (pos? (:decision-nodes (measure-hardness puzzle))))
          (repeatedly make-sudoku)))

=> (def h (make-hard-sudokus))
=> (take 5 h)   ;; This will take a while
([[- - - - - - 8 - -]
  [- - - 2 3 - - 6 5]
  [- 6 - - - - - 2 -]
  [- - - - 8 4 - - -]
  [- 3 - - 5 - - 8 -]
  [- - - 7 9 - 1 3 -]
  [6 - - - - 8 9 - -]
  [- 7 3 - - 5 2 - -]
  [8 - 9 - - - - - 7]]
 [[- - 3 - - 5 - 8 -]
  [- - 2 1 3 4 9 - 5]
  [9 - - 8 6 - 1 - -]
  [- - 8 - - - - - -]
  [- - 6 5 8 - - 2 7]
  [- 9 - 2 - 6 - - 1]
  [8 3 - - - - 6 5 -]
  [2 - - 6 - - - - -]
  [- 6 - - 4 - - - 8]]
 [[6 - - - 3 - 4 1 -]
  [- 2 3 - 1 5 6 - 8]
  [- 1 8 4 - - - - 5]
  [- - - 9 - - - - -]
  [8 - - - 6 - 1 - -]
  [1 4 - - - - - 6 2]
  [2 - - 6 - - 9 - -]
  [- - - - - - - 5 6]
  [- 5 - - - - 8 - 1]]
 [[- 6 - 3 - 5 - - 7]
  [- 1 - 9 6 - 2 - -]
  [- - 3 - - 7 - 6 9]
  [6 - - 7 - 1 - 2 5]
  [- 2 5 - 9 - - - 6]
  [1 3 - 2 5 6 - - 4]
  [- 8 - - 1 - - - -]
  [9 7 - - - - - - -]
  [- 5 - 4 - - 6 9 -]]
 [[- 7 - - - 5 - 6 2]
  [5 - - - - - 4 - 3]
  [- - 3 - 2 - - 9 7]
  [- - - 8 - 4 - - 9]
  [- 5 - 2 9 7 - - 8]
  [7 - - - - 1 2 - 4]
  [- 6 - - - - - - -]
  [- - 4 - - 2 8 - -]
  [2 - - 5 - 3 - - -]])

=> (map measure-hardness *1)
({:nodes 137, :decision-nodes 7, :dead-ends 7}
 {:nodes 73, :decision-nodes 2, :dead-ends 2}
 {:nodes 67, :decision-nodes 1, :dead-ends 1}
 {:nodes 54, :decision-nodes 1, :dead-ends 1}
 {:nodes 92, :decision-nodes 2, :dead-ends 2})
```

So there you have it: five fresh, challenging Sudokus and the ability to create as many more as you'd like. Enjoy!

## Resources

* [Wikipedia entry for Exact Cover problems](https://en.wikipedia.org/wiki/Exact_cover)
* [Solving Sudoku with Dancing Links](http://buzzard.ups.edu/talks/beezer-2010-stellenbosch-sudoku.pdf)

## License

Copyright © 2016 Mark Engelberg

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
