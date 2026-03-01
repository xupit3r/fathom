(ns fathom.backward_test
  "Tests for backward chaining inference"
  (:require [clojure.test :refer :all]
            [fathom.core :as core]))

(deftest basic-fact-prove
  (testing "provable facts return proofs"
    (let [engine (core/make-engine #{[:person :alice]} [:person])
          results (core/prove engine [:person :alice])]
      (is (seq results))) ))

(deftest explain-format
  (testing "explain function returns proper structure"
    (let [engine (core/make-engine #{} [])
          result (core/explain engine [:test])]
      (is (map? result))) ))

(deftest no-proofs
  (testing "non-provable goals return empty"
    (let [engine (core/make-engine
                  #{[:likes :alice :wine]}
                  [{:when [[:friend '?x '?y]] :then [[:likes '?x '?y]]}])]
      (let [results (core/prove engine [:likes "who"])]
        (is (some #(or (nil? %) (map? %)) results))))))