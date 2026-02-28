# Fathom Public API Specification

## Overview

This document defines the public API for the Fathom inference engine.

## Namespace Structure

```
fathom.core       - Main engine and high-level API
fathom.facts      - Fact base operations
fathom.pattern    - Pattern matching
fathom.unify      - Unification
fathom.rules      - Rule DSL
fathom.forward    - Forward chaining engine
fathom.backward   - Backward chaining engine
```

## fathom.core

### make-engine

Creates a new inference engine.

```clojure
;; Signature
(make-engine) => engine
(make-engine facts) => engine
(make-engine facts rules) => engine

;; Arguments
;;   facts  - collection of facts or fact base (default: #{})
;;   rules  - collection of rules (default: [])

;; Examples
(make-engine)
=> engine with empty fact base

(make-engine #{[:person :alice]})
=> engine with one fact

(make-engine #{[:person :alice]} [{:when [[:person ?x]] :then [[:human ?x]]}])
=> engine with facts and rules
```

### facts

Get current facts from engine.

```clojure
;; Signature
(facts [engine]) => set

;; Example
(facts engine)
=> #{[:person :alice] [:person :bob]}
```

### assert!

Add facts to engine.

```clojure
;; Signature
(assert! [engine fact] [engine fact & more-facts])

;; Example
(assert! engine [:person :charlie])
=> engine (updated)
```

### retract!

Remove facts from engine.

```clojure
;; Signature
(retract! [engine fact] [engine fact & more-facts])

;; Example
(retract! engine [:person :charlie])
=> engine (updated)
```

### add-rule!

Add a rule to engine.

```clojure
;; Signature
(add-rule! [engine rule]) => engine

;; Example
(add-rule! engine {:when [[:likes ?x ?y]] :then [[:likes ?y ?x]]})
=> engine (updated)
```

### run!

Run forward chaining to fixed point.

```clojure
;; Signature
(run! [engine]) => engine
(run! [engine {:keys [max-steps trace] :or {max-steps 1000 trace false}}]) => engine

;; Options
;;   :max-steps  - maximum rule firings (default: 1000)
;;   :trace      - enable tracing output (default: false)

;; Example
(run! engine)
=> engine with inferred facts
```

### prove

Prove a goal using backward chaining.

```clojure
;; Signature
(prove [engine goal]) => seq-of-proofs
(prove [engine goal substitution]) => seq-of-proofs

;; Example
(prove engine [:person ?who])
=> [{:bindings {?who :alice} :proof ...}]
```

### prove-one

Prove a goal, returning first proof.

```clojure
;; Signature
(prove-one [engine goal]) => proof-or-nil
(prove-one [engine goal substitution]) => proof-or-nil

;; Example
(prove-one engine [:person ?who])
=> {:bindings {?who :alice} :proof ...}
```

### ask

Query engine with goal pattern.

```clojure
;; Signature
(ask [engine goal]) => seq-of-bindings
(ask [engine goal {:keys [limit depth trace] :or {limit nil depth 10 trace false}}]) 
  => seq-of-bindings

;; Options
;;   :limit  - maximum results (default: nil)
;;   :depth  - max proof depth (default: 10)
;;   :trace  - enable tracing (default: false)

;; Example
(ask engine [:person ?who])
=> ({?who :alice} {?who :bob})
```

### configure!

Configure engine behavior.

```clojure
;; Signature
(configure! [engine option value]) => engine

;; Options
;;   :conflict-resolution - :mrs | :mevis | :simplicity | :priority | :random
;;   :strategy            - :depth-first | :breadth-first | :iterative
;;   :max-depth           - integer (backward chaining depth limit)
;;   :max-steps           - integer (forward chaining step limit)

;; Example
(configure! engine :strategy :depth-first)
(configure! engine :max-depth 20)
```

### stats

Get engine statistics.

```clojure
;; Signature
(stats [engine]) => map

;; Example
(stats engine)
=> {:facts 10
    :rules 5
    :forward-steps 23
    :backward-proofs 7}
```

## fathom.facts

### fact-base

Create a fact base.

```clojure
;; Signature
(fact-base) => fact-base
(fact-base facts) => fact-base

;; Example
(fact-base)
=> atom containing empty set

(fact-base #{[:a 1] [:b 2]})
=> atom containing facts
```

