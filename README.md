# Fathom

A Clojure inference engine supporting both forward and backward chaining over a fact base.

## Overview

Fathom provides an expressive, data-driven inference engine built on three core concepts:

- **Facts** – atomic knowledge units stored in a fact base
- **Patterns** – Prolog-style variable patterns used to query and match facts
- **Rules** – conditional logic that derives new facts or proves goals

Fathom supports two reasoning strategies:

| Strategy | Description |
|---|---|
| **Forward chaining** | Data-driven — applies rules repeatedly until no new facts can be derived (fixed point) |
| **Backward chaining** | Goal-driven — starts from a query and works backwards, finding proofs via unification |

## Requirements

- [Clojure](https://clojure.org/) 1.11.1+
- [Leiningen](https://leiningen.org/)

## Installation

Add Fathom to your `project.clj` dependencies:

```clojure
[fathom "0.1.0-SNAPSHOT"]
```

## Quick Start

```clojure
(require '[fathom.core :refer [make-engine assert! run! facts ask]])

;; Create an engine with initial facts and rules
(def engine
  (make-engine
    #{[:parent :alice :bob]
      [:parent :bob :carol]}
    [{:when [[:parent ?x ?y]] :then [[:ancestor ?x ?y]]}
     {:when [[:ancestor ?x ?y] [:parent ?y ?z]] :then [[:ancestor ?x ?z]]}]))

;; Run forward chaining to derive new facts
(run! engine)

(facts engine)
;; => #{[:parent :alice :bob]
;;      [:parent :bob :carol]
;;      [:ancestor :alice :bob]
;;      [:ancestor :bob :carol]
;;      [:ancestor :alice :carol]}

;; Query using backward chaining
(ask engine [:ancestor :alice ?who])
;; => ({:?who :bob} {:?who :carol})
```

## Core Concepts

### Facts

Facts are vectors of the form `[relation & arguments]`:

```clojure
[:person :alice]
[:age :alice 30]
[:likes :alice :bob]
```

Terms in a fact can be keywords, numbers, strings, booleans, `nil`, or nested vectors.

### Variables

Variables are symbols prefixed with `?` and are used in patterns:

```clojure
?x         ; named variable
?person    ; named variable
?          ; wildcard — matches anything, no binding
```

### Pattern Matching

A pattern is matched against a collection of facts to produce variable bindings:

```clojure
(require '[fathom.pattern :refer [match?]])

(match? [:likes ?x ?y] [:likes :alice :bob])
;; => {:?x :alice :?y :bob}

(match? [:likes ?x ?x] [:likes :alice :bob])
;; => nil  ; consistency check fails
```

### Unification

Unification extends pattern matching to allow variables on both sides:

```clojure
(require '[fathom.unify :refer [unify apply-subst]])

(unify [:likes ?x ?y] [:likes :alice ?y])
;; => {:?x :alice}

(apply-subst [:person ?name] {:?name :alice})
;; => [:person :alice]
```

### Rules

Rules have the form `{:when [antecedents] :then [consequents]}`:

```clojure
;; If X likes Y, then Y likes X
{:when [[:likes ?x ?y]]
 :then [[:likes ?y ?x]]
 :name "reciprocal-likes"
 :priority 10}

;; If X is parent of Y and Y is parent of Z, then X is grandparent of Z
{:when [[:parent ?x ?y] [:parent ?y ?z]]
 :then [[:grandparent ?x ?z]]}
```

## API Reference

### `fathom.core`

| Function | Description |
|---|---|
| `(make-engine)` | Create an empty engine |
| `(make-engine facts)` | Create an engine with initial facts |
| `(make-engine facts rules)` | Create an engine with facts and rules |
| `(assert! engine fact & more)` | Add one or more facts |
| `(retract! engine fact & more)` | Remove one or more facts |
| `(facts engine)` | Get current fact set |
| `(run! engine)` | Run forward chaining to fixed point |
| `(run! engine {:max-steps n :trace true})` | Run with options |
| `(prove engine goal)` | Backward chaining — all proofs |
| `(prove-one engine goal)` | Backward chaining — first proof |
| `(ask engine goal)` | Query — returns seq of binding maps |
| `(ask engine goal {:limit n :depth n :trace true})` | Query with options |
| `(configure! engine option value)` | Set engine configuration |
| `(stats engine)` | Get engine statistics |

### `fathom.facts`

| Function | Description |
|---|---|
| `(fact-base)` | Create an empty fact base |
| `(fact-base facts)` | Create a fact base from a set of facts |
| `(assert! fb fact & more)` | Add facts |
| `(retract! fb fact & more)` | Remove facts |
| `(facts fb)` | Get all facts |
| `(fact? fb fact)` | Check if a fact exists |
| `(clear! fb)` | Remove all facts |
| `(by-relation fb relation)` | Get facts by relation keyword |
| `(query fb pattern)` | Query with pattern, returns binding maps |

### `fathom.pattern`

| Function | Description |
|---|---|
| `(match? pattern fact)` | Match pattern against a fact, returns bindings or nil |
| `(match pattern facts)` | Match pattern against a collection of facts |
| `(bind pattern bindings)` | Apply bindings to a pattern |
| `(extract-vars pattern)` | Get the set of variables in a pattern |
| `(is-var? term)` | Check if a term is a variable |
| `(is-wildcard? term)` | Check if a term is a wildcard |

### `fathom.unify`

| Function | Description |
|---|---|
| `(unify t1 t2)` | Find the most general unifier of two terms |
| `(unify t1 t2 subst)` | Unify under an existing substitution |
| `(unify* & terms)` | Unify multiple terms |
| `(apply-subst term subst)` | Apply a substitution to a term |
| `(compose-subst s1 s2)` | Compose two substitutions |
| `(occurs? var term)` | Occurs check — prevents infinite structures |

### Configuration Options

Pass options to `configure!` to tune engine behaviour:

| Option | Values | Default | Description |
|---|---|---|---|
| `:strategy` | `:depth-first`, `:breadth-first`, `:iterative` | `:depth-first` | Backward chaining search strategy |
| `:max-depth` | integer | `10` | Maximum backward chaining depth |
| `:max-steps` | integer | `1000` | Maximum forward chaining steps |
| `:conflict-resolution` | `:mrs`, `:mevis`, `:simplicity`, `:priority`, `:random` | `:mrs` | Forward chaining conflict resolution |

```clojure
(configure! engine :strategy :breadth-first)
(configure! engine :max-depth 20)
(configure! engine :conflict-resolution :priority)
```

## Examples

### Transitive Closure (Forward Chaining)

```clojure
(def engine
  (make-engine
    #{[:connected :a :b] [:connected :b :c]}
    [{:when [[:connected ?x ?y]] :then [[:connected ?y ?x]]}
     {:when [[:connected ?x ?y] [:connected ?y ?z]] :then [[:connected ?x ?z]]}]))

(run! engine)
(facts engine)
;; => #{[:connected :a :b] [:connected :b :c]
;;      [:connected :b :a] [:connected :c :b]
;;      [:connected :a :c] [:connected :c :a]}
```

### Recursive Proof (Backward Chaining)

```clojure
(def engine
  (make-engine
    #{[:parent :alice :bob] [:parent :bob :carol]}
    [{:when [[:parent ?x ?y]] :then [[:ancestor ?x ?y]]}
     {:when [[:ancestor ?x ?y] [:parent ?y ?z]] :then [[:ancestor ?x ?z]]}]))

(ask engine [:ancestor :alice ?desc])
;; => ({:?desc :bob} {:?desc :carol})
```

### Negation as Failure

```clojure
(ask engine [:not [:likes :alice :broccoli]])
```

### Tracing

```clojure
(ask engine [:person ?x] {:trace true})
;; Prints proof steps to stdout
```

## Development

### Running Tests

```sh
lein test
```

### Project Structure

```
src/
  fathom/
    core.clj      ; main engine API
    facts.clj     ; fact base operations
    pattern.clj   ; pattern matching
    unify.clj     ; unification algorithm
test/
  fathom/
    engine_test.clj
    facts_test.clj
    pattern_test.clj
    unify_test.clj
specs/            ; detailed specifications for each subsystem
```

## Specifications

Detailed design documents for each subsystem are in the [`specs/`](specs/) directory:

- [`01-facts.md`](specs/01-facts.md) — Fact representation and fact base operations
- [`02-patterns.md`](specs/02-patterns.md) — Pattern matching and variable binding
- [`03-unification.md`](specs/03-unification.md) — Unification algorithm and substitutions
- [`04-forward-chaining.md`](specs/04-forward-chaining.md) — Forward chaining and conflict resolution
- [`05-backward-chaining.md`](specs/05-backward-chaining.md) — Backward chaining and proof trees
- [`06-api.md`](specs/06-api.md) — Full public API reference

## License

Copyright © 2024 Contributors

Distributed under the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/).
