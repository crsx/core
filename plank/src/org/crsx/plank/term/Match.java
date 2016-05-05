/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.crsx.plank.base.Origined;
import org.crsx.plank.base.Var;

/**
 * Match builder.
 * @see Term#match(Term)
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class Match extends Origined {

	// External interface.
	
	/**
	 * Attempt match.
	 * @param origin of match
	 * @param pattern of match - this can be a pattern fragment, i.e., does not need to be a defined construction
	 * @param redex of match
	 * @param full whether to force the full match, even in case the match fails, or collect the failure point
	 */
	static Match match(String origin, Term pattern, Term redex, boolean full) {
		return new Match(origin, pattern, redex, full);
	}
	
	/**
	 * A substitute component of a valuation.
	 * @param <T> the kind of part of the redex that this substitute substitutes
	 */
	static class Substitute<T> {
		/** Bound variables in redex component that must be substituted. */
		public final Var[] var;
		/** Redex component. */
		public final T body;
		/** Create substitute. */
		private Substitute(final Var[] var, final T body) {
			this.var = var;
			this.body = body;
		}
		@Override
		public String toString() {
			return Arrays.asList(var).toString() +  body.toString();
		}
	}
	
	// State.
	
	/** Valuate (map) each meta-variable to the substitute describing what substitutions it can perform and in what redex. */
	public final Map<String, Substitute<Term>> valuation = new HashMap<>();

	/** Free variable renamings. */
	public final Map<Var, Var> freeRenames = new HashMap<Var, Var>();

	/** Association catch-all mappings. */
	public final Map<String, Substitute<Assoc>> assocAllValuation = new HashMap<>();
	
	/** Whether the match succeeded. */
	public boolean success = true;
	
	/** Whether the match could be half of a unification (only accurate for {@link #full} match). */
	public boolean unifyPossible = true;

	/** Whether the match failed because of a conflict with something irreducible. */
	public boolean alwaysFail = false;

	/** Whether the match failed because a variable was not substituted. */
	public boolean variableFail = false;

	/** The path to the place where the match failed. */
	public final Path failurePath = Term.path();
	
	/** Whether a full match was attempted (continues after failure, useful for unification). */
	public final boolean full;
	
	// Constructor.
	
	/** Real instance. */
	private Match(String origin, Term pattern, Term redex, boolean full) {
		super(origin);
		this.full = full;
		matchTerm(pattern, redex, new HashMap<Var, Var>()); // do the actual matching!
	}

	// Helpers (all invoked by construction).
	
	/**
	 * Handle recursive matching.
	 * @param pattern fragment of original pattern to match
	 * @param redex fragment of original redex to match
	 * @param binderMap maps variables in pattern to variables in redex
	 */
	private void matchTerm(final Term pattern, final Term redex, final Map<Var, Var> binderMap) {
		switch (pattern.kind()) {
		
		case CONS : {
			// Pattern is construction.
			switch (redex.kind()) {
			
			case CONS : {
				// Pattern and redex both constructions.
				final Cons p = pattern.cons();
				final Cons r = redex.cons();
				if (p.form.equals(r.form)) {
					// Same form!
					// First check scopes.
					final int scopeCount = p.sub.length; // of both
					for (int i = 0; i < scopeCount; ++i) { // every scope
						final int scopeRank = p.binder[i].length;
						for (int j = 0; j < scopeRank; ++j) // every binder of scope
							binderMap.put(p.binder[i][j], r.binder[i][j]);
						matchTerm(p.sub[i], r.sub[i], binderMap); // match scopes recursively
						if (!full && !success) { failurePath.pushScopeStep(i); return; }
					}
					// Second check associations.
					final int assocCount = p.assoc.length;
					for (int i = 0; i < assocCount; ++i) {
						matchAssoc(p.assoc[i], r.assoc[i], binderMap, i);
						if (!full && !success) return; // failure already updated in matchAssoc
					}
				} else {
					// Different forms.
					assert !p.form.name.equals(r.form.name) : "Panic: ConsForm not unique by name?!?";
					success = false;
					unifyPossible = false;
					if (!full) {
						if (!r.form.scheme)
							alwaysFail = true; // cannot recover from data-data failure
						return;
					}
				}
				break;
			}
			
			case OCCUR :
				// Pattern is construction, redex is variable. Construction cannot match or unify variable.
				success = false;
				unifyPossible = false;
				if (!full) { variableFail = true; return; }
				break;
				
			case META :
				// Pattern is construction, redex is meta-application. Construction cannot match meta-application but perhaps unify.
				success = false;
				if (!full) { alwaysFail = true; return; }
				break;

			}
			break;
		} // end of constructor pattern cases
		
		case OCCUR : {
			// Pattern is variable.
			switch (redex.kind()) {

			case CONS :
				// Pattern is variable, redex is construction. Construction cannot match or unify variable.
				success = false;
				unifyPossible = false;
				if (!full) { alwaysFail = true; return; }
				break;

			case OCCUR : {
				// Pattern and redex are both variables!
				final Var p = pattern.occur().var;
				final Var r = redex.occur().var;
				final Var rCandidater = mapVariable(p, r, binderMap);
				if (rCandidater != r) {
					success = false;
					unifyPossible = false;
					if (!full) { alwaysFail = true; return; }
				}
				break;
			}

			case META :
				// Pattern is variable, redex is meta-variable. May unify.
				success = false;
				if (!full) { alwaysFail = true; return; }
				break;

			}
			break;
		} // end of variable pattern cases
		
		case META : {
			// Pattern is meta-application, with just bound variables as substitution parameters. Redex kind does not matter. 
			final Meta p = pattern.meta();
			final Term r = redex;
			if (!valuation.containsKey(p.name)) {
				// First encounter of this meta-variable! Valuate it.
				updateValuation(valuation, p, r, binderMap);
			} else {
				// Subsequent encounter - non-left-linear pattern!
				// TODO
			}
			// TODO: Check missing bound variables.
			break;
		}
		
		} // end of meta-application pattern cases
	}

	/**
	 * Match associations.
	 * @param pAssoc pattern association
	 * @param rAssoc redex association
	 * @param binderMap mapping from pattern to redex binders 
	 */
	private void matchAssoc(final Assoc pAssoc, final Assoc rAssoc, final Map<Var, Var> binderMap, int n) {
		
		// Match plain variable mappings.
		for (Var pKey : pAssoc.map.keySet()) {
			final Var rCandidate = mapVariable(pKey, null, binderMap);
			if (rCandidate != null && rAssoc.map.containsKey(rCandidate)) {
				// Pattern key variable is known and the corresponding variable exists in redex map.
				matchTerm(rAssoc.map.get(pKey), rAssoc.map.get(rCandidate), binderMap);
				if (!full && !success) { failurePath.pushAssocStep(n, pKey); return; }
			} else {
				// Pattern key variable unknown or unmapped in redex.
				success = false;
				unifyPossible = false;
				if (!full) { failurePath.pushAssocStep(n, pKey); return; }
			}
		}
		
		// Check variable omission constraints.
		for (Var pOmit : pAssoc.omit) {
			final Var rOmitCandidate = mapVariable(pOmit, null, binderMap);
			if (rOmitCandidate == null || rAssoc.map.containsKey(rOmitCandidate)) {
				// Pattern key variable is unknown or occurs in redex map.
				success = false;
				unifyPossible = false;
				if (!full) { alwaysFail = true; return; }
			}
		}
		
		// Capture catch-all meta-applications.
		for (Meta pAll : pAssoc.all) {
			if (!assocAllValuation.containsKey(pAll.name)) {
				// First encounter of this meta-variable! Valuate it.
				updateValuation(assocAllValuation, pAll, rAssoc, binderMap);
			} else {
				// Subsequent encounter - non-left-linear pattern!
				// TODO
			}
			// TODO: Check missing bound variables.
		}
	}

	/**
	 * Find variable that pattern variable maps to in redex.
	 * @param p variable in pattern
	 * @param r fall-back variable in redex, or null for none
	 * @param binderMap map of bound variable mappings
	 */
	private Var mapVariable(final Var p, final Var r, final Map<Var, Var> binderMap) {
		if (binderMap.containsKey(p))
			return binderMap.get(p);
		if (freeRenames.containsKey(p))
			return freeRenames.get(p);
		if (r != null)
			freeRenames.put(p, r); // fall-back
		return r;
	}

	/**
	 * Update a valuation with a substitute.
	 * @param val the valuation to update
	 * @param p meta-application to map
	 * @param r redex component to valuate to
	 * @param binderMap with binders providing the substitute's binders
	 */
	private <T>void updateValuation(final Map<String, Substitute<T>> val, final Meta p, final T r, final Map<Var, Var> binderMap) {
		final int size = p.sub.length;
		final Var[] substituteVariables = new Var[size];
		for (int i = 0; i < size; ++i) {
			substituteVariables[i] = binderMap.get(p.sub[i].occur().var);
		}
		val.put(p.name,  new Substitute<T>(substituteVariables, r));
	}
	
	// Object...
	
	@Override
	public String toString() {
		return "MAP(\n"
				+ "  meta: " + valuation + "\n"
				+ " var: " + freeRenames + "\n"
				+ " assoc: " + assocAllValuation + "\n"
				+ ")";
	}
}