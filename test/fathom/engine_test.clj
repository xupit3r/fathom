(ns fathom.engine-test
  (:require [clojure.test :refer :all]
            [fathom.core :refer :all]))

;; Integration Tests for Forward and Backward Chaining
;; See specs/04-forward-chaining.md and specs/05-backward-chaining.md

(deftest basic-engine
  (testing "Create empty engine"
    (let [engine (make-engine)]
      (is (empty? (facts engine))))))

(deftest forward-chaining-basic
  (testing "Simple forward chaining"
    (let [engine (make-engine
                   #{[:likes :alice :bob]}
                   [{:when [[:likes ?x ?y]]
                     :then [[:likes ?y ?x]]}])]
      (run-forward! engine)
      (is (contains? (facts engine) [:likes :bob :alice])))))

(deftest backward-chaining-basic
  (testing "Simple backward chaining query"
    (let [engine (make-engine
                   #{[:person :alice] [:person :bob]}
                   [])]
      (is (= #{{:?who :alice} {:?who :bob}}
             (set (ask engine [:person ?who])))))))

(deftest rule-addition
  (testing "Add rule to engine"
    (let [engine (make-engine)]
      (add-rule! engine {:when [[:a ?x]] :then [[:b ?x]]})
      (is (= 1 (count (engine :rules)))))))
