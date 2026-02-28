(ns fathom.unify-test
  (:require [clojure.test :refer :all]
            [fathom.unify :refer :all]))

;; Unification Specification Tests
;; See specs/03-unification.md

(deftest unify-basics
  (testing "Unify identical literals"
    (is (= {} (unify :alice :alice))))

  (testing "Unify different literals fails"
    (is (nil? (unify :alice :bob)))))

(deftest unify-variables
  (testing "Variable unifies with literal"
    (is (= {?x :alice} (unify ?x :alice))))

  (testing "Literal unifies with variable"
    (is (= {?x :alice} (unify :alice ?x))))

  (testing "Two variables unify"
    (is (= {?x ?y} (unify ?x ?y)))))

(deftest occurs-check
  (testing "Occurs check prevents infinite structure"
    (is (nil? (unify ?x [:list ?x]))))

  (testing "Non-recursive unify succeeds"
    (is (= {?x :alice} (unify [:list ?x] [:list :alice])))))

(deftest substitution-operations
  (testing "Apply substitution"
    (is (= :alice (apply-subst ?x {?x :alice}))))

(deftest compose-subst
  (testing "Compose substitutions"
    (is (= {?x :bob :?y :alice}
           (compose-subst {?x :bob} {?y :alice})))))
