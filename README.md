# Tarantella

Tarantella is "a rapid whirling dance, so named because it was thought to be a cure for tarantism, a psychological illness characterized by an extreme impulse to dance, prevalent in southern Italy from the 15th to the 17th century, and widely believed at the time to have been caused by the bite of a tarantula. The victim danced the tarantella until exhausted."

Tarantella is also an implementation of Knuth's Dancing Links algorithm, a brute-force backtracking search for solving exact cover problems, made blisteringly fast through the use of a very clever data structure -- a network of circular, doubly-linked lists.  The name of the algorithm comes from the way the links disconnect and reconnect from one another as the algorithm runs.  In tarantella, the low-level implementation of the algorithm is written in Java for speed, with a high-level interface written in Clojure.

Tarantella is feature complete, but I'm using this library as an opportunity to learn clojure.spec, which is still in alpha.

So, I will complete the README and officially launch this library when clojure.spec has stabilized.  If you don't know what exact cover problems are, and why you'd want to use this algorithm to solve them, stay tuned...

In the meantime, you can learn about how tarantella works by watching my talk [Solving Problems Declaratively](https://youtu.be/TA9DBG8x-ys).

## Usage

[tarantella "1.1.0"]

The API exposes a single function, `tarantella.core/dancing-links` which can take exact cover problems in three different input formats.  See doc string for details.

You can instrument the function with clojure.spec for full input checking.

## Resources

* [Wikipedia entry for Exact Cover problems](https://en.wikipedia.org/wiki/Exact_cover)
* [Solving Sudoku with Dancing Links](http://buzzard.ups.edu/talks/beezer-2010-stellenbosch-sudoku.pdf)

## License

Copyright © 2016 Mark Engelberg

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
