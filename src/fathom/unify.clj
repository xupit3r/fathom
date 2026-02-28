(ns fathom.unify
  "Unification algorithm")

(defn is-var? [term]
  (and (symbol? term)
       (some? (re-find #"^\?" (name term)))))

(defn occurs?
  "Check if variable occurs in term"
  [var term]
  (cond
    (= var term) true
    (sequential? term) (some #(occurs? var %) term)
    :else false))

(defn apply-subst
  "Apply substitution to term"
  [term subst]
  (if (map? subst)
    (if (contains? subst term)
      (recur (get subst term) subst)
      (if (sequential? term)
        (mapv #(apply-subst % subst) term)
        term))
    term))

(defn compose-subst
  "Compose substitutions"
  [s1 s2]
  (let [s2-applied (into {} (map (fn [[k v]] [k (apply-subst v s1)]) s2))]
    (merge s1 s2-applied)))

(defn unify
  "Unify two terms"
  ([t1 t2]
   (unify t1 t2 {}))
  ([t1 t2 subst]
   (let [t1 (apply-subst t1 subst)
         t2 (apply-subst t2 subst)]
     (cond
       (= t1 t2) subst
       (is-var? t1)
       (if (occurs? t1 t2)
         nil
         (compose-subst subst {t1 t2}))
       (is-var? t2)
       (if (occurs? t2 t1)
         nil
         (compose-subst subst {t2 t1}))
       (and (sequential? t1) (sequential? t2))
       (if (= (count t1) (count t2))
         (reduce (fn [s [a b]] (if s (unify a b s) nil))
                 subst
                 (map vector t1 t2))
         nil)
       :else nil))))

(defn unify*
  "Unify multiple terms"
  [& terms]
  (if (empty? terms)
    {}
    (reduce (fn [s [t1 t2]] (if s (unify t1 t2 s) nil))
            {}
            (partition 2 terms))))

(defn subst?
  "Check if value is a substitution"
  [x]
  (map? x))
