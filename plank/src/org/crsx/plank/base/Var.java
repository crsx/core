/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.base;

/**
 * A variable instance in a term.
 * Variables are only equal when they are the same instance.
 * Variables hash and compare using {@link System#identityHashCode(Object)}.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
final public class Var implements Comparable<Var> {

	/** Base name of variable. */
	public final String name;
	
	/**
	 * Create variable instance.
	 * @param name prefix of variable (it may print differently)
	 */
	public Var(String name) {
		this.name = name;
	}
	
	// Object...

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	// Comparable...

	@Override
	public int compareTo(Var o) {
		return hashCode() - o.hashCode();
	}
}