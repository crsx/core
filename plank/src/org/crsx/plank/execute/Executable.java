/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.execute;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.crsx.plank.base.PlankException;
import org.crsx.plank.loader.Rule;
import org.crsx.plank.term.Cons;
import org.crsx.plank.term.Match;
import org.crsx.plank.term.Path;
import org.crsx.plank.term.Step;
import org.crsx.plank.term.Term;
import org.crsx.plank.term.Term.Kind;
import org.crsx.plank.term.TermBuilder;

import com.google.common.collect.ImmutableListMultimap;

/**
 * Normalize input terms with the rewrite system.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public class Executable {
	
	// State.

	/** The rules, by pattern constructor. */
	private final ImmutableListMultimap<String, Rule> _constructorRules;
	
	// Constructor.
	
	/** Setup execution with the provided rules. */
	public Executable(Map<String, Rule> rules) {
		ImmutableListMultimap.Builder<String, Rule> b = ImmutableListMultimap.builder();
		for (Rule rule : rules.values())
			b.put(rule.pattern.form.name, rule);
		_constructorRules = b.build();
	}

	// Helper classes.
	
	/**
	 * The state of evaluation at a certain level of the evaluator.
	 * @author Kristoffer H. Rose <krisrose@crsx.org>
	 */
	final class State {
		/** The term that we are considering for evaluation. */
		final Term term;
		/** The path we followed in the term in this state to the next (pushed after, or for top, the current state). */
		final Path path;
		/** Whether this and all ancestors are data. */
		final boolean frontier;
		final Step lastFocus;
		/** Whether the term stored here was changed. */
		final boolean changed;
		/** Whether we have tried all rules with only data failures for this one. */
		final boolean blocked;
		/** Paths to direct children that have been decisively determined to be normalized. */
		final List<Step> nfs;
		/** Create a state (null only allowed for nfs to initialize as empty). 
		 * @param lastFocus TODO*/
		State(Term term, Path path, boolean frontier, Step lastFocus, boolean changed, boolean blocked, List<Step> nfs) {
			this.term = term;
			this.path = path;
			this.frontier = frontier;
			this.lastFocus = lastFocus;
			this.changed = changed;
			this.blocked = blocked;
			this.nfs = nfs == null ? new ArrayList<Step>() : nfs;
		}
		// TODO: toString?
	}

	// Methods.
	
	/**
	 * Normalize term with the rules of the script.
	 * Proceeds top-down.
	 * When a rule fails because of an unevaluated fragment, we suspend the normal evaluation to evaluate that fragment.
	 * @param input to normalize - will be destroyed
	 * @throws PlankException
	 */
	public Term normalize(Term input) throws PlankException {
		
		// Make copy?
		TermBuilder tb = Term.builder();
		input.send(tb);
		input = tb.build();

		// Start evaluation with empty stack.
		Deque<State> stack = new ArrayDeque<>();
		
		// The work state.
		Term term = input; // current work term
		boolean changed = false;
		boolean frontier = true; // whether all stack terms are data
		boolean reducible = true; // whether we think the top term is reducible
		boolean blocked = false; // not blocked yet!
		boolean variableFail = false; // we have failed with variables (so blocked but only until the context catches up)
		Path schemeFailure = null; // we have failed with a scheme at the path if non-null
		Step lastFocus = null; // for walking the frontier - should be Path!
		
		// Main loop.
		Evaluate: while (true) {

			// Function.
			if (term.isFun()) {
				
				// Evaluation: We have an unblocked function!
				if (reducible) {
					Cons fun = term.cons();

					// Assume the worst until proven otherwise...
					blocked = true;
					variableFail = true;
					schemeFailure = null;

					// Try the rules for the function until we get a match.
					List<Rule> rules = _constructorRules.get(fun.form.name);
					for (Rule rule : rules) {

						// Try to match a rule.
						Match match = rule.pattern.match(fun);
						if (match.success) {

							// We have a successful match...destructive rewrite and retry!
							TermBuilder b = Term.builder();
							rule.contractum.rewrite(b, match);
							term = b.build();
							changed = true;
							// Reset state and restart loop.
							blocked = false;
							variableFail = false;
							schemeFailure = null;
							continue Evaluate;

						}
						// The match failed. Update the state and try next rule...
						variableFail = variableFail || match.variableFail;
						blocked = blocked && (match.alwaysFail || (frontier && variableFail));
						if (schemeFailure == null && !match.alwaysFail && !match.variableFail)
							schemeFailure = match.failurePath;
						
					} // rule loop

					// All rules tried and failed. Restart evaluator with the state updated.
					reducible = false;
					continue Evaluate;
				}

				// This function cannot reduce but we have a place to evaluate! Suspend this, switch to there, reset state, and restart.
				if (schemeFailure != null) {
					assert term.isFun() : "Suspending non-function for schemeFailure?";
					stack.push(new State(term, schemeFailure, frontier, lastFocus, changed, false, null));
					term = schemeFailure.apply(term);
					changed = false;
					reducible = true;
					frontier = false;
					blocked = false;
					variableFail = false;
					schemeFailure = null;
					lastFocus = null;
					continue Evaluate;
				}
				
				// Function cannot make progress...fall through to refocusing.
			}
			
			// All cases where the term cannot reduce come here. We have to move the focus elsewhere!
			
			frontier = stack.isEmpty() || stack.peek().frontier; // update whether we're on the frontier
			if (frontier) {
				if (term.kind() == Kind.CONS) {
					Cons cons = term.cons();

					// 	If we are on the frontier with a usual construction then go brute force to first or next child...TODO: function!
					Step child = lastFocus == null ? Step.first(cons) : lastFocus.next(term);
					if (child != null) {
						stack.push(new State(cons, Term.path().pushStep(child), frontier, lastFocus, changed, blocked, null)); 
						term = child.apply(term);
						changed = false;
						reducible = true;
						frontier = false;
						lastFocus = null;
						blocked = false;
						variableFail = false;
						schemeFailure = null;
						continue Evaluate;
					}
				}
			}

			// No more to do here. Pop stack and repeat.
			State parent = stack.pop();
			if (changed)
				parent.term.update(parent.path, term);
			term = parent.term;
			reducible = parent.frontier || parent.blocked;
			frontier = parent.frontier;
			lastFocus = parent.lastFocus;
			changed = parent.changed;
			blocked = parent.blocked;
			variableFail = false;
			schemeFailure = null;
			continue Evaluate;
			
		}
	}
}