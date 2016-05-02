/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.crsx.plank.base.Origined;
import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.sort.Sort;

/**
 * Struct describing an association map piece of a term.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class Assoc extends Origined {

	/**
	 * Create association.
	 * @param origin of association
	 * @param realIndex the real argument index of the association
	 * @param keySort sort of the key variables
	 * @param valueSort sort of the values
	 * @param map concrete maps from variables to terms
	 * @param omit variables that must be non-present for match
	 * @param all catch-all meta-applications
	 */
	public static Assoc mk(String origin, int realIndex, Sort keySort, Sort valueSort, Map<Var, Term> map, Set<Var> omit, List<Meta> all) {
		return new Assoc(origin, realIndex, keySort, valueSort, map, omit, all.toArray(new Meta[all.size()]));
	}
	
	// State.
	
	/** The real argument index of the association (so it prints in the right position). */
	public final int realIndex;
	
	/** Sort of the key variables. */
	public final Sort keySort;
	
	/** Sort of the values. */
	public final Sort valueSort;

	/** Concrete maps from variables to terms. */
	public final Map<Var, Term> map;
	
	/** Variables that must be non-present for match. */
	public final Set<Var> omit;
	
	/** Catch-all meta-applications. */
	public final Meta[] all;
	
	/** Real constructor. */
	private Assoc(String origin, int realIndex, Sort keySort, Sort valueSort, Map<Var, Term> map, Set<Var> omit, Meta[] all) {
		super(origin);
		this.realIndex = realIndex;
		this.keySort = keySort;
		this.valueSort = valueSort;
		this.map = map;
		this.omit = omit;
		this.all = all;
	}

	/**
	 * Append the text of an association.
	 * @param out target for the text
	 * @param prefix to use in embedded terms
	 * @param namings to use for variables
	 * @param includeSorts whether to include sorts in terms
	 * @throws PlankException when the association cannot be printed
	 */
	public void appendAssoc(Appendable out, String prefix, Map<Var, String> namings, boolean includeSorts) throws PlankException {
		try {
			String sep = "{";
			for (Var key : map.keySet()) {
				out.append(sep);
				Occur.appendFreeVar(out, keySort, key, prefix, namings, includeSorts);
				out.append(":");
				map.get(key).appendTerm(out, "", namings, includeSorts);
				sep = ", ";
			}
			out.append("}");
		} catch (IOException ioe) {
			throw new PlankException(ioe);
		}
	}
	
	// Object...

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			appendAssoc(sb, "\n  ", new HashMap<Var, String>(), true);
		} catch (PlankException e) {
			sb.append("**BADASSOC(" + e.getMessage() + ")**");
		}
		return sb.toString();
	}
}