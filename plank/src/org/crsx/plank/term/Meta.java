/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import org.crsx.plank.sort.Sort;

/**
 * A meta-application.
 * @author krisrose
 */
public class Meta extends Term {

	private final Term[] _sub;

	/**
	 * Make a meta-application.
	 * @param origin of construction
	 * @param sort of construction
	 * @param sub the substitution terms
	 */
	Meta(String origin, Sort sort, Term[] sub) {
		super(origin, sort);
		_sub = sub;
	}

	// Methods.

	/**
	 * Get a substitution term.
	 * @param index of the term
	 */
	public Term sub(int index) {
		return _sub[index];
	}
	
	// Term...
	
	@Override
	public Kind kind() {
		return Kind.META;
	}
	
	@Override
	public Meta meta() {
		return this;
	}
}