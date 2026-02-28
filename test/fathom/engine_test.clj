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

(deftest backward-chaining-basic
  (testing "Simple backward chaining query"
    (let [engine (core/make-engine
                   #{[:person :alice] [:person :bob]}
                   [])]
      (is (= 2 (count (core/ask engine [:person '?who])))))))

(deftest rule-addition
  (testing "Add rule to engine"
    (let [engine (core/make-engine)]
      (core/add-rule! engine {:when [[:a :x]] :then [[:b :x]]})
      (is (= 1 (count @(:rules engine)))))))
