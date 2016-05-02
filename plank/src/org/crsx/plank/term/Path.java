/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import java.util.ArrayDeque;
import java.util.Iterator;

import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;

/**
 * A path into a term.
 * @see Term#path()
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class Path implements Comparable< Path>, Cloneable {
	
	// State.
	
	/**
	 * Path to pin-point a subtree in a term as a sequence of steps.
	 * <p>
	 * The implementation is a deque used as a stack with the top of stack as the first navigation from the root.
	 */
	private final ArrayDeque<Step> _path;
	
	// Constructor.
	
	/** Instantiate. */
	Path() {
		_path = new ArrayDeque<>();
	}

	// Methods.
	
	// * General.
	
	/** Is the path empty? */
	public boolean isEmpty() {
		return _path.isEmpty();
	}
	
	/** Is the path a single step? */
	public boolean isStep() {
		return _path.size() == 1;
	}

	/** The size of the path. */
	public int size() {
		return _path.size();
	}

	/**
	 * Follow the path all the way.
	 * Does not modify the path.
	 * @param term to follow the path in
	 * @return the subterm at the end of the path
	 */
	public Term apply(Term term) throws PlankException {
		for (Step step : _path)
			term = step.apply(term);
		return term;
	}

	// * Observe and add first step.
	
	/** Whether there is a first step that selects an immediate scope body. */
	boolean startsWithScopeTerm() {
		return !_path.isEmpty() && _path.peek().key == null;
	}
	
	/**
	 * Return the first index number (for either {@link Cons#sub} or {@link Cons#assoc}).
	 * @throws PlankException for empty path
	 */
	int firstIndex() throws PlankException {
		if (_path.isEmpty())
			throw new PlankException("cannot take first step of empty path");
		return _path.peek().index;
	}

	/**
	 * Return the first index number (for either {@link Cons#sub} or {@link Cons#assoc}).
	 * @throws PlankException for empty path or path starting with a scope
	 */
	Var firstKey() throws PlankException {
		if (startsWithScopeTerm())
			throw new PlankException("need a first association step to get the key");
		return _path.peek().key;
	}

	/**
	 * Consume the first step of this path.
	 * Actually removes the first step of the path, making it one shorter.
	 * (In case the step goes into an association value it becomes two shorter.)
	 * @param term to start from
	 * @return the component of the term corresponding to following a step of the path
	 * @throws PlankException if the path is empty or invalid for this term
	 */
	public Term popApplyStep(Term term) throws PlankException {
		if (_path.isEmpty())
			throw new PlankException("cannot take first step of empty path");
		return _path.pop().apply(term);
	}

	/** Pop the first step off the path. Chainable. */
	public Path popStep() throws PlankException {
		_path.pop();
		return this;
	}
	
	/** Pushes a new initial step on the path to enter the n'th scope (0-based) of a term. Chainable. */
	public Path pushScopeStep(int n) {
		_path.push(new Step(n, null));
		return this;
	}

	/** Pushes a new initial step on the path to enter the value of the (0-based) n'th association map's k key of a term. Chainable. */
	public Path pushAssocStep(int n, Var k) {
		_path.push(new Step(n, k));
		return this;
	}

	/** Pushes a new initial step on the path. Chainable. */
	public Path pushStep(Step step) {
		_path.push(step);
		return this;
	}
	
	// Observe and modify last step.
	
	/** Whether there is a last step that selects an immediate scope body. */
	boolean endsWithScopeTerm() {
		return !_path.isEmpty() && _path.peekLast().key == null;
	}

	/**
	 * Return the last index number (for either {@link Cons#sub} or {@link Cons#assoc}).
	 * @throws PlankException for empty path
	 */
	int lastIndex() throws PlankException {
		if (_path.isEmpty())
			throw new PlankException("cannot take last step of empty path");
		return _path.peekLast().index;
	}

	/**
	 * Return the last key (
	 * @throws PlankException for empty path or path ending with a scope
	 */
	Var lastKey() throws PlankException {
		if (startsWithScopeTerm())
			throw new PlankException("need a last association step to get the key");
		return _path.peekLast().key;
	}
	
	// Comparable...

	@Override
	public int compareTo(Path that) {
		Iterator<Step> it1 = _path.iterator();
		Iterator<Step> it2 = that._path.iterator();
		while (it1.hasNext()) {
			if (!it2.hasNext())
				return 1;
			Step step1 = it1.next();
			Step step2 = it2.next();
			int cmp = step1.compareTo(step2);
			if (cmp != 0)
				return cmp;
		}
		return it2.hasNext() ? -1 : 0;
	}

	// Cloneable...
	
	@Override
	protected Path clone() {
		Path p = Term.path();
		for (Step step : _path)
			p._path.add(step);
		return p;
	}

	// Object...
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Path && compareTo((Path) other) == 0;
	}
	
	@Override
	public String toString() {
		String result = "";
		String sep = "";
		for (Step step : _path)
			result += sep + step.toString();
		return result;
	}
}