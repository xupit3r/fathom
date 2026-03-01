(ns fathom.core
  "Main inference engine API"
  (:require [fathom.pattern :as pattern]
            [fathom.unify :as unify]
            [fathom.backward :as backward]))

(defn make-engine
  "Create a new inference engine"
  ([]
    (make-engine #{} []))
  ([facts]
    (make-engine facts []))
  ([facts rules]
    {:facts (atom (set facts))
     :rules (atom (vec rules))
     :config (atom {:strategy :depth-first
                    :max-depth 10
                    :max-steps 1000
                    :conflict-resolution :mrs})}))

(defn facts
  "Get current facts from engine"
  [engine]
  @(:facts engine))

(defn rules
  "Get all rules from engine"
  [engine]
  @(:rules engine))

(defn assert!
  "Add facts to engine"
  ([engine fact]
    (swap! (:facts engine) conj fact)
    engine)
  ([engine fact & more-facts]
    (apply swap! (:facts engine) conj (cons fact more-facts))
    engine))

(defn add-rule!
  "Add a rule to engine"
  [engine rule]
  (swap! (:rules engine) conj rule)
  engine)

(defn retract!
  "Remove a fact from engine"
  [engine fact]
  (swap! (:facts engine) disj fact)
  engine)

(defn match-rule
  "Match a rule's antecedents against facts"
  [rule facts]
  (let [antecedents (:when rule)]
    (if (empty? antecedents)
      [{}]
      (let [all-bindings (atom [{}])]
        (doseq [ante antecedents]
          (let [matches (pattern/match ante facts)]
            (when (seq matches)
              (let [new-bindings (for [b1 @all-bindings
                                       b2 matches]
                                  (merge b1 b2))]
                (reset! all-bindings new-bindings)))))
        @all-bindings))))

(defn apply-consequent
  "Apply consequent with bindings"
  [consequent bindings]
  (pattern/bind consequent bindings))

(defn fire-rule
  "Fire a rule with given bindings"
  [rule bindings]
  (let [consequents (:then rule)]
    (map #(apply-consequent % bindings) consequents)))

(defn run-forward!
  "Run forward chaining to fixed point"
  ([engine]
    (run-forward! engine {}))
  ([engine opts]
    (let [max-steps (or (:max-steps opts) 1000)
          rules @(:rules engine)]
      (loop [step 0
             current-facts @(:facts engine)]
        (if (>= step max-steps)
          engine
          (let [new-facts (atom #{})]
            (doseq [rule rules
                    bindings (match-rule rule current-facts)]
              (doseq [result (fire-rule rule bindings)]
                (when-not (contains? current-facts result)
                  (swap! new-facts conj result))))
            (if (empty? @new-facts)
              engine
              (do
                (swap! (:facts engine) into @new-facts)
                (recur (inc step) @(:facts engine))))))))))

(defn prove
  "Prove a goal using backward chaining
  Returns seq of proof maps with bindings, path, goal, node
  Options:
    {:trace boolean
     :max-depth integer}"
  ([engine goal]
    (prove engine goal {}))
  ([engine goal opts]
    (backward/prove engine goal opts)))

(defn prove-one
  "Prove a goal, returning first proof"
  ([engine goal]
    (prove-one engine goal {}))
  ([engine goal opts]
    (first (prove engine goal opts))))

(defn ask
  "Query engine with goal pattern"
  ([engine goal]
    (ask engine goal {}))
  ([engine goal opts]
    (let [results (prove engine goal)]
      (map :bindings results))))

(defn explain
  "Explain how a goal was proven"
  [engine goal]
  (backward/explain engine goal))