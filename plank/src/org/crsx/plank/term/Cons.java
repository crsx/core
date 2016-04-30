/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import java.io.IOException;
import java.util.Map;

import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.sort.ConsForm;
import org.crsx.plank.sort.Sort;

/**
 * A construction.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class Cons extends Term {

	// State.
	
	/** The declared form of the construction. */
	public final ConsForm form;
	
	/** The binders for the scopes of the construction. */ 
	public final Var[][] binder;
	
	/** The subterms of the scopes of the construction. */
	public final Term[] sub;
	
	/** The associations of the construction. */
	public final Assoc[] assoc;

	/** Generate it. */
	Cons(final String origin, final Sort sort, final ConsForm form, final Var[][] binder, final Term[] sub, final Assoc[] assoc) {
		super(origin, sort);
		this.form = form;
		this.binder = binder;
		this.sub = sub;
		this.assoc = assoc;
		assert sub.length == binder.length : "Panic: construction with inconsistent binders and subterms?";
		assert form.subSort.length == sub.length : "Panic: construction subterms inconsistent with form?!?";
		assert form.binderSort.length == binder.length : "Panic: construction binders inconsistent with form?!?";
		for (int i = 0; i < sub.length; ++i) {
			assert form.binderSort[i].length == binder[i].length : "Panic: construction binders inconsistent with form?!?";
		}
		assert form.keySort.length == assoc.length : "Panic: construction associations inconsistent with form?!?";
	}

	// Term...
	
	@Override
	public Kind kind() {
		return Kind.CONS;
	}
	
	@Override
	public Cons cons() {
		return this;
	}

	@Override
	boolean equalsTerm(Term that, Map<Var, Var> freeRenames) {
		if (that.kind() != Kind.CONS)
			return false;
		final Cons c = that.cons();
		if (!form.equals(c.form))
			return false;
		for (int i = 0; i < c.sub.length; ++i) {
			for (int j = 0; j < binder[i].length; ++j)
				freeRenames.put(binder[i][j], c.binder[i][j]);
			if (!sub[i].equalsTerm(c.sub[i], freeRenames))
				return false;
		}
		return true;
	}

	@Override
	Sink rewriteTerm(Sink sink, Match match, Map<Var, Var> freeRenames) throws PlankException {
		sink = sink.open(origin(), sort(), form);
		// Scopes.
		final int scopeCount = sub.length;
		for (int i = 0; i < scopeCount; ++i) {
			final int rank = binder[i].length;
			final Var[] newBinders = new Var[rank];
			for (int j = 0; j < rank; ++j) {
				final Var b = binder[i][j];
				final Var b2 = new Var(b.name);
				freeRenames.put(b,  b2);
				newBinders[j] = b2;
			}
			sink = sink.scope(newBinders);
			sink = sub[i].rewriteTerm(sink, match, freeRenames);
		}
		// Associations.
		final int assocCount = assoc.length;
		for (int i = 0; i < assocCount; ++i) {
			final Assoc a = assoc[i];
			sink = sink.openAssoc(a.origin(), a.keySort, a.valueSort);
			for (Map.Entry<Var, Term> e : a.map.entrySet()) {
				Var key = e.getKey();
				Term value = e.getValue();
				sink = sink.map(freeRenames.containsKey(key) ? freeRenames.get(key) : key); 
				sink = value.rewriteTerm(sink, match, freeRenames);
			}
			sink = sink.closeAssoc();
		}
		sink = sink.close();
		return sink;
	}

	@Override
	Sink substituteTerm(Sink sink, Map<Var, Var> freeRenames, Map<Var, Term> substitution, Map<Var, Var> replacementRenames) throws PlankException {
		sink = sink.open(origin(), sort(), form);
		// Scopes.
		final int scopeCount = sub.length;
		for (int i = 0; i < scopeCount; ++i) {
			final int rank = binder[i].length;
			final Var[] newBinders = new Var[rank];
			for (int j = 0; j < rank; ++j) {
				final Var b = binder[i][j];
				final Var b2 = new Var(b.name);
				freeRenames.put(b,  b2);
				newBinders[j] = b2;
			}
			sink = sink.scope(newBinders);
			sink = sub[i].substituteTerm(sink, freeRenames, substitution, replacementRenames);
		}
		// Associations.
		final int assocCount = assoc.length;
		for (int i = 0; i < assocCount; ++i) {
			final Assoc a = assoc[i];
			sink = sink.openAssoc(a.origin(), a.keySort, a.valueSort);
			for (Map.Entry<Var, Term> e : a.map.entrySet()) {
				Var key = e.getKey();
				Term value = e.getValue();
				sink = sink.map(freeRenames.containsKey(key) ? freeRenames.get(key) : key); 
				sink = value.substituteTerm(sink, freeRenames, substitution, replacementRenames);
			}
			sink = sink.closeAssoc();
		}
		sink = sink.close();
		return sink;
	}

	@Override
	void appendTerm(Appendable out, String prefix, Map<Var, String> namings) throws PlankException {
		try {
			out.append(prefix);
			out.append("<");
			sort().appendSort(out, namings);
			out.append(">");
			out.append(form.name);
			if (sub.length + assoc.length > 0) {
				if (prefix.startsWith("\n")) prefix += "  ";
				
				String sep = "(";
				// Scope arguments.
				for (int i = 0; i < sub.length; ++i) {
					out.append(sep);
					if (binder[i].length > 0) {
						String sep2 = "[";
						for (Var b : binder[i]) {
							out.append(sep2);
							String newName = b.name + namings.size();
							namings.put(b, newName);
							out.append(newName);
							sep2 = ",";
						}
						out.append("]");
					}
					sub[i].appendTerm(out, prefix, namings);;
					sep = ", ";
				}
				// Association arguments.
				for (Assoc a : assoc) {
					out.append(prefix);
					out.append(sep);
					a.appendAssoc(out, prefix, namings);
					sep = ", ";
				}
				out.append(")");
			}
		} catch (IOException e) {
			throw new PlankException(e);
		}
	}
}