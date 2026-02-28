# Facts Specification

## Overview

Facts are the fundamental unit of knowledge in Fathom. They represent atomic pieces of information that the inference engine operates on.

## Data Model

### Fact Representation

A fact is a vector containing symbolic or literal values:

```
[relation & arguments]
```

- `relation`: A keyword or symbol identifying the relationship
- `arguments`: Zero or more terms (atoms, variables, or nested facts)

#### Examples

```clojure
;; Simple facts
[:person :alice]
[:likes :alice :bob]
[:age :alice 30]

;; Facts with nested structure
[:parent [:person :alice] [:person :bob]]
[:point 10 20]

;; Facts with variables (handled by pattern matcher/unifier)
[:likes ?x ?y]
```

### Term Types

| Type | Description | Example |
|------|-------------|---------|
| Keyword | Symbolic relation or argument | `:alice`, `:likes` |
| Symbol | Variable placeholder | `?x`, `?person` |
| Number | Numeric literal | `30`, `3.14` |
| String | Text literal | `"hello"` |
| Boolean | Logical value | `true`, `false` |
| Nil | Null value | `nil` |
| Vector | Nested fact | `[:person :alice]` |

## Fact Base Operations

### Creation

```clojure
;; Create an empty fact base
(fact-base) => atom containing empty set

;; Create with initial facts
(fact-base #{[:person :alice] [:person :bob]})
```

### Core Operations

#### assert!

Adds a fact to the fact base.

```clojure
;; Signature
(assert! [fb fact] [fb fact & more-facts])

;; Examples
(let [fb (fact-base)]
  (assert! fb [:person :alice])
  (facts fb) => #{[:person :alice]}

  ;; Multiple facts
  (assert! fb [:person :bob] [:age :alice 30])
  (count (facts fb)) => 3)
```

**Rules:**
- Facts must be vectors
- Duplicate facts are idempotent (no-op)
- Returns the asserted fact(s)

#### retract!

Removes a fact from the fact base.

```clojure
;; Signature
(retract! [fb fact] [fb fact & more-facts])

;; Examples
(let [fb (fact-base #{[:person :alice] [:person :bob]})]
  (retract! fb [:person :alice])
  (facts fb) => #{[:person :bob]})

;; Retracting non-existent fact is idempotent
(let [fb (fact-base #{[:person :alice]})]
  (retract! fb [:person :bob])
  (facts fb) => #{[:person :alice]})
```

**Rules:**
- Returns the retracted fact(s)
- Non-existent facts return nil without error

#### facts

Retrieves all facts in the fact base.

```clojure
;; Signature
(facts [fb]) => set

;; Examples
(let [fb (fact-base #{[:a 1] [:b 2]})]
  (facts fb) => #{[:a 1] [:b 2]})
```

#### fact?

Checks if a fact exists in the fact base.

```clojure
;; Signature
(fact? [fb fact]) => boolean

;; Examples
(let [fb (fact-base #{[:person :alice]})]
  (fact? fb [:person :alice]) => true
  (fact? fb [:person :bob]) => false)
```

#### clear!

Removes all facts from the fact base.

```clojure
;; Signature
(clear! [fb]) => fact-base

;; Examples
(let [fb (fact-base #{[:a 1]})]
  (clear! fb)
  (facts fb) => #{})
```

## Query Operations

### Query by Relation

```clojure
;; Signature
(by-relation [fb relation]) => seq

;; Examples
(let [fb (fact-base #{[:likes :alice :bob] [:likes :bob :carol] [:hates :alice :charlie]})]
  (by-relation fb :likes) => ([:likes :alice :bob] [:likes :bob :carol]))
```

### Query Facts Matching Pattern

See [Pattern Matching](02-patterns.md) for detailed query semantics.

```clojure
;; Signature
(query [fb pattern]) => seq of bindings

;; Examples
(let [fb (fact-base #{[:likes :alice :bob] [:likes :bob :carol] [:likes :alice :carol]})]
  (query fb [:likes ?x ?y])
  => ({:?x :alice :?y :bob}
      {:?x :bob :?y :carol}
      {:?x :alice :?y :carol}))

;; With filter
(query fb [:likes :alice ?who] :only [:?who])
=> (:bob :carol))
```

## Implementation Requirements

1. **Thread Safety**: Fact base must be thread-safe via atom with validator
2. **Immutability**: Operations return new state, don't mutate input facts
3. **Idempotency**: Assert/retract are idempotent operations
4. **Performance**: Fact lookups should be O(1) for single fact queries

## Data Structures

### Internal Representation

```clojure
;; Fact base is an atom containing a map for indexed lookups
{
  :facts #{[:relation arg1 arg2]...}  ; primary fact set
  :index {:relation #{[fact]...}}      ; relation-based index
}
```

### Indexing Strategy

- Primary storage: Set of facts for O(1) membership tests
- Secondary index: Map from relation keywords to fact sets for fast filtering

## Edge Cases

| Case | Behavior |
|------|----------|
| Empty fact vector | Invalid, throw ex-info |
| Nested vectors as args | Allowed, treated as nested facts |
| Duplicate assert | Idempotent, no error |
| Retract non-existent | Idempotent, returns nil |
| Nil in fact | Allowed as argument |
| Mixed case keywords/symbols | Case-sensitive matching |

## Future Considerations

- Persistent indexed data structures for large fact bases
- Transaction support for bulk operations
- Fact versioning and timestamps
- Time-windowed facts (facts that expire)