### fact?

Check if fact exists.

```clojure
;; Signature
(fact? [fb fact]) => boolean

;; Example
(fact? fb [:person :alice])
=> true
```

### query

Query fact base with pattern.

```clojure
;; Signature
(query [fb pattern]) => seq-of-bindings
(query [fb pattern {:keys [only limit] :or {only nil limit nil}}]) => seq-of-bindings

;; Example
(query fb [:person ?name])
=> ({:?name :alice} {:?name :bob})
```

### by-relation

Get facts by relation name.

```clojure
;; Signature
(by-relation [fb relation]) => seq

;; Example
(by-relation fb :likes)
=> ([:likes :alice :bob] [:likes :bob :carol])
```

### clear!

Remove all facts.

```clojure
;; Signature
(clear! [fb]) => fb

;; Example
(clear! fb)
=> empty fact base
```

## fathom.pattern

### match?

Match pattern against fact.

```clojure
;; Signature
(match? [pattern fact]) => bindings-or-nil
(match? [pattern fact bindings]) => bindings-or-nil

;; Example
(match? [:person ?name] [:person :alice])
=> {:?name :alice}
```

### match

Match pattern against facts collection.

```clojure
;; Signature
(match [pattern facts]) => seq-of-bindings
(match [pattern facts bindings]) => seq-of-bindings

;; Example
(match [:person ?name] #{[:person :alice] [:person :bob]})
=> ({:?name :alice} {:?name :bob})
```

### bind

Apply bindings to pattern.

```clojure
;; Signature
(bind [pattern bindings]) => term

;; Example
(bind [:person ?name] {:?name :alice})
=> [:person :alice]
```

### extract-vars

Extract variables from pattern.

```clojure
;; Signature
(extract-vars [pattern]) => set

;; Example
(extract-vars [:likes ?x ?y])
=> #{?x ?y}
```

### is-var?

Check if term is variable.

```clojure
;; Signature
(is-var? [term]) => boolean

;; Example
(is-var? ?x) => true
```

## fathom.unify

### unify

Unify two terms.

```clojure
;; Signature
(unify [term1 term2]) => substitution-or-nil
(unify [term1 term2 substitution]) => substitution-or-nil

;; Example
(unify ?x :alice)
=> {?x :alice}
```

### unify*

Unify multiple terms.

```clojure
;; Signature
(unify* [& terms]) => substitution-or-nil

;; Example
(unify* ?x ?y :alice)
=> {?x :alice :?y :alice}
```

### apply-subst

Apply substitution to term.

```clojure
;; Signature
(apply-subst [term substitution]) => term

;; Example
(apply-subst ?x {?x :alice})
=> :alice
```

### compose-subst

Compose substitutions.

```clojure
;; Signature
(compose-subst [s1 s2]) => substitution

;; Example
(compose-subst {?x :bob} {?y :alice})
=> {?x :bob :?y :alice}
```

### occurs?

Check if variable occurs in term.

```clojure
;; Signature
(occurs? [var term]) => boolean

;; Example
(occurs? ?x [:list ?x])
=> true
```

## fathom.rules

### defrule

Define a rule.

```clojure
;; Signature
(defrule name & body)
(defrule [name opts] & body)

;; Example
(defrule reciprocal-likes
  {:when [[:likes ?x ?y]]
   :then [[:likes ?y ?x]]})

(defrule named-rule
  {:when [[:human ?x]]
   :then [[:mortal ?x]]
   :priority 10})
```

### rule?

Check if value is a rule.

```clojure
;; Signature
(rule? [x]) => boolean

;; Example
(rule? {:when [[:a ?x]] :then [[:b ?x]]})
=> true
```

### rule-name

Get rule name.

```clojure
;; Signature
(rule-name [rule]) => any

;; Example
(rule-name my-rule)
=> :reciprocal-likes
```

## Versioning

This API follows [Semantic Versioning](https://semver.org/).

### Deprecation Policy

- Deprecated functions will be marked with deprecation metadata
- Deprecated functions will remain for at least 2 minor versions
- Migration guidance will be provided in docstrings
