/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.sort;

import org.crsx.plank.base.Origined;
import org.crsx.plank.base.Var;

/**
 * A sort, either a sort variable or a sort instance.
 * @author krisrose
 */
public class Sort extends Origined {
	
	/**
	 * Create sort variable.
	 * @param origin of variable
	 * @param var to use
	 */
	public static Sort mkSortVar(String origin, Var var) {
		return new Sort(origin, var, null, null);
	}

	/**
	 * Create sort instance.
	 * @param origin of instance
	 * @param name of sort it is an instance of
	 * @param param sort parameters
	 */
	public static Sort mkSortInstance(String origin, String name, Sort[] param) {
		return new Sort(origin, null, name, param);
	}
	
	/** For no parameters. */
	private final static Sort[] NO_PARAM = new Sort[0];

	// State.
	
	/** The variable for a sort variable, null otherwise. */
	private Var var;

	/** The sort name for a sort instance, null otherwise. */
	public final String name;
	
	/** The sort parameters of a sort instance, null otherwise. */
	public final Sort[] param;
	
	// Constructor.
	
	/** Instantiate sort. */
	private Sort(String origin, Var var, String name, Sort[] param) {
		super(origin);
		this.var = var;
		this.name = name;
		this.param = (param == null || param.length == 0 ? NO_PARAM : param);
	}
	
	// Methods.

	/** Is this sort just a sort variable? */
	public boolean isVar() {
		return var != null;
	}
	
	/** True only for sorts instances that apply a named sort to a list of sort variable parameters. */
	public boolean isPrimary() {
		if (isVar())
			return false;
		for (Sort p : param) {
			if (!p.isVar())
				return false;
		}
		return true;
	}
}