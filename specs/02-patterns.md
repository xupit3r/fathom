# Pattern Matching Specification

## Overview

Pattern matching is the mechanism by which Fathom matches facts against patterns containing variables. It forms the foundation for both forward and backward chaining.

## Variable Syntax

### Variable Identifiers

Variables are symbols prefixed with `?`:

```
?variable-name
```

- Must start with `?`
- Followed by one or more alphanumeric characters or hyphens
- Case-sensitive: `?x` and `?X` are different variables

### Examples

```clojure
?x
?person
?first-name
?x1
```

### Wildcard Variables

A single `?` matches any single term without binding:

```clojure
;; Matches anything in this position, doesn't bind
[:likes ? :bob]  ; matches [:likes :alice :bob], [:likes :bob :bob]
```

## Core Functions

### match?

Determines if a pattern matches a fact and returns variable bindings.

```clojure
;; Signature
(match? [pattern fact]) => bindings-or-nil
(match? [pattern fact bindings]) => bindings-or-nil

;; With initial bindings
(match? [pattern fact] {:initial-bindings {}}) => bindings-or-nil

;; Examples

;; Exact match (no variables)
(match? [:person :alice] [:person :alice])
=> {}  ; empty bindings = success

;; Simple variable
(match? [:person ?name] [:person :alice])
=> {:?name :alice}

;; Multiple variables
(match? [:likes ?x ?y] [:likes :alice :bob])
=> {:?x :alice :?y :bob}

;; Nested patterns
(match? [:father ?parent ?child] [:father :bob [:child :alice]])
=> {:?parent :bob :?child [:child :alice]}
```

**Returns:**
- `nil` if no match
- Map of bindings if matched (empty map for exact match)

### match

Returns all possible matches of a pattern against a collection of facts.

```clojure
;; Signature
(match [pattern facts]) => seq-of-bindings
(match [pattern facts bindings]) => seq-of-bindings

;; Examples

(let [facts #{[:person :alice] [:person :bob] [:age :alice 30]}]
  (match [:person ?name] facts)
  => ({:?name :alice} {:?name :bob})

  ;; With pre-existing bindings
  (match [:likes ?x ?y] facts {:?x :alice})
  => ({:?x :alice :?y :bob} {:?x :alice :?y :carol}))
```

### bind

Applies bindings to a pattern, producing an instantiated fact.

```clojure
;; Signature
(bind [pattern bindings]) => fact

;; Examples

(bind [:person ?name] {:?name :alice})
=> [:person :alice]

(bind [:likes ?x ?y] {:?x :alice :?y :bob})
=> [:likes :alice :bob]

;; With no variables - returns original
(bind [:person :alice] {})
=> [:person :alice]
```

## Matching Rules

### Structural Matching

1. **Length**: Pattern and fact must have same number of elements
2. **Position**: Each element matches position-by-position

### Term Matching

| Pattern Term | Fact Term | Result |
|--------------|-----------|--------|
| Literal (keyword, number, string) | Same literal | Match |
| Literal | Different literal | No match |
| Variable `?x` | Any term | Match, bind `?x` to term |
| Wildcard `?` | Any term | Match, no binding |
| Nested pattern | Nested fact | Recursive match |
| Nested pattern | Non-vector | No match |

### Binding Consistency

A pattern cannot bind the same variable to different values:

```clojure
;; This fails - ?x would need to be both :alice and :bob
(match? [:likes ?x ?x] [:likes :alice :bob])
=> nil
```

```clojure
;; This succeeds - ?x is bound to :alice, then matched against :alice
(match? [:likes ?x ?x] [:likes :alice :alice])
=> {:?x :alice}
```

## Advanced Patterns

### Conjunction (AND)

Multiple patterns must all match:

```clojure
;; Using match-all
(match-all [[:person ?name] [:age ?name ?age]] facts)
=> ({:?name :alice :?age 30} {:?name :bob :?age 25})

;; Facts must come from same binding set
```

### Disjunction (OR)

At least one pattern must match:

```clojure
;; Using match-any
(match-any [[:person ?x] [:likes ?x :wine]] facts)
```

### Negation

Pattern must NOT match:

```clojure
;; Using match-not
(match-not [:person ?name] [:banned ?name] facts)
```

## Pattern Validation

### is-var?

Checks if a term is a variable.

```clojure
;; Signature
(is-var? [term]) => boolean

;; Examples
(is-var? ?x) => true
(is-var? :alice) => false
(is-var? 42) => false
(is-var? ?) => true
```

### is-wildcard?

Checks if a term is a wildcard.

```clojure
;; Signature
(is-wildcard? [term]) => boolean

;; Examples
(is-wildcard? ?) => true
(is-wildcard? ?x) => false
```

### extract-vars

Extracts all variables from a pattern.

```clojure
;; Signature
(extract-vars [pattern]) => set

;; Examples
(extract-vars [:likes ?x ?y])
=> #{?x ?y}

(extract-vars [:person ?x])
=> #{?x}

(extract-vars [:person :alice])
=> #{}
```

## Implementation Details

### Algorithm

Pattern matching uses recursive descent with accumulation:

```
function match(pattern, fact, bindings):
  if pattern is empty:
    return bindings
  
  if pattern[0] is variable:
    if pattern[0] in bindings:
      return match if bindings[pattern[0]] == fact[0], else fail
    else:
      return match(rest(pattern), rest(fact), bindings ∪ {pattern[0]: fact[0]})
  
  if pattern[0] is literal:
    return match if pattern[0] == fact[0], else fail
  
  if pattern[0] is nested pattern:
    return match if fact[0] is vector and match(pattern[0], fact[0], bindings)
```

### Performance Considerations

- Simple patterns (no variables) use hash-based lookup
- Patterns with variables iterate and filter
- Binding consistency checked incrementally
- Cached compiled patterns for repeated matching

## Edge Cases

| Case | Behavior |
|------|----------|
| Pattern longer than fact | No match |
| Pattern shorter than fact | No match |
| Nested vectors at different depths | No recursive match |
| Same var at same position | Always matches |
| Variable binding to nil | Allowed |
| Variable binding to vector | Allowed |
| Empty pattern `[]` | Matches empty fact `[]` |

## Integration with Fact Base

Pattern matching is the primary query mechanism for the fact base:

```clojure
;; From facts spec - query function uses pattern matching
(query fb [:person ?name])
=> ({:?name :alice} {:?name :bob})
```

See [Facts Specification](01-facts.md) for details.
