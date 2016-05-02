/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.sort.ConsForm;
import org.crsx.plank.sort.Sort;

/**
 * Construct a term by events.
 * Only supports non-meta terms.
 * Note: <em>every sink object can only be used once, after that use the one it returns!</em>
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public abstract class Sink {
	
	/**
	 * Open a construction context.
	 * Must be followed by sequence of scope events.
	 * @param origin for term
	 * @param sort of construction
	 * @param form of construction
	 * @throws PlankException if events are sent out of order or otherwise invalid
	 */
	public abstract Sink open(String origin, Sort sort, ConsForm form) throws PlankException;
	
	/**
	 * Close the most recently unclosed context, which must be a construction.
	 * @throws PlankException if events are sent out of order or otherwise invalid
	 */ 
	public abstract Sink close() throws PlankException;
	
	/**
	 * Add a scope to the most recently opened and unclosed construction.
	 * Must be followed by events that form the term body of the scope.
	 * @param binders the binders of the scope
	 * @throws PlankException if events are sent out of order or otherwise invalid
	 */
	public abstract Sink scope(Var[] binders) throws PlankException;

	/**
	 * Open an association argument context.
	 * @param origin of the association
	 * @param realIndex real argument index
	 * @param keySort of association
	 * @param valueSort of association
	 * @throws PlankException if events are sent out of order or otherwise invalid
	 */
	public abstract Sink openAssoc(String origin, int realIndex, Sort keySort, Sort valueSort) throws PlankException;
	
	/**
	 * Close the most recently opened and unclosed context, which must be an association.
	 * @throws PlankException if events are sent out of order or otherwise invalid
	 */
	public abstract Sink closeAssoc() throws PlankException;

	/**
	 * Add map to the currently open association.
	 * Must be followed by events for a term for the value.
	 * @param key the key
	 * @throws PlankException if events are sent out of order or otherwise invalid
	 */
	public abstract Sink map(Var key) throws PlankException;
	
	/**
	 * Insert a term that is a variable occurrence.
	 * @param origin of the variable occurrence
	 * @param sort of the variable occurrence
	 * @param var that is occurring
	 * @throws PlankException if events are sent out of order or otherwise invalid
	 */
	public abstract Sink occur(String origin, Sort sort, Var var) throws PlankException;
}