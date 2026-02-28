# Forward Chaining Specification

## Overview

Forward chaining is a data-driven inference strategy. Given a set of facts and rules, it repeatedly applies rules to derive new facts until no more can be inferred (fixed point).

## Rule Structure

### Rule Representation

```clojure
{:when [antecedent-patterns]
 :then [consequent-facts]
 :name "optional-name"
 :priority integer}
```

### Components

| Component | Description |
|-----------|-------------|
| `:when` | Vector of patterns that must match facts |
| `:then` | Vector of facts to add when rule fires |
| `:name` | Optional identifier for debugging |
| `:priority` | Conflict resolution priority (higher fires first) |

### Examples

```clojure
;; Simple rule: If X likes Y, then Y likes X
{:when [[:likes ?x ?y]]
 :then [[:likes ?y ?x]]
 :name "reciprocal-likes"}

;; Multi-pattern rule: If X is parent of Y, and Y is parent of Z, then X is grandparent of Z
{:when [[:parent ?x ?y] [:parent ?y ?z]]
 :then [[:grandparent ?x ?z]]}

;; Rule with no variables
{:when [[:mortal :socrates]]
 :then [[:mortal :socrates]]}
```

## Core API

### defrule

Creates a rule definition.

```clojure
;; Signature
(defrule name & body)
(defrule [name opts] & body)

;; Examples

(defrule reciprocal-likes
  {:when [[:likes ?x ?y]]
   :then [[:likes ?y ?x]]})

(defrule grandparent
  {:when [[:parent ?x ?y] [:parent ?y ?z]]
   :then [[:grandparent ?x ?z]]})

(defrule named-rule
  {:when [[:human ?x]]
   :then [[:mortal ?x]]
   :name "humans-are-mortal"
   :priority 10})
```

### add-rule!

Adds a rule to the engine.

```clojure
;; Signature
(add-rule! [engine rule]) => engine

;; Example
(add-rule! engine {:when [[:likes ?x ?y]] :then [[:likes ?y ?x]]})
```

### run!

Executes forward chaining until fixed point.

```clojure
;; Signature
(run! [engine]) => engine
(run! [engine {:keys [max-steps trace] :or {max-steps 1000 trace false}}]) => engine

;; Example
(run! engine)
;; Returns engine with new facts inferred
```

## Conflict Resolution

When multiple rules can fire, conflict resolution determines which executes.

### Strategies

| Strategy | Description |
|----------|-------------|
| `:mrs` | Most Recent Specificity (default) |
| `:mevis` | Most Evidence, then Specificity, then Recency |
| `:simplicity` | Fewest premises |
| `:priority` | User-defined priority |
| `:random` | Random selection |

### Strategy Configuration

```clojure
;; Set strategy
(configure! engine :conflict-resolution :mrs)

;; MRS: Most Recent + Specificity
;; - More specific rules (more variables bound) win
;; - Among same specificity, most recently added fact wins

;; Mevis: Most Evidence + Specificity + Recency
;; - Rules matching more facts win
;; - Then most specific
;; - Then most recent fact
```

### Priority-Based

```clojure
(defrule high-priority
  {:when [[:important ?x]]
   :then [[:process ?x]]
   :priority 100})

(defrule low-priority
  {:when [[:normal ?x]]
   :then [[:process ?x]]
   :priority 10})
```

## Inference Process

### Algorithm

```
function forward-chain(facts, rules):
  agenda = initial-matched-rules(facts, rules)
  fired = {}
  
  while agenda is not empty:
    rule = select-rule(agenda, conflict-resolution)
    agenda = agenda - rule
    
    if rule not in fired or not (:once rule):
      bindings = match-rule(rule, facts)
      new-facts = instantiate-consequents(rule, bindings)
      
      added = new-facts - facts
      facts = facts ∪ added
      
      if added is not empty:
        agenda = agenda ∪ new-matched-rules(rules, added, facts)
        fired = fired ∪ {rule}
  
  return facts
```

### Execution Steps

