/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.crsx.plank.base.Origined;
import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.sort.ConsForm;
import org.crsx.plank.sort.Sort;
import org.crsx.plank.term.Assoc;
import org.crsx.plank.term.Meta;
import org.crsx.plank.term.Term;

/**
 * Helper struct representing a piece - a single argument of a construction, either scope or association - during parsing.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
final class Piece extends Origined {

	/**
	 * Make a scope piece.
	 * @param origin of piece
	 * @param binders of the scope (may be null)
	 * @param sub body term of the scope
	 */
	static Piece mkScope(String origin, List<Var> binders, Term sub) {
		return new Piece(origin,
				(binders == null || binders.isEmpty() ? NO_SCOPE_BINDERS : binders.toArray(new Var[binders.size()])),
				sub, null, null, null);
	}
	
	/**
	 * Make an association piece.
	 * @param origin of piece
	 * @param maps concrete variable to term mappings
	 * @param omit set of variables with ¬ markers
	 * @param all set of catch-all terms 
	 */
	static Piece mkAssoc(String origin, Map<Var, Term> maps, Set<Var> omit, List<Meta> all) {
		return new Piece(origin, null, null,
				(maps == null || maps.isEmpty() ? NO_MAPS : maps),
				(omit == null || omit.isEmpty()  ? NO_OMIT : omit),
				(all == null || all.isEmpty() ? NO_ALL : all));
	}
	
	/**
	 * Create association piece from list of association pieces.
	 * @param origin of list of pieces
	 * @param pieces to combine
	 */
	static Piece mkAssoc(String origin, List<Piece> pieces) {
		Map<Var, Term> maps = new HashMap<>();
		Set<Var> omit = new HashSet<Var>();
		List<Meta> all = new ArrayList<>();
		for (Piece p : pieces) {
			maps.putAll(p.maps);
			omit.addAll(p.omit);
			all.addAll(p.all);
		}
		return mkAssoc(origin, maps, omit, all);
	}

	/**
	 * Create construction term from pieces.
	 * @param origin of list of pieces
	 * @param sort to assign to the construction
	 * @param form the specific construction form to use
	 * @param cons name of constructor
	 * @param pieces the pieces that compose the construction
	 * @throws PlankException if the form and pieces are inconsistent
	 */
	public static Term mkCons(String origin, Sort sort, ConsForm form, String cons, List<Piece> pieces)
		throws PlankException
	{
		final int subCount = form.subSort.length;
		final int assocCount = form.keySort.length;
		if (subCount + assocCount != pieces.size())
			throw new PlankException("number of arguments do not match declaration");
		
		List<Var[]> binders = new ArrayList<>();
		List<Term> subs = new ArrayList<>();
		List<Assoc> assocs = new ArrayList<>();
		int assocIndex = 0;
		for (Piece piece : pieces) {
			if (piece.isScopePiece()) {
				subs.add(piece.sub);
				binders.add(piece.binder);
			} else { // association piece
				Sort keySort = form.keySort[assocIndex];
				Sort valueSort = form.valueSort[assocCount];
				assocs.add(Assoc.mk(origin, keySort, valueSort, piece.maps, piece.omit, piece.all));
			}
		}
		Var[][] binder = binders.isEmpty() ? NO_BINDERS : binders.toArray(new Var[binders.size()][]);
		Term[] sub = subs.isEmpty() ? NO_TERMS : subs.toArray(new Term[subs.size()]);
		Assoc[] assoc = assocs.isEmpty() ? NO_ASSOC : assocs.toArray(new Assoc[assocs.size()]);
		return Term.mkCons(origin, sort, form, binder, sub, assoc);
	}
	
	private static final Var[][] NO_BINDERS = new Var[0][];
	private static final Var[] NO_SCOPE_BINDERS = new Var[0];
	private static final Term[] NO_TERMS = new Term[0];
	private static final Assoc[] NO_ASSOC = new Assoc[0];
	private static final Map<Var, Term> NO_MAPS = new HashMap<>();
	private static final Set<Var> NO_OMIT = new HashSet<>();
	private static final List<Meta> NO_ALL = new ArrayList<>(0);
	
	// State.

	/** Scope binders (only when {@link #isScopePiece()}. */
	final Var[] binder;

	/** Scope subterm (only when {@link #isScopePiece()}. */
	final Term sub;

	/** Mapped variables (only when not {@link #isScopePiece()}. */
	final Map<Var, Term> maps;

	/** Omitted variables (only when not {@link #isScopePiece()}. */
	final Set<Var> omit;

	/** Catch-all terms (only when not {@link #isScopePiece()}. */
	final List<Meta> all;
	
	// Constructor.

	/** The real constructor. */
	private Piece(String origin, Var[] binder, Term sub, Map<Var, Term> maps, Set<Var> omit, List<Meta> all) {
		super(origin);
		this.binder = binder;
		this.sub = sub;
		this.maps = maps;
		this.omit = omit;
		this.all = all;
	}
	
	// Methods.
	
	/** Is this piece a scope piece? */
	boolean isScopePiece() {
		return sub != null;
	}
}