/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.script;

import java.util.Map;

import org.crsx.plank.base.Origined;
import org.crsx.plank.sort.Sort;
import org.crsx.plank.term.Term;

/**
 * Struct describing a rule.
 * @author krisrose
 */
public class Rule extends Origined {

	/**
	 * Create a rule.
	 * @param origin of rule
	 * @param sort of rule
	 * @param options to rule
	 * @param pattern of rule
	 * @param contractum of rule
	 */
	public static Rule mk(String origin, Sort sort, Map<String, String> options, Term pattern, Term contractum) {
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
	 * @author krisrose
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
	public final Term pattern;
	
	/** The contraction of the rule. */
	public final Term contractum; 
	
	/** Instantiate. */
	private Rule(String origin, Sort sort, Priority priority, Term pattern, Term contractum) {
		super(origin);
		this.sort = sort;
		this.priority = priority;
		this.pattern = pattern;
		this.contractum = contractum;
	}
}