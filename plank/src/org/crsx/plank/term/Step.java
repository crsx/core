/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.term.Term.Kind;

/**
 * Simple record helper class to represent a step.
 * 
 * The members are either
 * <ul>
 * <li> move from construction to the term in one of its scopes
 * <li> move from construction to the term for a particular variable in one of its associations
 * </ul>
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class Step implements Comparable<Step> {
	
	// State.
	
	/** The index into either {@link Cons#sub} or {@link Cons#assoc}. */
	final int index;
	
	/** For scope step, null, for association step, the key. */
	final Var key;
	
	// Constructor.
	
	/**Instance. */
	Step(int index, Var key) {
		super();
		this.index = index;
		this.key = key;
	}
	
	// Methods.
	
	/** Helper to apply a single explicit step. */ 
	public Term apply(Term term) throws PlankException {
		if (term.kind() != Kind.CONS)
			throw new PlankException("cannot navigate into non-construction");
		Cons cons = term.cons();
		return key == null ? cons.sub[index] : cons.assoc[index].map.get(key);
	}

	/**
	 * Get first (leftmost) step in term.
	 * Note: relies on stable Maps...
	 * @param term the new step must be valid in this term
	 * @return the first step, or null if there are none
	 */
	public static Step first(Term term) {
		if (term.kind() == Kind.CONS) {
			Cons cons = term.cons();
			if (cons.sub.length > 0)
				return new Step(0	, null);
			else {
				for (Assoc assoc : cons.assoc) {
					for (Var var : assoc.map.keySet()) {
						return new Step(0, var);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Get "next" step (to the right of this one) in term.
	 * Note: relies on stable Maps...
	 * @param term the new step must be valid in this term
	 * @return the new step, or null if there are no more
	 */
	public Step next(Term term) {
		if (term.kind() == Kind.CONS) {
			int n = index; // current index, modify as needed
			Var k = key; // current key, modify as needed
			Cons cons = term.cons();
			if (k == null) {
				// Scope step...
				if (n + 1 < cons.sub.length) {
					return new Step(n + 1, null);
				}
				n = 0;
			}
			if (n < cons.assoc.length) {
				for (int i = n; i < cons.assoc.length; ++i) {
					for (Var var : cons.assoc[i].map.keySet()) {
						if (key == null)
							return new Step(i, var);
						if (k == key)
							k = null; // get the next one!
					}
					k = null;
				}
			}
		}
		return null;
	}

	// Comparable...
	
	@Override
	public int compareTo(Step that) {
		if (key == null)
			return that.key == null ? that.index - index : 0;
		return -1;
	}
	
	// Object...
	
	@Override
	public String toString() {
		return "" + index + (key == null ? "" : key.name);
	}
}