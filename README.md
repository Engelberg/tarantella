# Tarantella

Tarantella is "a rapid whirling dance, so named because it was thought to be a cure for tarantism, a psychological illness characterized by an extreme impulse to dance, prevalent in southern Italy from the 15th to the 17th century, and widely believed at the time to have been caused by the bite of a tarantula. The victim danced the tarantella until exhausted."

Tarantella is also an implementation of Knuth's Dancing Links algorithm, a brute-force backtracking search for solving exact cover problems, made blisteringly fast through the use of a very clever data structure -- a network of circular, doubly-linked lists.  The name of the algorithm comes from the way the links disconnect and reconnect from one another as the algorithm runs.  In tarantella, the low-level implementation of the algorithm is written in Java for speed, with a high-level interface written in Clojure.

You can learn about how tarantella works by watching my talk [Solving Problems Declaratively](https://youtu.be/TA9DBG8x-ys).

## Usage

[tarantella "1.1.0"]

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
   :ignore-columns   - A set of column labels you want to ignore
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
(def m { :A #{:c :e :f}
         :B #{:a :d :g}
         :C #{:b :c :f}
         :D #{:a :d}
         :E #{:b :g}
         :F #{:d :e :g} ])
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

* `:ignore-columns #{0 4}` - This example would treat columns 0 and 4 as "at most zero 1s". In other words, dancing-links won't allow selecting any row that has a 1 in those columns.

* `:select-rows #{:A :D}` - This example would begin the search process by selecting rows `:A` and `:D`. So `:A` and `:D` will be in every single solution.

* `:shuffle true` - This flag causes the dancing-links algorithm to shuffle the matrix rows and columns before searching, which has the effect of causing the algorithm to choose randomly among equally promising alternatives at each stage of the search. Thus, it will discover solutions in a random order.

* `:limit 8` - This example would limit the output of dancing-links to the first 8 solutions. If you use this keyword, then the vector of solutions returned by dancing-links will have metadata on it `{:search-ended-due-to-limit true/false}` which will tell you whether the search was terminated by reaching the limit, or whether the search terminated after finding all the solutions.

* `:timeout 3000` - This example would terminate the dancing-links search after 3000 ms, at which point it will return a vector of all the solutions it has found so far. If you use this keyword, then the vector of solutions returned by dancing-links will have metadata on it `{:search-ended-due-to-timeout true/false}` which will tell you whether the search was terminated by reaching the timeout, or whether the search terminated after finding all the solutions. You can use the `:limit` and `:timeout` flags together, and whichever is reached first will cause the search to terminate.

* `:lazy true` - This flag causes the dancing-links algorithm to return a lazy sequence of solutions, generated one at a time as needed, rather than returning a fully realized vector of solutions. Laziness can be convenient, but do be aware that you'll be paying approximately a 50% performance penalty.

## Metadata












## Resources

* [Wikipedia entry for Exact Cover problems](https://en.wikipedia.org/wiki/Exact_cover)
* [Solving Sudoku with Dancing Links](http://buzzard.ups.edu/talks/beezer-2010-stellenbosch-sudoku.pdf)

## License

Copyright © 2016 Mark Engelberg

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
