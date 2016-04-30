/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.execute;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import org.crsx.plank.loader.Rule;
import org.crsx.plank.term.Term;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;

/**
 * Normalize input terms with the rewrite system.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public class Executable {
	
	// State.

	/** The rules, by pattern constructor. */
	private final ImmutableMultimap<String, Rule> _constructorRules;
	
	// Constructor.
	
	/** Setup execution with the provided rules. */
	public Executable(Map<String, Rule> rules) {
		ImmutableListMultimap.Builder<String, Rule> b = ImmutableListMultimap.builder();
		for (Rule rule : rules.values())
			b.put(rule.pattern.form.name, rule);
		_constructorRules = b.build();
	}
	
	// Methods.
	
	/** Normalize term with rules. The returned term has no common structure with the passed term. */
	public Term normalize(Term term) {
		
		// Start evaluation with just term on stack.
		Deque<Term> stack = new ArrayDeque<>();
		stack.push(term);
		
		// First try to evaluate 
		
		// TODO
		
		return null;
	}

}
