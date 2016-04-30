/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import java.util.ArrayDeque;
import java.util.Deque;

import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;

/**
 * A path into a term.
 * @see Term#path()
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class TermPath {
	
	// State.
	
	/**
	 * Path to pin-point a subtree in a term.
	 * The members are either
	 * <ul>
	 * <li> positive (or zero) {@link Integer} values for an index into {@link Cons#sub} terms,
	 * <li> negative {@link Integer} values for an index into {@link Cons#assoc},
	 * <li> a {@link Var} for following a key to the corresponding value in an {@link Assoc}.
	 * </ul>
	 * where the last two always occur in pairs.
	 * <p>
	 * The implementation is a deque used as a stack with the top  of stack as the first navigation from the root.
	 */
	private final Deque<Object> _path = new ArrayDeque<>();
	
	// Constructor.
	
	/** Instantiate. */
	TermPath() {}

	// Methods.
	
	/** Is the path empty? */
	public boolean isEmpty() {
		return _path.isEmpty();
	}
	
	/** The size of the path (note going into the value of an association counts as two steps here). */
	public int size() {
		return _path.size();
	}

	/**
	 * Consume the first step of this path.
	 * Actually removes the first step of the path, making it one shorter.
	 * (In case the step goes into an association value it becomes two shorter.)
	 * @param term to start from
	 * @return the component of the term corresponding to following a step of the path
	 * @throws PlankException if the path is invalid for this term
	 */
	public Term popApplyStep(Term term) throws PlankException {
		return doApplyStep(_path, term);
	}

	/** Pop the first step off the path. */
	public void popStep() throws PlankException {
		if (_path.isEmpty())
			throw new PlankException("attempt to access non-existing subterm");
		Object step = _path.pop();
		assert step instanceof Integer : "Panic: unbalanced path - non-scope/assoc on top?!?";
		int index = (Integer) step;
		if (index < 0) {
			step = _path.pop();
			assert step instanceof Var : "Panic: unbalanced path - assoc not followed by key?!?";
		}
	}
	
	/** Pushes a new initial step on the path to enter the n'th scope (0-based) of a term . */
	public void pushScopeStep(int n) {
		_path.push(Integer.valueOf(n));
	}

	/** Pushes a new initial step on the path to enter the value of the (0-based) n'th association map's k key of a term. */
	public void pushAssocStep(int n, Var k) {
		_path.push(k);
		_path.push(Integer.valueOf(- n - 1));
	}

	/**
	 * Follow the path all the way.
	 * Does not modify the path.
	 * @param term to follow the path in
	 * @return the subterm at the end of the path
	 */
	public Term apply(Term term) throws PlankException {
		Deque<Object> path = new ArrayDeque<>();
		path.addAll(_path);
		while (!path.isEmpty())
			term = doApplyStep(path, term);
		return term;
	}

	/**
	 * Consume the first step of a path stack (like {@link #_path})..
	 * Actually removes the first step of the path, making it one shorter.
	 * (In case the step goes into an association value it becomes two shorter.)
	 * @param path to consume step from
	 * @param term to start from
	 * @return the component of the term corresponding to following a step of the path
	 * @throws PlankException if the path is invalid for this term
	 */
	private static Term doApplyStep(Deque<Object> path, Term term) throws PlankException {
		if (path.isEmpty())
			throw new PlankException("attempt to access non-existing subterm");
		if (term.kind() != Term.Kind.CONS)
			throw new PlankException("attempt to navigate into non-construction");
		Cons cons = term.cons();
		
		Object step = path.pop();
		assert step instanceof Integer : "Panic: unbalanced path?!?";
		int index = (Integer) step;
		if (index >= 0) {
			// Scope case.
			return cons.sub[index];
		} else {
			// Association case.
			index = - index - 1;
			Assoc assoc = cons.assoc[index];
			step = path.pop();
			assert step instanceof Var : "Panic: association lookup not followed by key lookup?!?";
			Var key = (Var) step;
			return assoc.map.get(key);
		}
	}
}