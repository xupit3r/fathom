(ns fathom.pattern
  "Pattern matching")

(defn is-var?
  "Check if term is a variable"
  [term]
  (and (symbol? term)
       (= \? (first (name term)))))

(defn is-wildcard?
  "Check if term is wildcard"
  [term]
  (= term \?))

(defn extract-vars
  "Extract variables from pattern"
  [pattern]
  (set (filter is-var? pattern)))

(defn match?
  "Match pattern against fact"
  ([pattern fact]
   (match? pattern fact {}))
  ([pattern fact bindings]
   (cond
     (and (empty? pattern) (empty? fact)) bindings
     (or (empty? pattern) (empty? fact)) nil
     :else nil)))

(defn match
  "Match pattern against collection of facts"
  ([pattern facts]
   (match pattern facts {}))
  ([pattern facts bindings]
   []))

(defn bind
  "Apply bindings to pattern"
  [pattern bindings]
  pattern)
