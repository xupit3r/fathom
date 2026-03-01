(ns fathom.backward
  "Backward chaining inference engine"
  (:require [fathom.pattern :as pattern]
            [fathom.unify :as unify]
            [clojure.set :as set]))

(defn match-rules-consequent
  "Find rules that can prove a goal"
  [goal rules]
  (let [goal-vars (pattern/extract-vars goal)]
    (filter (fn [rule]
              (let [then-clause (:then rule)]
                (and (not (empty? then-clause))
                     (not (empty? goal-vars))
                     (set/subset? (pattern/extract-vars then-clause) goal-vars)
                     (not (nil? (unify/unify* goal then-clause))))))
            rules)))

(defn conj-bindings
  "Prove conjunction goals"
  [conjunctives facts rules bindings max-depth config]
  (if (empty? conjunctives)
    [bindings]
    (let [head (first conjunctives)
          bindings0 (unify/unify* head head bindings)]
      (if bindings0
        (let [tail-bindings (conj-bindings (rest conjunctives) facts rules bindings0 max-depth config)]
          (if tail-bindings
            (map #(unify/compose-subst bindings0 %) tail-bindings)
            []))
        []))))

(defn prove-facts
  "Try to prove goal by matching facts"
  [goal facts]
  (pattern/match goal facts))

(defn prove-rules
  "Try to prove goal using rules"
  [goal rules facts bindings depth config]
  (let [matching-rules (match-rules-consequent goal rules)
        trace? (get config :trace false)]
    (if (seq matching-rules)
      (mapv (fn [rule]
              (let [then-clause (:then rule)
                    bindings0 (unify/unify* goal then-clause)
                    antecedents (:when rule)]
                (if (or (empty? antecedents) bindings0)
                  (if bindings0
                    {:bindings bindings0
                     :goal goal
                     :path [bindings0]
                     :node rule}
                    nil)
                  (let [sub-bindings (conj-bindings antecedents facts rules bindings0 depth config)]
                    (if (seq sub-bindings)
                      {:bindings bindings0
                       :goal goal
                       :path [bindings0 sub-bindings]
                       :node rule}
                      nil)))))
            matching-rules)
      [])))

(defn prove*
  "Recursive prover"
  [goal facts rules bindings depth max-depth config]
  (when (<= depth max-depth)
    (let [goal' (unify/apply-subst goal bindings)
          fact-results (prove-facts goal' facts)
          depth' (inc depth)
          rule-results (prove-rules goal' rules facts bindings depth' config)]
      (if (seq fact-results)
        fact-results
        rule-results))))

(defn prove
  "Prove a goal using backward chaining"
  ([engine goal]
   (prove engine goal {}))
  ([engine goal opts]
   (let [config (merge @(:config engine) (into {} opts))
         facts @(:facts engine)
         rules @(:rules engine)
         max-depth (or (:max-depth opts) (:max-depth config))
         trace? (get config :trace false)]
     (when trace?
       (println (str "BEGIN prove " goal)))
     (let [results (prove* goal facts rules {} 0 max-depth config)]
       (when (and trace? results)
         (println (str "END prove " goal " => " (count results) " proofs")))
       results))))

(defn explain
  "Get detailed proof structure for a goal"
  [engine goal & [opts]]
  (if (seq (prove engine goal opts))
    {:goal goal
     :proof (first (prove engine goal opts))
     :paths (prove engine goal opts)}
    nil))

(defn explain-one
  "Get first proof path only"
  [engine goal & [opts]]
  (first (prove engine goal opts)))