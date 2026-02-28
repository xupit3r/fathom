# Backward Chaining Specification

## Overview

Backward chaining is a goal-driven inference strategy. Starting from a query (goal), it works backwards to find facts that support the goal by recursively proving subgoals.

## Core Concepts

### Goal

A goal is a pattern to prove:

```clojure
[:likes ?who :wine]
[:mortal :socrates]
[:ancestor :alice ?descendant]
```

### Proof Tree

Backward chaining builds a proof tree showing how goals were proven:

```
Goal: [:likes :alice ?x]
  └─ Found fact: [:likes :alice :bob]
     Bindings: {?x :bob}
  
  └─ Rule: {:when [[:friend ?x ?y]] :then [[:likes ?x ?y]]}
     └─ Subgoal: [:friend :alice ?y]
        └─ Found fact: [:friend :alice :bob]
           Bindings: {?y :bob}
```

## Core API

### prove

Attempts to prove a goal, returning all successful proofs.

```clojure
;; Signature
(prove [engine goal]) => seq-of-proofs
(prove [engine goal substitution]) => seq-of-proofs

;; Examples

;; Direct fact match
(let [engine (make-engine #{[:person :alice]})]
  (prove engine [:person :alice]))
=> [{:bindings {} :proof [:fact [:person :alice]]}]

;; With variable
(let [engine (make-engine #{[:person :alice] [:person :bob]})]
  (prove engine [:person ?name]))
=> [{:bindings {:?name :alice} :proof [:fact [:person :alice]]}
    {:bindings {:?name :bob} :proof [:fact [:person :bob]]}])
```

### prove-one

Returns the first successful proof.

```clojure
;; Signature
(prove-one [engine goal]) => proof-or-nil
(prove-one [engine goal substitution]) => proof-or-nil

;; Examples
(prove-one engine [:person ?name])
=> {:bindings {:?name :alice} :proof [:fact [:person :alice]]}
```

### ask

High-level query interface.

```clojure
;; Signature
(ask [engine goal]) => seq-of-bindings
(ask [engine goal {:keys [limit depth trace] :or {limit nil depth 10 trace false}}}])
  => seq-of-bindings

;; Examples
(ask engine [:likes :alice ?what])
=> ({:?what :wine} {:?what :cheese})

;; With limit
(ask engine [:person ?name] {:limit 1})
=> ({:?name :alice})
```

## Proof Structure

### Proof Node

```clojure
{:type :fact | :rule | :conjunction | :disjunction
 :bindings {}       ; variable bindings at this node
 :node fact|rule    ; the fact or rule used
 :children [...]    ; subproofs for rule premises}
```

### Proof Example

```clojure
;; Engine with:
;;   Fact: [:likes :alice :wine]
;;   Rule: {:when [[:friend ?x ?y]] :then [[:likes ?x ?y]]}
;;   Fact: [:friend :alice :bob]

(prove engine [:likes ?who ?what])

=> [{:bindings {?who :alice ?what :wine}
     :proof {:type :fact
             :node [:likes :alice :wine]}}
    {:bindings {?who :alice ?what :bob}
     :proof {:type :rule
             :node {:when [[:friend ?x ?y]] :then [[:likes ?x ?y]]}
             :children [{:type :fact
                         :node [:friend :alice :bob]
                         :bindings {?x :alice ?y :bob}}]}}]
```

## Inference Process

### Algorithm

```
function prove(goal, facts, rules, subst, depth):
  if depth > max-depth:
    return []
  
  ;; Try facts first
  for fact in facts:
    if unify(goal, fact, subst) succeeds:
      return [proof(fact, subst)]
  
  ;; Try rules (backward)
  for rule in rules where goal matches rule.then:
    for bindings in match(rule.when, facts, subst):
      subproofs = prove-all(rule.when, facts, rules, bindings, depth + 1)
      if subproofs is not empty:
        return [proof(rule, subproofs)]
  
  return []
```

### Search Strategies

| Strategy | Description |
|----------|-------------|
| `:depth-first` | Explore fully before backtracking (default) |
| `:breadth-first` | Explore all at current depth first |
| `:iterative` | Increase depth limit progressively |

### Strategy Configuration

```clojure
(configure! engine :strategy :depth-first)
(configure! engine :max-depth 20)
```

## Rule Handling

