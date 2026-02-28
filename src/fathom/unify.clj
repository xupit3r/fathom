(ns fathom.unify
  "Unification algorithm")

(defn is-var? [term]
  (and (symbol? term)
       (= \? (first (name term)))))

(defn occurs?
  "Check if variable occurs in term"
  ([var term]
   (occurs? var term {}))
  ([var term subst]
   (let [term (get subst term term)]
     (cond
       (= var term) true
       (sequential? term) (some #(occurs? var % subst) term)
       :else false))))

(defn unify
  "Unify two terms"
  ([t1 t2]
   (unify t1 t2 {}))
  ([t1 t2 subst]
   nil))

(defn unify*
  "Unify multiple terms"
  [& terms]
  nil)

(defn apply-subst
  "Apply substitution to term"
  [term subst]
  (get subst term term))

(defn compose-subst
  "Compose substitutions"
  [s1 s2]
  (merge s1 s2))

(defn subst?
  "Check if value is a substitution"
  [x]
  (map? x))
