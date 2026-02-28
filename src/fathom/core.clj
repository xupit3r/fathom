(ns fathom.core
  "Main inference engine API")

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

(defn assert!
  "Add facts to engine"
  ([engine fact]
   (swap! (:facts engine) conj fact)
   engine)
  ([engine fact & more-facts]
   (apply swap! (:facts engine) conj (cons fact more-facts))
   engine))

(defn retract!
  "Add a rule to engine"
  [engine rule]
  (swap! (:rules engine) conj rule)
  engine)

(defn run-forward!
  "Run forward chaining to fixed point"
  ([engine]
   (run-forward! engine {}))
  ([engine opts]
   engine))

(defn prove
  "Prove a goal using backward chaining"
  ([engine goal]
   (prove engine goal {}))
  ([engine goal subst]
   []))

(defn prove-one
  "Prove a goal, returning first proof"
  ([engine goal]
   (prove-one engine goal {}))
  ([engine goal subst]
   nil))

(defn ask
  "Query engine with goal pattern"
  ([engine goal]
   (ask engine goal {}))
  ([engine goal opts]
   []))

(defn configure!
  "Configure engine behavior"
  [engine option value]
  (swap! (:config engine) assoc option value)
  engine)

(defn stats
  "Get engine statistics"
  [engine]
  {:facts (count @(:facts engine))
   :rules (count @(:rules engine))})
