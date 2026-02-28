(ns fathom.unify-test
  (:require [clojure.test :refer :all]
            [fathom.unify :as unify]))

(deftest unify-basics
  (testing "Unify identical literals"
    (is (= {} (unify/unify :alice :alice))))
  (testing "Unify different literals fails"
    (is (nil? (unify/unify :alice :bob)))))

(deftest unify-variables
  (testing "Variable unifies with literal"
    (is (= '{?x :alice} (unify/unify (quote ?x) :alice))))
  (testing "Literal unifies with variable"
    (is (= '{?x :alice} (unify/unify :alice (quote ?x)))))
  (testing "Two variables unify"
    (is (= '{?x ?y} (unify/unify (quote ?x) (quote ?y))))))

(deftest substitution-operations
  (testing "Apply substitution"
    (is (= :alice (unify/apply-subst (quote ?x) { (quote ?x) :alice })))))

(deftest compose-subst
  (testing "Compose substitutions"
    (is (= { (quote ?x) :bob (quote ?y) :alice }
           (unify/compose-subst { (quote ?x) :bob } { (quote ?y) :alice })))))
