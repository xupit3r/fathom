# Unification Specification

## Overview

Unification is the process of finding a substitution that makes two terms identical. It extends pattern matching by allowing bidirectional matching and handling nested structures with variables on both sides.

## Relationship to Pattern Matching

| Feature | Pattern Matching | Unification |
|---------|------------------|-------------|
| Variables | Left side only (pattern) | Both sides |
| Direction | Pattern → fact | Either direction |
| Consistency | Single pattern | Two-way agreement |
| Use case | Querying facts | Rule firing, goal proving |

## Core Functions

### unify

Finds the most general unifier (MGU) of two terms.

```clojure
;; Signature
(unify [term1 term2]) => substitution-or-nil
(unify [term1 term2 substitution]) => substitution-or-nil

;; Examples

;; Simple literals
(unify :alice :alice)
=> {}  ; empty substitution = already identical

(unify :alice :bob)
=> nil  ; cannot unify different literals

;; Variables
(unify ?x :alice)
=> {?x :alice}

(unify :alice ?x)
=> {?x :alice}

;; Two variables
(unify ?x ?y)
=> {?x ?y}  ; either maps to the other

;; Complex terms
(unify [:person ?name] [:person :alice])
=> {:?name :alice}

(unify [:likes ?x ?y] [:likes :alice ?y])
=> {:?x :alice}

;; Nested unification
(unify [:father ?x [:child ?y]] [:father :bob ?child])
=> {:?x :bob :?child [:child ?y] :?y ?child}
```

**Returns:**
- `nil` if unification fails
- Empty map `{}` if terms are identical (no variables)
- Map of bindings if variables were bound

### unify*

Extended unify that handles multiple terms.

```clojure
;; Signature
(unify* [& terms]) => substitution-or-nil

;; Examples

;; Unify multiple terms
(unify* ?x ?y :alice)
=> {?x :alice :?y :alice}

;; Unify a pattern with multiple facts
(unify* [:p ?x] [:p :a] [:p ?y])
=> {?x :a :?y :a}
```

## Substitution Operations

### apply-subst

Applies a substitution to a term.

```clojure
;; Signature
(apply-subst [term substitution]) => term

;; Examples

(apply-subst ?x {?x :alice})
=> :alice

(apply-subst [:person ?name] {?name :alice})
=> [:person :alice]

(apply-subst [:likes ?x ?x] {?x :bob})
=> [:likes :bob :bob]

;; Nested application
(apply-subst [:father ?x [:child ?y]] {?x :bob, ?y :alice})
=> [:father :bob [:child :alice]]
```

### compose-subst

Composes two substitutions.

```clojure
;; Signature
(compose-subst [s1 s2]) => substitution

;; Composition: apply s1 then s2

;; Examples

;; Simple composition
(compose-subst {?x :alice} {?x :bob})
=> {?x :bob}  ; s2 overrides s1

;; Adding new bindings
(compose-subst {?x :alice} {?y :bob})
=> {?x :alice :?y :bob}

;; Variable to variable
(compose-subst {?x ?y} {?y :alice})
=> {?x :alice :?y :alice}
```

### subst?

Checks if a value is a substitution (map with variable keys).

```clojure
;; Signature
(subst? [x]) => boolean

;; Examples
(subst? {?x :alice}) => true
(subst? {}) => true
(subst? :alice) => false
(subst? [?x :alice]) => false
```

## Occurs Check

The occurs check prevents unification that would create infinite structures.

### occurs?

Checks if a variable occurs in a term.

```clojure
;; Signature
(occurs? [var term]) => boolean
(occurs? [var term substitution]) => boolean

;; Examples

(occurs? ?x :alice)
=> false

(occurs? ?x ?x)
=> true

(occurs? ?x [:person ?x])
=> true

(occurs? ?x [:likes ?x ?y])
=> true
```

### Unification with Occurs Check

```clojure
;; This should fail - ?x would contain itself
(unify ?x [:list ?x])
=> nil  ; occurs check prevents infinite structure

;; This succeeds - no cycle
(unify [:list ?x] [:list :alice])
=> {?x :alice}
```

## Algorithm

### Unify Algorithm

```
function unify(t1, t2, subst={}):
  t1 = apply-subst(t1, subst)
  t2 = apply-subst(t2, subst)
  
  if t1 == t2:
    return subst
  
  if t1 is variable:
    if occurs?(t1, t2, subst):
      return nil
    return compose-subst(subst, {t1: t2})
  
  if t2 is variable:
    if occurs?(t2, t1, subst):
      return nil
    return compose-subst(subst, {t2: t1})
  
  if t1 and t2 are compound terms (vectors):
    if first(t1) != first(t2):
      return nil
    return unify(args(t1), args(t2), subst)
  
  if t1 and t2 are lists:
    if empty?(t1) and empty?(t2):
      return subst
    return unify(rest(t1), rest(t2), unify(first(t1), first(t2), subst))
  
  return nil  ; cannot unify
```

### Most General Unifier (MGU)

The unification algorithm produces the MGU - a substitution that:
1. Makes terms equal
2. Contains no unnecessary bindings
3. Is unique up to variable renaming

## Comparison with Pattern Matching

### Pattern Matching

```clojure
;; Pattern on left, fact on right
(match? [:likes ?x ?y] [:likes :alice :bob])
=> {:?x :alice :?y :bob}
```

### Unification

```clojure
;; Bidirectional - either can have variables
(unify [:likes ?x ?y] [:likes :alice ?y])
=> {:?x :alice}

;; Both can have variables
(unify [:likes ?x ?y] [:likes ?w ?z])
=> {:?x ?w :?y ?z}
```

## Use Cases

### Rule Firing

When a rule fires, we unify the rule's antecedent with matched facts:

```clojure
;; Rule: [:if [:likes ?x ?y] :then [:likes ?y ?x]]
;; Fact: [:likes :alice :bob]

(unify [:likes ?x ?y] [:likes :alice :bob])
=> {?x :alice :?y :bob}

;; Apply to consequent: [:likes ?y ?x] => [:likes :bob :alice]
```

### Goal Proving

Backward chaining uses unification to find facts matching goals:

```clojure
;; Goal: [:likes ?who :alice]
;; Fact: [:likes :bob :alice]

(unify [:likes ?who :alice] [:likes :bob :alice])
=> {?who :bob}
```

## Edge Cases

| Case | Behavior |
|------|----------|
| Same variable both sides | Binds to shared identity |
| Different literals | Fail |
| Compound vs atomic | Fail |
| Different relation names | Fail |
| Different arity | Fail |
| Occurs check failure | Return nil |
| Empty structures | Succeed |

## Implementation Requirements

1. **Occurs Check**: Must always be performed
2. **MGU Property**: Return most general unifier
3. **Idempotence**: `unify(t, t, s) = s`
4. **Composability**: `unify(t1, t2, s) = compose-subst(s, unify(apply-subst(t1,s), apply-subst(t2,s)))`

## Performance Considerations

- Cache substitution compositions
- Destructuring can avoid repeated first/rest
- Stack depth management for nested terms
- Early termination on obvious failures
