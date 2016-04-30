/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.sort.ConsForm;
import org.crsx.plank.sort.Sort;

/**
 * Helper to build a term by sending events to a sink.
 * @see Term#builder()
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class TermBuilder extends Sink {

	// State.

	/** The sink for the next event, updated by all {@link Sink} methods of this container class. */
	private Sink _sink;
	
	/** The result term, when done. */
	private Term _term;
	
	// Constructor.
	
	/** Instantiate builder. */
	public TermBuilder() {
		_sink = new RootSink(null);
	}
	
	// Methods

	/**
	 * Return term that has been built.
	 * @throws PlankException if term is unfinished
	 */
	public Term build() throws PlankException {
		if (_term == null)
			throw new PlankException("Premature attempt to extract unfinished term from builder");
		return _term;
	}
	
	// Sink...
	
	@Override
	public Sink open(String origin, Sort sort, ConsForm form) throws PlankException {
		_sink = _sink.open(origin, sort, form);
		return this;
	}

	@Override
	public Sink close() throws PlankException {
		_sink = _sink.close();
		return this;
	}

	@Override
	public Sink scope(Var[] binders) throws PlankException {
		_sink = _sink.scope(binders);
		return this;
	}

	@Override
	public Sink openAssoc(String origin, Sort keySort, Sort valueSort) throws PlankException {
		_sink = _sink.openAssoc(origin, keySort, valueSort);
		return this;
	}

	@Override
	public Sink closeAssoc() throws PlankException {
		_sink = _sink.closeAssoc();
		return this;
	}

	@Override
	public Sink map(Var key) throws PlankException {
		_sink = _sink.map(key);
		return this;
	}

	@Override
	public Sink occur(String origin, Sort sort, Var var) throws PlankException {
		_sink = _sink.occur(origin, sort, var);
		return this;
	}

	// Classes doing the actual work.
	
	/**
	 * The sink at the root of the construction.
	 * Understands all delegations to the subclasses (that all extend it).
	 * @author Kristoffer H. Rose <krisrose@crsx.org>
	 */
	private class RootSink extends Sink {

		// State.
		/** Parent sink. */
		final RootSink _parent;
		/** Bound variable mappings from term to sink events. Used by all subclasses. */
		final Map<Var, Var> _boundNew = new HashMap<>();
		
		// Constructor.
		/** Capture parent sink. */
		RootSink(RootSink parent) {
			_parent = parent;
		}
		
		// Methods
		/** Add term to the current context, and return sink to continue the current context. */
		Sink addTerm(Term term) throws PlankException {
			_term = term; // this is the root -- we are done
			return null; // there is no continuation
		}
		/** Add association to the current context, and return sink to continue the current context. */
		Sink addAssoc(Assoc assoc) throws PlankException {
			throw new PlankException("cannot export association as unit");
		}
		
		// Sink...
		@Override
		public Sink open(String origin, Sort sort, ConsForm form) throws PlankException {
			return new ConsSink(this, origin, sort, form); // open local construction context -- always allowed!
		}
		@Override
		public Sink close() throws PlankException {
			throw new PlankException("can only close construction that has actually been started");
		}
		@Override
		public Sink scope(final Var[] binders) throws PlankException {
			throw new PlankException("scopes only allowed inside constructions");
		}
		@Override
		public Sink openAssoc(String origin, Sort keySort, Sort valueSort) throws PlankException {
			throw new PlankException("associations only allowed inside constructions");
		}
		@Override
		public Sink closeAssoc() throws PlankException {
			throw new PlankException("can only close association that has actually been started");
		}
		@Override
		public Sink map(Var key) throws PlankException {
			throw new PlankException("maps only allowed inside association contexts");
		}
		@Override
		public Sink occur(String origin, Sort sort, Var var) throws PlankException {
			return addTerm(Term.mkOccur(origin, sort, var));
		}
	}

	/**
	 * Holds a construction in progress.
	 * @author Kristoffer H. Rose <krisrose@crsx.org>
	 */
	class ConsSink extends RootSink {

		/** State is information for the final {@link Term#mkCons(String, Sort, ConsForm, Var[][], Term[], Assoc[])}. */
		private final String _origin;
		private final Sort _sort;
		private final ConsForm _form;
		private final List<Var[]> _binders = new ArrayList<>();
		private final List<Term> _subs = new ArrayList<>();
		private final List<Assoc> _assocs = new ArrayList<>();
		
		/** Constructor collects information from {@link Sink#open(String, Sort, ConsForm)}. */
		public ConsSink(RootSink parent, String origin, Sort sort, ConsForm form) {
			super(parent);
			_origin = origin;
			_sort = sort;
			_form = form;
		}

		// RootSink...
		@Override
		Sink addTerm(Term term) throws PlankException {
			if (_binders.size() == _subs.size())
				_binders.add(new Var[0]); // inject missing binders if not included
			_subs.add(term);
			return this; // ready for next
		}
		@Override
		Sink addAssoc(Assoc assoc) throws PlankException {
			_assocs.add(assoc);
			return this; // ready for next
		}

		// Sink...
		@Override
		public Sink close() throws PlankException {
			assert _binders.size() == _subs.size() : "Panic: internally misbalanced binders/subs?";
			// Finished. Pass entire construction to parent, that also continues the sink.
			final int subCount = _subs.size();
			return _parent.addTerm(Term.mkCons(_origin, _sort, _form,
					_binders.toArray(new Var[subCount][]), _subs.toArray(new Term[subCount]), 
					_assocs.toArray(new Assoc[_assocs.size()])));
		}
		@Override
		public Sink scope(Var[] scopeBinders) throws PlankException {
			if (_subs.size() != _binders.size())
				throw new PlankException("scope and subterm events out of sync");
			final int rank = scopeBinders.length;
			Var[] newScopeBinders = new Var[rank];
			for (int i = 0; i < rank; ++i) {
				Var oldv = scopeBinders[i];
				Var newv = new Var(oldv.name);
				_boundNew.put(oldv, newv);
				newScopeBinders[i] = newv;
			}
			_binders.add(newScopeBinders);
			return this;
		}
		@Override
		public Sink openAssoc(String origin, Sort keySort, Sort valueSort) throws PlankException {
			return new AssocSink(this, origin, keySort, valueSort);
		}
	}

	/**
	 * Holds an association map in progress.
	 * @author Kristoffer H. Rose <krisrose@crsx.org>
	 */
	class AssocSink extends RootSink {

		/** State information for the final {@link Assoc#mk(String, Sort, Sort, Map, java.util.Set, List)}. */ 
		private final String _origin;
		private final Sort _keySort;
		private final Sort _valueSort;
		private Var _key; // to save key from key event to after value subtree has been processed 
		private final Map<Var, Term> _map = new HashMap<>();

		/** Initial association information from {@link Sink#openAssoc(String, Sort, Sort)}. */
		public AssocSink(RootSink parent, String origin, Sort keySort, Sort valueSort) {
			super(parent);
			_origin = origin;
			_keySort = keySort;
			_valueSort = valueSort;
		}
		
		// RootSink...
		@Override
		Sink addTerm(Term term) throws PlankException {
			// Adding a term is adding a value.
			if (_key == null)
				throw new PlankException("value term in association map must follow a key");
			_map.put(_key,  term);
			_key = null;
			return this;
		}
		@Override
		Sink addAssoc(Assoc assoc) throws PlankException {
			throw new PlankException("associations only allowed in construction context");
		}
		
		// Sink...
		@Override
		public Sink closeAssoc() throws PlankException {
			return _parent.addAssoc(Assoc.mk(_origin, _keySort, _valueSort, _map, new HashSet<>(), new ArrayList<>()));
		}
		@Override
		public Sink map(Var key) throws PlankException {
			if (_key != null)
				throw new PlankException("keys need a value each");
			_key = key;
			return this;
		}
	}
}