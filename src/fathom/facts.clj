(ns fathom.facts
  "Fact base operations")

(defn fact-base
  "Create a fact base"
  ([]
   (atom #{}))
  ([facts]
   (atom (set facts))))

(defn facts
  "Get all facts from fact base"
  [fb]
  @fb)

(defn assert!
  "Add facts to fact base"
  ([fb fact]
   (swap! fb conj fact)
   fb)
  ([fb fact & more-facts]
   (apply swap! fb conj (cons fact more-facts))
   fb))

(defn retract!
  "Remove facts from fact base"
  ([fb fact]
   (swap! fb disj fact)
   fb)
  ([fb fact & more-facts]
   (apply swap! fb disj (cons fact more-facts))
   fb))

(defn fact?
  "Check if fact exists in fact base"
  [fb fact]
  (contains? @fb fact))

(defn clear!
  "Remove all facts"
  [fb]
  (reset! fb #{})
  fb)

(defn by-relation
  "Get facts by relation name"
  [fb relation]
  (filter #(= (first %) relation) @fb))

(defn query
  "Query fact base with pattern"
  [fb pattern]
  [])
