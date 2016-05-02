/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.crsx.plank.base.Origined;
import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.execute.Executable;
import org.crsx.plank.sort.ConsForm;
import org.crsx.plank.sort.Sort;
import org.crsx.plank.term.Assoc;
import org.crsx.plank.term.Cons;
import org.crsx.plank.term.Meta;
import org.crsx.plank.term.Occur;
import org.crsx.plank.term.Term;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Loading and holding a plank script with rich information.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class Loader {

	// State.
	
	/** The sort names with their (sort parameter) rank. Updated by {@link #addSort(Sort)}. */
	private final Map<String, Integer> _sortRank = new HashMap<>();

	/** All substitutions: variables that are equivalent all map to their representative. Updated by {@link #unify(Sort, Sort)}. */
	private final Map<Var, Var> _unifyEquiv = new HashMap<>();

	/**
	 * Mapping of all sort variables to their instantiation, if any.
	 * All <em>keys</em> are always "representatives" in the sense of {@link #_unifyEquiv}.
	 *  Updated by {@link #unify(Sort, Sort)}.
	 */
	private final Map<Var, Sort> _unifySortConstraint = new HashMap<>();
	
	/** The forms in the system, indexed by constructor name. Updated by {@link #addConsDeclaration(ConsForm)}. */
	private final Map<String, ConsForm> _consForms = new HashMap<>();
	
	/** The names of sorts that have syntactic variables. Updated by {@link #addVariableDeclaration(Sort)}. */
	private final Set<String> _syntactic = new HashSet<>(); 
	
	/** The rules in the system, indexed by origin. Updated by {@link #addRule(String, Sort, Map, Term, Term)}. */
	private final Map<String, Rule> _rules = new HashMap<>();
	
	/** Any errors added to the context. Updated by {@link #addError(String, String, Object...)}. */
	private final List<String> _errors = new ArrayList<>();

	// Constructor.
	
	/**
	 * Constructor just initializes: use the add* methods to add declarations.
	 */
	public Loader() {}

	// Build methods.
	
	/**
	 * Add the sort.
	 * @param sort to record the existence of
	 * @throws PlankException in case there is an inconsistency between this sort and prior sorts
	 */
	public void addSort(Sort sort) throws PlankException {
		if (sort.isVar()) {
			// For variables we currently do nothing: undefined = unbound.
		} else {
			if (!_sortRank.containsKey(sort.name))
				_sortRank.put(sort.name,  sort.param.length);
			else if (sort.param.length != _sortRank.get(sort.name)) {
				throw new PlankException("this sort instance has %d parameters but the sort was previously used with %d parameters",
						sort.param.length, _sortRank.get(sort.name)); 
			}
			for (Sort p : sort.param)
				addSort(p);
		}
	}

	/**
	 * Unify the two sorts, updating the sort variable mappings.
	 * @param sort1 first sort
	 * @param sort2 second sort
	 * @return the unified sort
	 * @throws PlankException if unification fails with a conflict
	 */
	public Sort unify(Sort sort1, Sort sort2) throws PlankException {
		
		// Advance incoming sort variables to their representative.
		if (sort1.isVar() && _unifyEquiv.containsKey(sort1.var))
			sort1 = Sort.mkSortVar(sort1.origin(), unifyRepresentative(sort1.var));
		if (sort2.isVar() && _unifyEquiv.containsKey(sort2.var))
			sort2 = Sort.mkSortVar(sort2.origin(), unifyRepresentative(sort2.var));
		
		if (sort1.isVar() && sort2.isVar()) {

			// CASE 0: We have the same variable twice, escape!
			if (sort1.var == sort2.var)
				return sort1;
			
			// CASE 1: If we have two (now representative) variables. Make them equivalent, return the representative.
			Var var1 = sort1.var;
			Var var2 = sort2.var;
			if (var1.compareTo(var2) > 0) {
				Var v = var1; var1 = var2; var2 = v; // swap so var1 is smallest
				Sort s = sort1; sort1 = sort2; sort2 = s; // swap sorts correspondingly
			}
			// Now replace var2 with var1.
			_unifyEquiv.put(var2,  var1); // so future unifications get it
			// Update constraints.
			if (_unifySortConstraint.containsKey(var2)) {
				if (_unifySortConstraint.containsKey(var1)) {
					// Have constraints for both. The replacement constraints are obtained by unification.
					Sort sort = unify(_unifySortConstraint.get(var1), _unifySortConstraint.get(var2));
					_unifySortConstraint.put(var1, sort);
				} else {
					// Only have constraints for var2. Just move constraint for var2 to var1.
					Sort sort = _unifySortConstraint.get(var2);
					_unifySortConstraint.put(var1,  sort);
				}
				_unifySortConstraint.remove(var2);
			}
			return sort1;  // result is the representative variable (that now has the unified constraint)
		}
		assert !sort1.isVar() || !sort2.isVar() : "Panic: variable sort pair slipped through..."; 
		
		// Expand the at most one already known representative variable to its sort constraints. 
		Var originalVar1 = null;
		Var originalVar2 = null;
		if (sort1.isVar() && _unifySortConstraint.containsKey(sort1.var)) 
			sort1 = _unifySortConstraint.get(originalVar1 = sort1.var);
		if (sort2.isVar() && _unifySortConstraint.containsKey(sort2.var)) 
			sort2 = _unifySortConstraint.get(originalVar2 = sort2.var);
		
		// CASE 2: We have two instances. Unify as terms.
		if (!sort1.isVar() && !sort2.isVar()) {
			if (!sort1.name.equals(sort2.name))
				throw new PlankException("cannot unify sorts %s and %s", sort1.name, sort2.name);
			// Got identical sorts! Compute unified sort by recursing over the sort parameters.
			final int rank = sort1.param.length;
			final Sort[] param = new Sort[rank];
			for (int i = 0; i < rank; ++i)
				param[i] = unify(sort1.param[i], sort2.param[i]);
			Sort sort = Sort.mkSortInstance(Origined.combine("Unified between %s and %s", sort1.origin(), sort2.origin()), sort1.name, param);
			sort = unifySubstituteRepresentative(sort);
			// If one of the constraints were expanded from a variable, update the constraint.
			if (originalVar1 != null) {
				_unifySortConstraint.put(originalVar1, sort);
				return sort1; // result is expanded variable sort
			}
			if (originalVar2 != null) {
				_unifySortConstraint.put(originalVar2, sort);
				return sort2; // result is expanded variable sort
			}
			return sort; // no variables were expanded so result is the unified constraint
		}
		assert originalVar1 == null && originalVar2 == null : "Panic: With one variable and an instance, the instance cannot be expanded....";

		// CASE 3: We have one unconstrained sort variable and one instance.
		// The instance was not expanded from a variable because then this would be the first case.
		// Map the unconstrained variable to the instance, and return the variable!
		if (sort2.isVar()) { // flip to make sort1 the variable sort
			Sort s = sort1; sort1 = sort2; sort2 = s;
		}
		assert !sort2.isVar() && originalVar1 == null && originalVar2 == null : "Panic: variable escaped through to mixed case.";
		_unifySortConstraint.put(sort1.var, sort2); // constrain the variable
		return sort1; // result is variable sort to catch more equivalences
	}
	
	/** Find representative variable (by repeated application of {@link #_unifyEquiv}). */
	private Var unifyRepresentative(Var var) {
		while (_unifyEquiv.containsKey(var))
			var = _unifyEquiv.get(var);
		return var;
	}
	
	/**
	 * Replace all variables with the equivalence class representative in sort.
	 * Used on created unification sorts to amortize equivalence class computation. 
	 */
	private Sort unifySubstituteRepresentative(Sort sort) {
		// TODO: optimize this to reuse var and primary sorts when no change...?
		if (sort.isVar()) {
			return Sort.mkSortVar(sort.origin(), unifyRepresentative(sort.var));
		} else { // !sort.isVar()
			final int size = sort.param.length;
			Sort[] param = new Sort[size];
			for (int i = 0; i < size; ++i) {
				param[i] = unifySubstituteRepresentative(sort.param[i]);
			}
			return Sort.mkSortInstance(sort.origin(), sort.name, param);
		}
	}
	
	/** Expand all sort constraints, leaving only unconstrained sort variables. */
	private Sort expandSort(Sort sort) {
		if (sort == null) {
			return null; // so it can be used on partial records
		} else if (sort.isVar()) {
			Var rep = unifyRepresentative(sort.var);
			if (_unifySortConstraint.containsKey(rep)) {
				sort = _unifySortConstraint.get(rep);
				return expandSort(sort); // tail recursive!
			}
			return Sort.mkSortVar(sort.origin(), rep);
		} else {
			final int rank = sort.param.length;
			Sort[] param = new Sort[rank]; 
			for (int i = 0; i < rank; ++i) {
				param[i] = expandSort(sort.param[i]);
			}
			return Sort.mkSortInstance(sort.origin(), sort.name, param);
		}
	}

	/** Expand all sorts in a constructor form. */
	private ConsForm expandConsForm(ConsForm form) {
		final int subCount = form.subSort.length;
		Sort[] newSubSort = new Sort[subCount];
		Sort[][] newBinderSort = new Sort[subCount][];
		for (int i = 0; i < subCount; ++i) {
			newSubSort[i] = expandSort(form.subSort[i]);
			final int rank = form.binderSort[i].length;
			Sort[] subBinderSort = new Sort[rank];
			for (int j = 0; j < rank; ++j)
				subBinderSort[j] = expandSort(form.binderSort[i][j]);
			newBinderSort[i] = subBinderSort;
		}
		final int assocCount = form.keySort.length;
		Sort[] newKeySort = new Sort[assocCount];
		Sort[] newValueSort = new Sort[assocCount];
		for (int i = 0; i < assocCount; ++i) {
			newKeySort[i] = expandSort(form.keySort[i]);
			newValueSort[i] = expandSort(form.valueSort[i]);
		}
		return ConsForm.mk(form.origin(), expandSort(form.sort), form.name, newSubSort, newBinderSort, newKeySort, newValueSort, form.assocRealIndex, form.scheme);
	}
	
	/** Expand all sorts in a term to incorporate all constraints. */
	public Term expandTerm(Term term) {
		switch (term.kind()) {
		case CONS : {
			Cons cons = term.cons();
			final int subCount = cons.sub.length;
			Term[] newSub = new Term[subCount];
			for (int i = 0; i < subCount; ++i)
				newSub[i] = expandTerm(cons.sub[i]);
			final int assocCount = cons.assoc.length;
			Assoc[] newAssoc = new Assoc[assocCount];
			for (int i = 0; i <assocCount; ++i)
				newAssoc[i] = expandAssoc(cons.assoc[i]);
			return Term.mkCons(cons.origin(), expandSort(cons.sort()), expandConsForm(cons.form), cons.binder, newSub, newAssoc);
		}
		case META : {
			Meta meta = term.meta();
			List<Term> subs = new ArrayList<Term>();
			for (Term t : meta.sub)
				subs.add(expandTerm(t));
			return Term.mkMeta(meta.origin(), expandSort(meta.sort()), meta.name, subs);
		}
		case OCCUR : {
			Occur occur = term.occur();
			return Term.mkOccur(occur.origin(), expandSort(occur.sort()), occur.var);
		}
		}
		return null; // unreachable
	}

	/** Create a new association where all sorts have been expanded. */
	private Assoc expandAssoc(Assoc assoc) {
		Sort newKeySort = expandSort(assoc.keySort);
		Sort newValueSort = expandSort(assoc.valueSort);
		Map<Var, Term> newMap = new HashMap<>();
		for (Var key : assoc.map.keySet())
			newMap.put(key, expandTerm(assoc.map.get(key)));
		return Assoc.mk(assoc.origin(), assoc.realIndex, newKeySort, newValueSort, newMap, assoc.omit, Arrays.asList(assoc.all));
	}

	/** Add constructor declaration of given form. */
	public void addConsDeclaration(ConsForm form) throws PlankException {
		if (!form.scheme && !form.sort.isPrimary())
			throw new PlankException("sort of data constructor %s must be named sort with optional parameters (%s)", form.name, form.sort.toString());
		if (_consForms.containsKey(form.name))
			throw new PlankException("duplicate declaration of constructor %s", form.name);
		_consForms.put(form.name, expandConsForm(form));
	}

	/** Set the given sort to have syntactic variables. */
	public void addVariableDeclaration(Sort sort) throws PlankException {
		if (!sort.isPrimary())
			throw new PlankException("%s: sort with variables must be named sort with optional parameters (%s)", sort.origin(), sort.toString());
		if (_syntactic.contains(sort.name))
			throw new PlankException("duplicate variable declaration for sort %s", sort.name);
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
	public void addRule(String origin, Sort sort, Map<String,String> options, Cons pattern, Term contractum) throws PlankException {
		if (_rules.containsKey(origin))
			throw new PlankException("duplicate rules registered from same place? (%s)", origin);
		Rule rule = Rule.mk(origin, expandSort(sort), options, (Cons) expandTerm(pattern), expandTerm(contractum));
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
	
	// Extraction methods.
	
	/** Get unique constructor form for a specific named constructor, or null if not defined. */ 
	public ConsForm consForm(String cons) {
		return _consForms.get(cons);
	}
	
	/** Whether the loader has any errors registered with it. */
	public boolean hasErrors() {
		return !_errors.isEmpty();
	}

	/** Append the actual errors to an output. */
	public void appendErrors(Appendable out) throws IOException {
		for (String err : _errors)
			out.append(err + "\n");
	}
	
	/** Extract an execution context. */
	public Executable executable() {
		return new Executable(Collections.unmodifiableMap(_rules));
	}
	
	/** Extract textual form. */
	public void append(Appendable out) throws PlankException {
		try {
			// Group all constructor declarations and rules by sort.
			ListMultimap<String, ConsForm> formsBySort= LinkedListMultimap.create();
			ListMultimap<String, String> sortsByName = LinkedListMultimap.create();
			for (String cons : _consForms.keySet()) {
				ConsForm form = _consForms.get(cons); 
				Sort sort = _consForms.get(cons).sort; 
				formsBySort.put(sort.toString(), form);
				String sortName = sort.name != null ? sort.name : "a";
				String sortText = sort.toString();
				if (!sortsByName.containsEntry(sortName, sortText))
					sortsByName.put(sortName, sortText);
			}
			// Print all the groups!
			
			for (String name : sortsByName.keySet()) {
				List<String> sorts = sortsByName.get(name);
				if (sorts.isEmpty())
					appendNoCaseSort(out, name);
				else {
					boolean checkSyntactic = true;
					for (String sort : sorts) {
						List<ConsForm> forms = formsBySort.get(sort);
						for (ConsForm form : forms) {
							if (checkSyntactic && form.sort.isPrimary() && _syntactic.contains(name)) {
								out.append(sort + " variable;\n");
								checkSyntactic = false;
							}
							form.appendConsForm(out);
						}
						out.append("\n");
					}
					if (checkSyntactic && _syntactic.contains(name))
						appendNoCaseSort(out, name); // in case we found no primary sort dump separately
				}
			}
			
			// Print the rules!
			for (String o : _rules.keySet()) {
				Rule rule = _rules.get(o);
				rule.appendRule(out, false);
			}
			
		} catch (IOException ioe) {
			throw new PlankException(ioe);
		}
	}

	/** Helper to append a pseudo-rule for an otherwise undefined sort, except for variables perhaps. */
	private void appendNoCaseSort(Appendable out, String name) throws IOException {
		if (!_syntactic.contains(name))
			out.append("/* "); // undefined sort shown as comment.
		out.append(name);
		final int rank = _sortRank.get(name);
		if (rank > 0) {
			String sep = "<";
			for (int i = 0; i < rank; ++i) {
				out.append(sep);
				out.append("s"+(i+1));
				sep = ", ";
			}
			out.append(">");
		}
		if (_syntactic.contains(name)) {
			out.append(" variable;\n\n"); // show just variable declaration 
		} else {
			out.append(" */\n\n");
		}
	}

	// Object...

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			append(sb);
		} catch (PlankException e) {
			sb.append("**** BAD LOADER (" + e.getMessage() + ") ****");
		}
		return sb.toString();
	}
} 