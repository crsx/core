/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.loader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.crsx.plank.base.Origined;
import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.sort.Sort;
import org.crsx.plank.term.Cons;
import org.crsx.plank.term.Term;

/**
 * Struct describing a rule.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class Rule extends Origined {

	/**
	 * Create a rule.
	 * @param origin of rule
	 * @param sort of rule
	 * @param options to rule
	 * @param pattern of rule
	 * @param contractum of rule
	 */
	public static Rule mk(String origin, Sort sort, Map<String, String> options, Cons pattern, Term contractum) {
		Priority priority = Priority.STANDARD;
		if (options.containsKey("priority")) {
			switch (options.get("priority")) {
			case "default" : priority = Priority.LOW; break;
			case "priority" : priority = Priority.HIGH; break;
			}
		}
		return new Rule(origin, sort, priority, pattern, contractum);
	}

	/**
	 * Rule priority.
	 * @author Kristoffer H. Rose <krisrose@crsx.org>
	 */
	public enum Priority {
		LOW, STANDARD, HIGH;
	}

	// State.

	/** The sort of the rule. */
	public final Sort sort;

	/** The priority of the rule. */
	public final Priority priority;
	
	/** The pattern of the rule.*/
	public final Cons pattern;
	
	/** The contraction of the rule. */
	public final Term contractum; 
	
	// Constructor.
	
	/** Instantiate. */
	private Rule(String origin, Sort sort, Priority priority, Cons pattern, Term contractum) {
		super(origin);
		this.sort = sort;
		this.priority = priority;
		this.pattern = pattern;
		this.contractum = contractum;
	}
	
	// Methods.
	
	/**
	 * Show rule in textual form.
	 * @param out target of text
	 * @param includeSorts whether rule should include sorts
	 * @throws PlankException if out fais or there is something wrong with the term
	 */
	public void appendRule(Appendable out, boolean includeSorts) throws PlankException {
		try {
			Map<Var, String> namings = new HashMap<>();
			// TODO: opts.
			sort.appendSort(out, namings);
			out.append(" rule ");
			pattern.appendTerm(out, "\n    ", namings, includeSorts);
			out.append("\n  →");
			contractum.appendTerm(out, "\n    ", namings, includeSorts);
			out.append("\n;\n");
		} catch (IOException ioe) {
			throw new PlankException(ioe);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			appendRule(sb, false);
		} catch (PlankException e) {
			sb.append("**BADSORT(" + e.getMessage() + ")**");
		}
		return sb.toString();
	}
}