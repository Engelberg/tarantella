(ns tarantella.core-test
  (:require 
    clojure.test
    [tarantella.core :as t]))

;; I use this library in my own work, but cannot open-source my tests.
;; I would welcome pull requests with tests and examples.

;; This would also be a good exercise for someone who wants to learn test.check
;; to generate random matrices of 0s and 1s and check that the output is a valid covering.
;; Checking the output would be straightforward for matrix inputs, 
;; just recover the original rows from the row numbers, then
;; (apply map + list-of-rows) and check that it is all 1's.
