/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.sort;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.crsx.plank.base.Origined;
import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;

/**
 * Struct describing the form of a construction.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class ConsForm extends Origined {

	/**
	 * Create constructor form (the name and argument shapes of a construction).
	 * <p>
	 * @param origin of form
	 * @param sort that form is declared to be
	 * @param name name of constructor
	 * @param subSort sorts of the terms in the argument scopes, in order
	 * @param binderSort sorts of the binders of each argument scope, in order
	 * @param keySort sorts of the association key variables, in order
	 * @param valueSort sorts of the association values, in order
	 * @param assocRealIndex the indices of the associations in the original term
	 * @param scheme whether the construction is a defined symbol
	 */
	public static ConsForm mk(String origin, Sort sort, String name, Sort[] subSort, Sort[][] binderSort, Sort[] keySort, Sort[] valueSort, int[] assocRealIndex, boolean scheme) {
		return new ConsForm(origin, sort, name, subSort, binderSort, keySort, valueSort, assocRealIndex, scheme);
	}

	// State
	/** Sort that the construction form belongs to. */
	public final Sort sort;

	/** The name of the constructor. */
	public final String name;

	/** The sorts of all scope subterms, in order. */
	public final Sort[] subSort;

	/** The binders of the scope subterms, in order. */
	public final Sort[][] binderSort;

	/** The key sorts of the association subpieces, in order. */
	public final Sort[] keySort;

	/** The value sorts of the association subpieces, in order. */
	public final Sort[] valueSort;

	/** Real index in the original form of each association. */
	public final int[] assocRealIndex;
	
	/** Whether this is a defined symbol. */
	public final boolean scheme;
	
	/** Actual instantiation. */
	private ConsForm(String origin, Sort sort, String name, Sort[] subSort, Sort[][] binderSort, Sort[] keySort, Sort[] valueSort, int[] assocRealIndex, boolean scheme) {
		super(origin);
		this.sort = sort;
		assert sort != null;
		this.name = name;
		assert sort != null;
		this.subSort = subSort == null || subSort.length == 0 ? NO_SORT : subSort;
		this.binderSort = binderSort == null || binderSort.length == 0 ? NO_BINDER_SORT : binderSort;
		this.keySort = keySort == null || keySort.length == 0 ? NO_SORT : keySort;
		this.valueSort = valueSort == null || valueSort.length == 0 ? NO_SORT : valueSort;
		this.assocRealIndex = assocRealIndex;
		this.scheme = scheme;
		assert subSort.length == binderSort.length : "Panic: constructor form with inconsistent binders and subterms?";
		assert keySort.length == valueSort.length : "Panic: constructor form with inconsistent key and value sorts?";
	}
	private static Sort[] NO_SORT = new Sort[0];
	private static Sort[][] NO_BINDER_SORT = new Sort[0][];
	
	// Methods.
	
	/**
	 * Output plank declaration corresponding to this form.
	 * @param out target for the output text
	 * @throws PlankException when the construction form cannot be printed
	 */
	public void appendConsForm(Appendable out) throws PlankException {
		try {
			Map<Var, String> namings = new HashMap<>();
			sort.appendSort(out, namings);
			out.append(scheme ? " scheme " : " data ");
			// Constructor.
			out.append(name);
			final int subCount = subSort.length;
			final int assocCount = keySort.length;
			if (subCount + assocCount > 0) {
				String sep = "(";
				// Scopes.
				for (int i = 0; i < subCount; ++i) {
					out.append(sep);
					subSort[i].appendSort(out, namings);
					sep = ", ";
				}
				// Associations.
				for (int i = 0; i < assocCount; ++i) {
					out.append(sep);
					out.append("{");
					keySort[i].appendSort(out, namings);
					out.append(":");
					valueSort[i].appendSort(out, namings);
					out.append("}");
					sep = ", ";
				}
				out.append(")");
			}
			out.append(";\n");
		} catch (IOException e) {
			throw new PlankException(e);
		}
	}
	
	// Object...

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ConsForm))
			return false;
		ConsForm that = (ConsForm) obj;
		return 
				sort.equals(that.sort)
				&& name.equals(that.name)
				&& Arrays.equals(subSort, that.subSort)
				&& array2Equals(binderSort, that.binderSort)
				&& Arrays.equals(keySort, that.keySort)
				&& Arrays.equals(valueSort, that.valueSort)
				&& Arrays.equals(assocRealIndex, that.assocRealIndex)
				&& scheme == that.scheme;
	}
	private static <T> boolean array2Equals(T[][] a1, T[][] a2) {
		final int length = a1.length;
		if (length != a2.length)
			return false;
		for (int i = 0; i < length; ++i)
			if (!Arrays.equals(a1[i], a2[i]))
				return false;
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			appendConsForm(sb);
		} catch (PlankException e) {
			sb.append("**** BAD CONSTRUCTOR DECLARATION (" + e.getMessage() + ") ****");
		}
		return sb.toString();
	}
}