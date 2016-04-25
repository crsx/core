/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import org.crsx.plank.base.Var;
import org.crsx.plank.sort.Sort;
import org.crsx.plank.sort.ConsForm;

/**
 * A construction.
 * @author krisrose
 */
public class Cons extends Term {

	private final ConsForm _form;
	private final Var[][] _binder;
	private final Term[] _sub;
	private final Assoc[] _assoc;

	/**
	 * Make a construction.
	 * @param origin of construction
	 * @param sort the actual sort (may be an instance of the declared sort)
	 * @param form the constructor form
	 * @param binder the binders to use (assumed immutable)
	 * @param sub the subscopes
	 * @param assoc the associations
	 */
	Cons(String origin, Sort sort, ConsForm form, Var[][] binder, Term[] sub, Assoc[] assoc) {
		super(origin, sort);
		_form = form;
		_binder = binder;
		_sub = sub;
		_assoc = assoc;
	}

	// Methods.

	/** The sort constructor form used for this construction. */
	public ConsForm form() {
		return _form;
	}
	
	/**
	 * Get one of the actually bound variables.
	 * @param index of subscope where the binder should be (0-based)
	 * @param binderIndex index in the list of binders for that subscope (0-based)
	 * @return the variable instance of the binder
	 */
	public Var binder(int index, int binderIndex) {
		return _binder[index][binderIndex];
	}

	/**
	 * Get a term of one of the scopes. 
	 * @param index of the subscope
	 * @return the term contained in the subscope
	 */
	public Term sub(int index) {
		return _sub[index];
	}
	
	/** The index'th association. */
	public Assoc assoc(int index) {
		return _assoc[index];
	}

	// Term...
	
	@Override
	public Kind kind() {
		return Kind.CONS;
	}
	
	@Override
	public Cons cons() {
		return this;
	}
}