(ns fathom.facts-test
  (:require [clojure.test :refer :all]
            [fathom.facts :refer :all]))

;; Facts Specification Tests
;; See specs/01-facts.md

(deftest fact-base-creation
  (testing "Creates empty fact base"
    (let [fb (fact-base)]
      (is (empty? (facts fb)))))

  (testing "Creates fact base with initial facts"
    (let [fb (fact-base #{[:person :alice] [:person :bob]})]
      (is (= 2 (count (facts fb)))))))

(deftest assert-operations
  (testing "Assert single fact"
    (let [fb (fact-base)]
      (assert! fb [:person :alice])
      (is (fact? fb [:person :alice]))))

  (testing "Assert multiple facts"
    (let [fb (fact-base)]
      (assert! fb [:a 1] [:b 2] [:c 3])
      (is (= 3 (count (facts fb))))))

  (testing "Assert duplicate is idempotent"
    (let [fb (fact-base #{[:a 1]})]
      (assert! fb [:a 1])
      (is (= 1 (count (facts fb)))))))

(deftest retract-operations
  (testing "Retract existing fact"
    (let [fb (fact-base #{[:person :alice]})]
      (retract! fb [:person :alice])
      (is (not (fact? fb [:person :alice])))))

  (testing "Retract non-existent is idempotent"
    (let [fb (fact-base #{[:a 1]})]
      (retract! fb [:b 2])
      (is (= 1 (count (facts fb)))))))

(deftest query-operations
  (testing "Query by relation"
    (let [fb (fact-base #{[:likes :alice :bob] [:likes :bob :carol]})]
      (is (= 2 (count (by-relation fb :likes))))))

  (testing "Query with pattern"
    (let [fb (fact-base #{[:person :alice] [:person :bob]})]
      (is (some #{%{:?name :alice}} (query fb [:person ?name]))))))

(deftest clear-operations
  (testing "Clear removes all facts"
    (let [fb (fact-base #{[:a 1] [:b 2]})]
      (clear! fb)
      (is (empty? (facts fb))))))
