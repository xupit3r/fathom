(ns fathom.pattern
  "Pattern matching")

(defn is-var?
  "Check if term is a variable"
  [term]
  (and (symbol? term)
       (some? (re-find #"^\?" (name term)))))

(defn is-wildcard?
  "Check if term is wildcard"
  [term]
  (= term '?))

(defn extract-vars
  "Extract variables from pattern"
  [pattern]
  (set (filter is-var? pattern)))

(defn- match-one
  "Match pattern against single fact"
  [pattern fact bindings]
  (cond
    (and (empty? pattern) (empty? fact)) bindings
    (or (empty? pattern) (empty? fact)) nil
    :else
    (let [p (first pattern)
          f (first fact)
          new-bindings (cond
                         (is-wildcard? p)
                         bindings

                         (is-var? p)
                         (if (contains? bindings p)
                           (if (= (bindings p) f) bindings nil)
                           (assoc bindings p f))

                          (= p f)
                          bindings

                          :else nil)]
      (if new-bindings
        (recur (rest pattern) (rest fact) new-bindings)
        nil))))

(defn match?
  "Match pattern against fact"
  ([pattern fact]
   (match? pattern fact {}))
  ([pattern fact bindings]
   (match-one pattern fact bindings)))

(defn match
  "Match pattern against collection of facts"
  ([pattern facts]
   (match pattern facts {}))
  ([pattern facts bindings]
   (keep #(match-one pattern % bindings) facts)))

(defn bind
  "Apply bindings to pattern"
  [pattern bindings]
  (if (empty? bindings)
    pattern
    (mapv #(get bindings % %) pattern)))
