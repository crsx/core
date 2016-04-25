/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.crsx.plank.sort.ConsForm;
import org.crsx.plank.sort.Sort;
import org.crsx.plank.term.Term;

/**
 * Context for composing and executing a plank script.
 * @author krisrose
 */
public class Context {

	// State.
	
	/** The sorts names with their (sort parameter) rank. */
	private final Map<String, Integer> _sortRank = new HashMap<>();
	
	/** The forms in the system, indexed by constructor name. */
	private final Map<String, ConsForm> _forms = new HashMap<>();
	
	/** The names of sorts that have syntactic variables. */
	private final Set<String> _syntactic = new HashSet<>(); 
	
	/** The rules in the system, indexed by name. */
	private final Map<String, Rule> _rules = new HashMap<>();
	
	/** Any errors accrued by the context. */
	private List<String> _errors = new ArrayList<>();

	// Constructor.
	
	/** Constructor just initializes: use the add* methods to add declarations. */
	public Context() {}

	// Methods.
	
	/** Add constructor declaration of given form. */
	public void addConsDeclaration(ConsForm form) throws PlankException {
		Sort sort = form.sort;
		if (!form.scheme && !sort.isPrimary())
			throw new PlankException("%s: sort of data must be named sort with optional parameters (%s)", sort.origin(), sort.toString());

		String cons = form.cons;

	}

	/** Set the given sort to have syntactic variables. */
	public void addVariableDeclaration(Sort sort) throws PlankException {
		if (!sort.isPrimary())
			throw new PlankException("%s: sort with variables must be named sort with optional parameters (%s)", sort.origin(), sort.toString());
		_syntactic.add(sort.name);
	}
	
	/**
	 * Add a rule.
	 * @param origin identification
	 * @param sort of rule
	 * @param options of rule
	 * @param pattern of rule
	 * @param contractum of rule
	 */
	public void addRule(String origin, Sort sort, Map<String,String> options, Term pattern, Term contractum) throws PlankException {
		// TODO: Check stuff?
		Rule rule = Rule.mk(origin, sort, options, pattern, contractum);
		_rules.put(origin, rule);
	}
	
	/**
	 * Add an error to the context.
	 * @param origin identifying the location of the error
	 * @param format message string in {@link String#format(String, Object...)} form
	 * @param args for message string
	 */
	public void addError(String origin, String format, Object... args) {
		_errors.add(String.format("Error {%s} " + format, origin, args));
	}
	
	// Extract information.
	
	/** Unique constructor form for a specific named constructor, or null if not defined. */ 
	public ConsForm consForm(String cons) {
		return _forms.get(cons);
	}

	/**
	 * Unify the two sorts, updating the sort variable mapping.
	 * @param sort1 first sort
	 * @param sort2 second sort
	 */
	public Sort unify(Sort sort, Sort sort2) {
		// TODO Auto-generated method stub
		return null;
	}
}