(ns fathom.pattern-test
  (:require [clojure.test :refer :all]
            [fathom.pattern :as pattern]))

(deftest match-basics
  (testing "Exact match returns empty bindings"
    (is (= {} (pattern/match? [:person :alice] [:person :alice]))))

  (testing "No match returns nil"
    (is (nil? (pattern/match? [:person :alice] [:person :bob])))))

(deftest variable-matching
  (testing "Single variable binds to value"
    (is (= '{?name :alice}
           (pattern/match? [:person '?name] [:person :alice]))))

  (testing "Multiple variables"
    (is (= '{?x :alice ?y :bob}
           (pattern/match? [:likes '?x '?y] [:likes :alice :bob])))))

(deftest binding-consistency
  (testing "Same variable must bind to same value"
    (is (= '{?x :alice}
           (pattern/match? [:likes '?x '?x] [:likes :alice :alice]))))

  (testing "Inconsistent binding fails"
    (is (nil? (pattern/match? [:likes '?x '?x] [:likes :alice :bob])))))

(deftest wildcard-matching
  (testing "Wildcard matches anything"
    (is (= {} (pattern/match? [:likes '? :bob] [:likes :alice :bob])))))

(deftest bind-operation
  (testing "Apply bindings to pattern"
    (is (= [:person :alice]
           (pattern/bind [:person '?name] '{?name :alice})))))
