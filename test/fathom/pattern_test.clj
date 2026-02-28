(ns fathom.pattern-test
  (:require [clojure.test :refer :all]
            [fathom.pattern :refer :all]))

;; Pattern Matching Specification Tests
;; See specs/02-patterns.md

(deftest match-basics
  (testing "Exact match returns empty bindings"
    (is (= {} (match? [:person :alice] [:person :alice]))))

  (testing "No match returns nil"
    (is (nil? (match? [:person :alice] [:person :bob])))))

(deftest variable-matching
  (testing "Single variable binds to value"
    (is (= {:?name :alice}
           (match? [:person ?name] [:person :alice]))))

  (testing "Multiple variables"
    (is (= {:?x :alice :?y :bob}
           (match? [:likes ?x ?y] [:likes :alice :bob])))))

(deftest binding-consistency
  (testing "Same variable must bind to same value"
    (is (= {:?x :alice}
           (match? [:likes ?x ?x] [:likes :alice :alice]))))

  (testing "Inconsistent binding fails"
    (is (nil? (match? [:likes ?x ?x] [:likes :alice :bob]))))))

(deftest wildcard-matching
  (testing "Wildcard matches anything"
    (is (= {} (match? [:likes ? :bob] [:likes :alice :bob])))))

(deftest bind-operation
  (testing "Apply bindings to pattern"
    (is (= [:person :alice]
           (bind [:person ?name] {:?name :alice})))))