1. **Match**: Find all rules whose antecedents match current facts
2. **Select**: Choose one rule using conflict resolution
3. **Apply**: Instantiate consequent with bindings
4. **Assert**: Add new facts to fact base
5. **Repeat**: Continue until no more rules can fire

### Rule Firing Events

```clojure
;; With tracing
(run! engine {:trace true})

;; Event hooks
(on engine :fire (fn [rule bindings new-facts] ...))
(on engine :match (fn [rule matched-facts] ...))
(on engine :assert (fn [fact] ...))
```

## Working Memory

### Engine State

```clojure
;; Create engine with fact base and rules
(make-engine)  ; empty
(make-engine facts)  ; with initial facts
(make-engine facts rules)  ; with facts and rules

;; Access components
(facts engine)  ; current facts
(rules engine)  ; defined rules
(agenda engine) ; rules ready to fire

;; Statistics
(stats engine)
=> {:facts 100
    :rules 10
    :firings 45
    :steps 23}
```

### Engine Lifecycle

```clojure
(let [engine (make-engine)]
  (add-rule! engine my-rule)
  (assert! engine [:likes :alice :bob])
  (run! engine)
  (facts engine))  ; contains inferred facts
```

## Examples

### Basic Example

```clojure
;; Facts: [:likes :alice :bob]
;; Rule: If X likes Y, then Y likes X

(let [engine (make-engine #{[:likes :alice :bob]}
                          [{:when [[:likes ?x ?y]]
                            :then [[:likes ?y ?x]]}])]
  (run! engine)
  (facts engine))
=> #{[:likes :alice :bob] [:likes :bob :alice]}
```

### Multi-Step Inference

```clojure
;; Facts:
;;   [:parent :alice :bob]
;;   [:parent :bob :carol]
;; Rule: If parent X Y, then ancestor X Y
;; Rule: If ancestor X Y and parent Y Z, then ancestor X Z

(let [engine (make-engine
               #{[:parent :alice :bob] [:parent :bob :carol]}
               [{:when [[:parent ?x ?y]] :then [[:ancestor ?x ?y]]}
                {:when [[:ancestor ?x ?y] [:parent ?y ?z]]
                 :then [[:ancestor ?x ?z]]}])]
  (run! engine)
  (facts engine))
=> #{[:parent :alice :bob]
     [:parent :bob :carol]
     [:ancestor :alice :bob]
     [:ancestor :bob :carol]
     [:ancestor :alice :carol]}
```

### Recursive Rules

```clojure
;; Facts: [:connected :a :b], [:connected :b :c]
;; Rule: If connected X Y, then connected Y X
;; Rule: If connected X Y and connected Y Z, then connected X Z

(let [engine (make-engine
               #{[:connected :a :b] [:connected :b :c]}
               [{:when [[:connected ?x ?y]] :then [[:connected ?y ?x]]}
                {:when [[:connected ?x ?y] [:connected ?y ?z]]
                 :then [[:connected ?x ?z]]}])]
  (run! engine)
  (facts engine))
=> #{[:connected :a :b] [:connected :b :c]
     [:connected :b :a] [:connected :c :b]
     [:connected :a :c] [:connected :c :a]}
```

## Implementation Requirements

1. **Termination**: Must detect fixed point (no new facts)
2. **Idempotence**: Running again produces same result
3. **Conflict Resolution**: Configurable strategies
4. **Efficiency**: Only re-evaluate affected rules on new facts
5. **Debugging**: Trace and explain rule firings

## Edge Cases

| Case | Behavior |
|------|----------|
| No rules | Return input facts |
| No matching facts | Return input facts |
| Circular dependencies | Detect via fixed point |
| Contradictory facts | Allow (handled by fact base) |
| Infinite inference | Max steps limit |
| Rule with no antecedents | Fire immediately |
| Duplicate facts | Ignored (fact base handles) |

## Performance Considerations

- Incremental rule matching (only check affected)
- Index patterns by first term
- Cache intermediate matches
- Lazy agenda evaluation
- Parallel rule evaluation (future)
