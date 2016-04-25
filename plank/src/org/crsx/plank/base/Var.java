/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.base;

/**
 * A variable instance in a term.
 * @author krisrose
 */
public class Var {

	public final String name;
	
	/**
	 * Create variable instance.
	 * @param name prefix of variable (it may print differently)
	 */
	public Var(String name) {
		this.name = name;
	}
}