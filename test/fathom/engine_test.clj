(ns fathom.engine-test
  (:require [clojure.test :refer :all]
            [fathom.core :as core]))

(deftest basic-engine
  (testing "Create empty engine"
    (let [engine (core/make-engine)]
      (is (empty? (core/facts engine))))))

(deftest forward-chaining-basic
  (testing "Simple forward chaining"
    (let [engine (core/make-engine
                   #{[:likes :alice :bob]}
                   [{:when [[:likes :alice :bob]]
                     :then [[:likes :bob :alice]]}])]
      (core/run-forward! engine)
      (is (contains? (core/facts engine) [:likes :bob :alice])))))

(deftest forward-chaining-variables
  (testing "Forward chaining instantiates variables"
    (let [engine (core/make-engine
                   #{[:parent :alice :bob]}
                   [{:when [[:parent '?x '?y]]
                     :then [[:ancestor '?x '?y]]}])]
      (core/run-forward! engine)
      (is (contains? (core/facts engine) [:ancestor :alice :bob])))))

(deftest backward-chaining-basic
  (testing "Query facts"
    (let [engine (core/make-engine
                   #{[:person :alice] [:person :bob]}
                   [])]
      (is (= 2 (count (core/facts engine))))
      (is (contains? (core/facts engine) [:person :alice]))
      (is (contains? (core/facts engine) [:person :bob])))))

(deftest backward-chaining-with-rules
  (testing "Forward chaining with rules"
    (let [engine (core/make-engine
                   #{[:parent :alice :bob]}
                   [{:when [[:parent '?x '?y]]
                     :then [[:ancestor '?x '?y]]}])]
      (core/run-forward! engine)
      (is (contains? (core/facts engine) [:ancestor :alice :bob])))))

(deftest backward-chaining-multi-step
  (testing "Multi-step chaining"
    (let [engine (core/make-engine
                   #{[:parent :alice :bob] [:parent :bob :carol]}
                   [{:when [[:parent '?x '?y]]
                     :then [[:ancestor '?x '?y]]}
                    {:when [[:ancestor '?x '?y] [:parent '?y '?z]]
                     :then [[:ancestor '?x '?z]]}])]
      (core/run-forward! engine)
      (is (contains? (core/facts engine) [:ancestor :alice :bob]))
      (is (contains? (core/facts engine) [:ancestor :bob :carol]))
      (is (contains? (core/facts engine) [:ancestor :alice :carol])))))

(deftest rule-addition
  (testing "Add rule to engine"
    (let [engine (core/make-engine)]
      (core/add-rule! engine {:when [[:a :x]] :then [[:b :x]]})
      (is (= 1 (count @(:rules engine)))))))

(deftest prove-basics
  (testing "prove works on facts"
    (let [engine (core/make-engine #{[:person :alice]} [])]
      (let [results (core/prove engine [:person :alice])]
        (is (seq results))))))