### Rule Selection

Rules are selected when their consequent (`:then`) can unify with the goal:

```clojure
;; Goal: [:likes ?x ?y]
;; Rule: {:when [[:friend ?x ?y]] :then [[:likes ?x ?y]]}

;; Unify goal with consequent:
(unify [:likes ?x ?y] [:likes ?x ?y])
=> {?x ?x :?y ?y}  ; identity bindings
```

### Proving Conjunctive Goals

Multiple premises are proven via recursive descent:

```clojure
;; Goal: [:grandparent ?x ?z]
;; Rule: {:when [[:parent ?x ?y] [:parent ?y ?z]] :then [[:grandparent ?x ?z]]}

;; Step 1: Prove [:parent ?x ?y]
;;   => Bindings: {?x :alice, ?y :bob}
;; Step 2: Prove [:parent ?y ?z] with {?y :bob}
;;   => Bindings: {?x :alice, ?y :bob, ?z :carol}
```

### Backtracking

When a proof path fails, the engine backtracks to try alternative paths:

```clojure
;; Goal: [:likes ?x :wine]
;; Facts: [:likes :alice :beer] [:likes :bob :wine]
;; Rules: (none)

(prove engine [:likes ?x :wine])
=> [{:bindings {?x :bob} :proof [:fact [:likes :bob :wine]]}]
```

## Negation as Failure

### not (Negation)

Proves negation via failure to prove:

```clojure
;; Syntax
(not [goal])

;; Example
[:not [:likes :alice :broccoli]]

;; Semantics:
;; Prove goal, if fails => not is true
;; If succeeds => not is false
```

### cut (!)

Stops backtracking once a certain point is reached:

```clojure
;; In rule consequent
{:when [[:bird ?x]]
 :then [[:flies ?x] !]}
```

## Examples

### Simple Query

```clojure
;; Facts
;;   [:person :alice]
;;   [:person :bob]
;;   [:age :alice 30]

(ask engine [:person ?who])
=> ({:?who :alice} {:?who :bob})
```

### Multi-Step Proof

```clojure
;; Facts:
;;   [:parent :alice :bob]
;;   [:parent :bob :carol]
;; Rules:
;;   {:when [[:parent ?x ?y]] :then [[:ancestor ?x ?y]]}
;;   {:when [[:ancestor ?x ?y] [:parent ?y ?z]] :then [[:ancestor ?x ?z]]}

(ask engine [:ancestor :alice ?desc])
=> ({:?desc :bob} {:?desc :carol})
```

### With Constraint

```clojure
;; Query: Find all ancestors of alice who are also parents
(ask engine [:and [:ancestor ?x :alice] [:parent ?x ?y]])
```

## Trace and Debug

### Enable Tracing

```clojure
(ask engine [:person ?x] {:trace true})

;; Output:
;; BEGIN prove [:person ?x]
;;   TRY fact [:person :alice]
;;     => Bind {:?x :alice}
;;   TRY fact [:person :bob]
;;     => Bind {:?x :bob}
;; END prove [:person ?x] => 2 proofs
```

### Explain Proof

```clojure
;; Get detailed proof tree
(explain engine [:likes :alice :wine])

=>
{:goal [:likes :alice :wine]
 :proof {:type :rule
         :rule {:when [[:friend ?x ?y]] :then [[:likes ?x ?y]]}
         :bindings {?x :alice ?y :bob}
         :subgoals
         [{:goal [:friend :alice ?y]
           :proof {:type :fact
                   :fact [:friend :alice :bob]
                   :bindings {?y :bob}}}}}}
```

## Implementation Requirements

1. **Soundness**: Only return valid proofs
2. **Completeness**: Find all proofs (within limits)
3. **Termination**: Respect max-depth
4. **Efficiency**: Avoid redundant proof attempts
5. **Debuggability**: Provide trace and explanation

## Edge Cases

| Case | Behavior |
|------|----------|
| Goal matches no facts/rules | Return empty |
| Circular rules | Detect via depth limit |
| Infinite proofs | Max-depth terminates |
| Unbound variables | Return all bindings |
| No rules needed | Direct fact lookup |

## Performance Considerations

- Memoize proof attempts
- Index rules by consequent
- Avoid re-proving same goals
- Prune failed branches early
- Incremental proof caching
