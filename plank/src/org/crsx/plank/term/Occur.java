/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import java.io.IOException;
import java.util.Map;

import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.sort.Sort;

/**
 * A variable occurrence in a term.
 * @see Term#mkOccur(String, Sort, Var)
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class Occur extends Term {

	/** The variable that this is an occurrence of. */
	public final Var var;
	
	/** Instance. */
	Occur(String origin, Sort sort, Var var) {
		super(origin, sort);
		this.var = var;
	}

	// Term...
	
	@Override
	public Kind kind() {
		return Kind.OCCUR;
	}
	
	@Override
	public Occur occur() {
		return this;
	}

	@Override
	boolean equalsTerm(Term that, Map<Var, Var> freeRenames) {
		if (that.kind() != Kind.OCCUR)
			return false;
		Var v = that.occur().var;
		if (freeRenames.containsKey(v))
			return var.equals(freeRenames.get(v));
		return var.equals(v); 
	}

	@Override
	Sink rewriteTerm(Sink sink, Match match, Map<Var, Var> freeRenames) throws PlankException {
		return pickVar(sink, freeRenames);
	}

	@Override
	Sink substituteTerm(Sink sink, Map<Var, Var> freeRenames, Map<Var, Term> substitution, Map<Var, Var> replacementRenames) throws PlankException {
		if (substitution.containsKey(var))
			return substitution.get(var).send(sink, replacementRenames);
		return pickVar(sink, freeRenames);
	}

	/** Generate renamed variable to sink, fresh if not explicitly renamed (with side effect on freeRenames). */
	private Sink pickVar(Sink sink, Map<Var, Var> freeRenames) throws PlankException {
		Var v;
		if (freeRenames.containsKey(var)) {
			v = freeRenames.get(var);
		} else {
			// This is a "fresh" variable...create and record.
			v = new Var(var.name);
			freeRenames.put(var, v);
		}
		return sink.occur(origin(), sort(), v);
	}

	@Override
	public void appendTerm(Appendable out, String prefix, Map<Var, String> namings, boolean includeSorts) throws PlankException {
		// NOTE: This method depends on the Plank.g4 format.
		appendFreeVar(out, sort(), var, prefix, namings, includeSorts);
	}

	/** Append just the sorted free variable, extending namings as needed in the process. */
	static void appendFreeVar(Appendable out, Sort sort, Var var, String prefix, Map<Var, String> namings, boolean includeSorts) throws PlankException {
		// NOTE: This method depends on the Plank.g4 format.
		try {
			out.append(prefix);
			if (includeSorts) {
				out.append("<");
				sort.appendSort(out, namings);
				out.append(">");
			}
			sort.appendSort(out, namings);
			if (!namings.containsKey(var))
				namings.put(var, var.name + namings.size());
			out.append(prefix);
			out.append(namings.get(var));
		} catch (IOException e) {
			throw new PlankException(e);
		}
	}
}