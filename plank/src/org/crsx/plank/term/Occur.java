/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import org.crsx.plank.base.Var;
import org.crsx.plank.sort.Sort;

/**
 * A variable occurrence in a term.
 * @author krisrose
 */
public class Occur extends Term {

	private final Var _var;
	
	/**
	 * Create variable occurrence.
	 * @param origin of variable
	 * @param sort of variable
	 * @param var the actual variable
	 */
	Occur(String origin, Sort sort, Var var) {
		super(origin, sort);
		_var = var;
	}
	
	/** The underlying variable. */
	public Var var() {
		return _var;
	}

	// Term...
	
	@Override
	public Kind kind() {
		return Kind.OCCUR;
	}
	
	@Override
	public Occur occur() {
		return this;
	}
}