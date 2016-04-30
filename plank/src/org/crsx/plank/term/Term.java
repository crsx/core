/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crsx.plank.base.Origined;
import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.sort.ConsForm;
import org.crsx.plank.sort.Sort;

/**
 * Term model.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public abstract class Term extends Origined {

	/**
	 * Create a construction.
	 * @param origin of construction
	 * @param sort the actual sort (may be an instance of the declared sort)
	 * @param form the constructor form
	 * @param binder the binders to use (assumed immutable)
	 * @param sub the subscopes
	 * @param assoc the associations
	 */
	public static Cons mkCons(String origin, Sort sort, ConsForm form, Var[][] binder, Term[] sub, Assoc[] assoc) {
		return new Cons(origin, sort, form, binder, sub, assoc);
	}

	/**
	 * Create a meta-application.
	 * @param origin of construction
	 * @param sort of construction
	 * @param name of the meta-variable
	 * @param subs the substitution terms
	 */
	public static Meta mkMeta(String origin, Sort sort, String name, List<Term> subs) {
		return new Meta(origin, sort, name, subs.toArray(new Term[subs.size()]));
	}

	/**
	 * Create a variable occurrence.
	 * @param origin of the occurrence
	 * @param sort of the variable
	 * @param var the variable
	 */
	public static Occur mkOccur(String origin, Sort sort, Var var) {
		return new Occur(origin, sort, var);
	}

	/**
	 * Create event-based term builder.
	 * Note: cannot create meta-terms.
	 * @return builder, which is also a sink ready for first event of term to build
	 */
	public static TermBuilder builder() {
		return new TermBuilder();
	}

	/** Create an empty path. */
	static TermPath path() {
		return new TermPath();
	}

	// State.
	
	/** The sort of the term. */
	private final Sort _sort;
	
	// Constructor.
	
	/**
	 * Assign sort to the term.
	 * @param origin of term
	 * @param sort of term
	 */
	Term(String origin, Sort sort) {
		super(origin);
		_sort = sort;
	}

	// Methods.

	/** What kind of term is this? */
	public abstract Kind kind();
	
	/** The three kinds of term. */
	public static enum Kind {
		CONS, OCCUR, META
	}

	/** The sort of the term.*/
	public Sort sort() {
		return _sort;
	}
	
	/** Return construction term when {@link #kind()} is {@link Kind#CONS}, otherwise null. */
	public Cons cons() {
		return null;
	}
	
	/** Return variable occurrence term when {@link #kind()} is {@link Kind#OCCUR}, otherwise null. */
	public Occur occur() {
		return null;
	}
	
	/** Return met-application term when {@link #kind()} is {@link Kind#META}, otherwise null. */
	public Meta meta() {
		return null;
	}
	
	/**
	 * Use this term as a pattern and match it against a (potential) redex.
	 * Afterwards the result is available in the returned match's {@link Match#success} field.
	 * If the match fails then the reason for failure is indicated as follows: 
	 * <ul>
	 * <li> if {@link Match#alwaysFail} is true then there is an inherent conflict that cannot be solved;
	 * <li> if {@link Match#variableFail} is true then a variable blocks evaluation, and substituting it may progress things;
	 * <li> otherwise, the problamtic term may be reducible. 
	 * </ul>
	 * In each case, {@link Match#failurePath} contains a {@link TermPath} to the point of failure.
	 *  
	 * @param redex to match against
	 */
	final Match match(Term redex) {
		return Match.match(origin(), this, redex, false); 
	}
	
	/**
	 * Use this term a the contraction of a rewrite.
	 * If match was constructed from pattern and this term is contraction, then this mimics pattern→contraction.
	 * @param sink to send the rewrite result to
	 * @param match previously constructed from pattern and redex
	 * @return the sink to use for subsequent events after rewrite result has been received
	 * @throws PlankException if an inconsistency is discovered or the sink fails
	 */
	final Sink rewrite(Sink sink, Match match) throws PlankException {
		return rewriteTerm(sink, match, new HashMap<Var, Var>());
	}

	/**
	 * Send a copy of the term to the sink.
	 * Free variables in the copy will be mapped to fresh variables.
	 * Note the requirement on the freeRenames parameter to ensure that the copy is well-defined.
	 * @param sink to send events to
	 * @return the sink to use for subsequent events
	 */
	final Sink send(Sink sink) throws PlankException {
		return send(sink, new HashMap<Var, Var>());
	}

	/**
	 * Send a copy of the term to the sink, with renamings
	 * Free variables in the copy will be mapped to fresh variables.
	 * Note the requirement on the freeRenames parameter to ensure that the copy is well-defined.
	 * @param sink to send events to
	 * @param freeRenames map variables in this to equal variables in that
	 * @return the sink to use for subsequent events
	 */
	final Sink send(Sink sink, Map<Var, Var> freeRenames) throws PlankException {
		Meta dummy = mkMeta("internal", sort(), "#", Arrays.asList(new Term[0]));
		Match match = dummy.match(this);
		return dummy.rewriteTerm(sink, match, freeRenames);
	}

	/**
	 * Check equality of two terms modulo a free variable map.
	 * @param that the term to compare to
	 * @param freeRenames map variables in this to equal variables in that
	 */
	abstract boolean equalsTerm(Term that, Map<Var, Var> freeRenames);

	/**
	 * Use this term a the contraction of a rewrite.
	 * If match was constructed from pattern and this term is contraction, then this mimics pattern→contraction.
	 * @param sink to send the rewrite result to
	 * @param match previously constructed from pattern and redex
	 * @param freeRenames rename all free variables in this term to unique new names;
	 * 	variables not mapped by freeRenames will be mapped to arbitrary fresh variables 
	 * @return the sink to use for subsequent events after rewrite result has been received
	 * @throws PlankException if an inconsistency is discovered or the sink fails
	 */
	abstract Sink rewriteTerm(Sink sink, Match match, Map<Var, Var> freeRenames) throws PlankException;

	/**
	 * Perform substitution in this term.
	 * @param sink to send result of substitution to
	 * @param freeRenames rename all free variables in this term to unique new names;
	 * 	variables not mapped by freeRenames will be mapped to arbitrary fresh variables 
	 * @param substitution maps variables in this term to replacement terms
	 * @param replacementRenames variable renamings to perform in replacement terms
	 * @return the sink to use for subsequent events after rewrite result has been received
	 * @throws PlankException if an inconsistency is discovered or the sink fails
	 */
	abstract Sink substituteTerm(Sink sink, Map<Var, Var> freeRenames, Map<Var, Term> substitution, Map<Var, Var> replacementRenames)
			throws PlankException;
	
	/**
	 * Append plank textual form of term to an output.
	 * @param out where to send the text
	 * @param prefix text to insert before each turn; if it starts with newline, the procedure will extend it with indentation
	 * @param namings of variables that are being used
	 * @throws PlankException if there is a problem, including an IOException from appendable
	 */
	abstract void appendTerm(Appendable out, String prefix, Map<Var, String> namings) throws PlankException;
	
	// Object...
	
	@Override
	public final boolean equals(Object obj) {
		return obj instanceof Term && equalsTerm((Term) obj, new HashMap<>());
	}
	
	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			appendTerm(sb, "\n  ", new HashMap<Var, String>());
		} catch (PlankException e) {
			sb.append("**** BAD TERM (" + e.getMessage() + ") ****");
		}
		return sb.toString();
	}
}