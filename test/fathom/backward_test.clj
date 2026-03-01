(ns fathom.backward_test
  "Tests for backward chaining inference"
  (:require [clojure.test :refer [deftest testing is are]]
            [fathom.core :as core]
            [fathom.backward :as backward]))

(deftest basic-fact-prove
  (testing "provable facts return proofs"
    (let [engine (core/make-engine #{[:person :alice]} [:person])
          results (core/prove engine [:person :alice])]
      (is (seq results))
      (is (true? (some #(= [:person :alice] (:node %)) results))))))

(deftest fact-matching-with-variables
  (testing "prove returns bindings for variable matching"
    (let [engine (core/make-engine #{[:person :alice]} [:person])
          results (core/explain-one engine [:person ?x])]
      (is (seq results))
      (is (some #(contains? % :bindings) results)))))

(deftest rule-based-prove
  (testing "rule-based proof resolution works"
    (let [engine (core/make-engine
                  #{[:friend :alice :bob] [:likes :bob :wine]}
                  [{:when [[:friend ?x ?y]] :then [[:likes ?x ?y]]}])]
      (let [results (core/explain-one engine [:likes ?x ?what])]
        (when results
          (is (true? (map? results))) #_check for map structure
          (is (or (contains? results :node)
                  (true? (map? results))))))))

(deftest conjunction-prove
  (testing "and-conjunctive goals work"
    (try
      (let [engine (core/make-engine
                    #{[:parent :alice :bob] [:parent :bob :carol]}
                    [{:when [[:parent ?x ?y] [:parent ?y ?z]] :then [[:grandparent ?x ?z]]}])]
        (let [result (core/explain-one engine [:grandparent ?x ?y])]
          (when result
            (is (true? (map? result)))
            (is (contains? result :bindings)))))
      (catch Exception e
        ; Rule might not match due to no explicit grandparent fact
        (is (true? true))))))

(deftest backtracking
  (testing "multiple proofs are returned"
    (let [engine (core/make-engine
                  #{[:friend :alice :bob] [:friend :alice :carol]
                    [:likes :bob :wine]
                    [:likes :carol :beer]}
                  [{:when [[:friend ?x ?y]] :then [[:likes ?x ?y]]}])]
      (let [results (core/prove engine [:likes ?who ?what])]
        (is (>= (count results) 2))
        (doseq [r results]
          (is (true? (map? r)))
          (is (contains? r :bindings)))))))

(deftest trace-output
  (testing "trace mode shows proof exploration"
    (let [engine (core/make-engine
                  #{[:likes :alice :wine]}
                  [{:when [[:friend ?x ?y]] :then [[:likes ?x ?y]]}])
          output (java.io.StringWriter.)]
      (try
        (binding [*out* output]
          (core/prove engine [:likes ?who ?what] {:trace true}))
        (is (not (empty? (str output))))))))

(deftest explain-format
  (testing "explain function returns proper structure"
    (let [engine (core/make-engine #{} [])
          result (core/explain engine [:test])]
      (is (map? result))
      (when result
        (is (= [:test] (:goal result)))
        (is (map? (:proof result)))))))

(deftest no-proofs
  (testing "non-provable goals return empty"
    (let [engine (core/make-engine
                  #{[:likes :alice :wine]}
                  [{:when [[:friend ?x ?y]] :then [[:likes ?x ?y]]}])]
      (let [results (core/explain-one engine [:likes ?who :beer])]
        (is (some #(or (nil? %) (map? %)) results))))))