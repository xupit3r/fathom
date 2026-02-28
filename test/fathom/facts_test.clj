(ns fathom.facts-test
  (:require [clojure.test :refer :all]
            [fathom.facts :as facts]))

(deftest fact-base-creation
  (testing "Creates empty fact base"
    (let [fb (facts/fact-base)]
      (is (empty? (facts/facts fb)))))

  (testing "Creates fact base with initial facts"
    (let [fb (facts/fact-base #{[:person :alice] [:person :bob]})]
      (is (= 2 (count (facts/facts fb)))))))

(deftest assert-operations
  (testing "Assert single fact"
    (let [fb (facts/fact-base)]
      (facts/assert! fb [:person :alice])
      (is (facts/fact? fb [:person :alice]))))

  (testing "Assert multiple facts"
    (let [fb (facts/fact-base)]
      (facts/assert! fb [:a 1] [:b 2] [:c 3])
      (is (= 3 (count (facts/facts fb))))))

  (testing "Assert duplicate is idempotent"
    (let [fb (facts/fact-base #{[:a 1]})]
      (facts/assert! fb [:a 1])
      (is (= 1 (count (facts/facts fb)))))))

(deftest retract-operations
  (testing "Retract existing fact"
    (let [fb (facts/fact-base #{[:person :alice]})]
      (facts/retract! fb [:person :alice])
      (is (not (facts/fact? fb [:person :alice])))))

  (testing "Retract non-existent is idempotent"
    (let [fb (facts/fact-base #{[:a 1]})]
      (facts/retract! fb [:b 2])
      (is (= 1 (count (facts/facts fb)))))))

(deftest query-operations
  (testing "Query by relation"
    (let [fb (facts/fact-base #{[:likes :alice :bob] [:likes :bob :carol]})]
      (is (= 2 (count (facts/by-relation fb :likes))))))

  (testing "Query with pattern returns results"
    (let [fb (facts/fact-base #{[:person :alice] [:person :bob]})]
      (is (seq (facts/query fb [:person :alice]))))))

(deftest clear-operations
  (testing "Clear removes all facts"
    (let [fb (facts/fact-base #{[:a 1] [:b 2]})]
      (facts/clear! fb)
      (is (empty? (facts/facts fb))))))
