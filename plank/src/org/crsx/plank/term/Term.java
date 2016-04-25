/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import java.util.List;

import org.crsx.plank.base.Origined;
import org.crsx.plank.base.Var;
import org.crsx.plank.sort.ConsForm;
import org.crsx.plank.sort.Sort;

/**
 * Term model.
 * @author krisrose
 */
public abstract class Term extends Origined {

	/**
	 * Create a construction.
	 * @param origin of construction
	 * @param sort the actual sort (may be an instance of the declared sort)
	 * @param form the constructor form
	 * @param binder the binders to use (assumed immutable)
	 * @param sub the subscopes
	 * @param assoc the associations
	 */
	public static Cons mkCons(String origin, Sort sort, ConsForm form, Var[][] binder, Term[] sub, Assoc[] assoc) {
		return new Cons(origin, sort, form, binder, sub, assoc);
	}

	/**
	 * Create a meta-application.
	 * @param origin of construction
	 * @param sort of construction
	 * @param subs the substitution terms
	 */
	public static Meta mkMeta(String origin, Sort sort, List<Term> subs) {
		return new Meta(origin, sort, subs.toArray(new Term[subs.size()]));
	}

	/** Create a variable occurrence. */
	public static Occur mkOccur(String origin, Sort sort, Var var) {
		return new Occur(origin, sort, var);
	}

	// State.
	
	private final Sort _sort;
	
	// Constructor.
	
	/**
	 * Assign sort to the term.
	 * @param origin of term
	 * @param sort of term
	 */
	Term(String origin, Sort sort) {
		super(origin);
		_sort = sort;
	}

	// Methods.

	/** What kind of term is this? */
	public abstract Kind kind();
	
	/** The three kinds of term. */
	public static enum Kind {
		CONS, OCCUR, META
	}

	/** The sort of the term.*/
	public Sort sort() {
		return _sort;
	}
	
	public Cons cons() {
		return null;
	}
	
	public Occur occur() {
		return null;
	}
	
	public Meta meta() {
		return null;
	}
}