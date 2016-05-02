/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.crsx.plank.sort.ConsForm;
import org.crsx.plank.sort.Sort;

/**
 * Helper struct to represent the sort of a scope or association or term.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
final class Form {

	/**
	 * Build piece sort holding a scope description.
	 * @param binders sorts
	 * @param sub sort
	 */
	static Form mkScopeForm(List<Sort> binders, Sort sub) {
		if (binders == null)
			binders = NO_BINDERS;
		Sort[] binder = binders.toArray(new Sort[binders.size()]);
		return new Form(binder, null, sub);
	}

	/** Empty binders list. */
	static List<Sort> NO_BINDERS = new ArrayList<>();
	
	/**
	 * Build piece form holding an association description.
	 * @param key sort
	 * @param value sort
	 */
	static Form mkAssocForm(Sort key, Sort value) {
		return new Form(null, key, value);
	}
	
	/**
	 * Create a ConsForm constructor descriptor from list of forms.
	 * @param origin of this declaration
	 * @param sort sort of the constructor declaration
	 * @param cons name of constructor
	 * @param forms the list of forms of individual arguments, in original argument order
	 * @param scheme whether the declaration should be a scheme (otherwise it is data)
	 */
	static ConsForm mkConsForm(String origin, Sort sort, String cons, List<Form> forms, boolean scheme) {
		final Form[] subForm = forms.stream().filter(f -> !f.isAssoc()).toArray(n -> new Form[n]);
		final Sort[] subSort = Arrays.stream(subForm).map(f -> f.sort).toArray(n -> new Sort[n]);
		final Sort[][] binderSort = Arrays.stream(subForm).map(f -> f.args).toArray(n -> new Sort[n][]);
		final Form[] assocForms = forms.stream().filter(f -> f.isAssoc()).toArray(n -> new Form[n]);
		final Sort[] keySort = Arrays.stream(assocForms).map(f -> f.key).toArray(n -> new Sort[n]);
		final Sort[] valueSort = Arrays.stream(assocForms).map(f -> f.sort).toArray(n -> new Sort[n]);
		int[] assocRealIndex = new int[assocForms.length];
		int realIndex = 0;
		int assocIndex = 0;
		for (Form f : forms) { // access indices so no streams
			if (f.isAssoc()) {
				assocRealIndex[assocIndex] = realIndex;
				++assocIndex;
			}
			++realIndex;
		}
		return ConsForm.mk(origin, sort, cons, subSort, binderSort, keySort, valueSort, assocRealIndex, scheme);
	}

	// State.
	
	/** Sorts of binders if scope sort, or null. */
	final Sort[] args;
	
	/** Sort of the association key, or null. */
	final Sort key;
	
	/** Sort of the term or scope body or association value. */
	final Sort sort;
	
	// Constructor.
	
	/**
	 * Hold sort of piece or subterm.
	 * @param binder sorts of binders if scope, or null
	 * @param key sort of the association key, or null
	 * @param sort of the subterm (or body or value)
	 */
	private Form(Sort[] binder, Sort key, Sort sort) {
		this.args = binder;
		this.key = key;
		this.sort = sort;
	}
	
	// Methods.

	/** Whether the piece is an association form. */
	boolean isAssoc() {
		return key != null;
	}
}