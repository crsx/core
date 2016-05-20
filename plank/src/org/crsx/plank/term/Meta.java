/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.term;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.sort.Sort;
import org.crsx.plank.term.Match.Substitute;

/**
 * A meta-application.
 * @see Term#mkMeta(String, Sort, String, java.util.List)
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public final class Meta extends Term {
	
	/** The meta-variable name. */
	public final String name;
	
	/** The substitution arguments. */
	public final Term[] sub;

	/** Instance. */
	Meta(String origin, Sort sort, String name, Term[] sub) {
		super(origin, sort);
		this.name = name;
		this.sub = sub;
	}

	// Term...
	
	@Override
	public Kind kind() {
		return Kind.META;
	}
	
	@Override
	public Meta meta() {
		return this;
	}

	@Override
	boolean equalsTerm(Term that, Map<Var, Var> freeRenames) {
		if (that.kind() != Kind.META)
			return false;
		final Meta m = that.meta();
		if (!name.equals(m.name))
			return false;
		for (int i = 0; i < m.sub.length; ++i) {
			if (!sub[i].equals(m.sub[i]))
				return false;
		}
		return true;
	}
	
	@Override
	Sink rewriteTerm(Sink sink, Match match, Map<Var, Var> freeRenames) throws PlankException {
		if (!match.valuation.containsKey(name))
			throw new PlankException("encountered unknown meta-variable %s in rewrite contraction", name);
		Substitute<Term> substitute = match.valuation.get(name);
		if (sub.length != substitute.var.length)
			throw new PlankException("inconsistent arity of meta-application of %s", name);
		Map<Var, Term> substitution = new HashMap<>();
		for (int i = 0; i < sub.length; ++i) {
			substitution.put(substitute.var[i], sub[i]);
		}
		return substitute.body.substituteTerm(sink, freeRenames, substitution, match);
	}

	@Override
	Sink substituteTerm(Sink sink, Map<Var, Var> freeRenames, Map<Var, Term> substitution, Match replacementMatch) throws PlankException {
		throw new PlankException("found meta-application in substituted term (%s)", name);
	}

	@Override
	public boolean containsFree(Set<Var> vars) {
		for (Term s : sub) {
			if (s.containsFree(vars))
				return true;
		}
		return false;
	}

	@Override
	public void appendTerm(Appendable out, String prefix, Map<Var, String> namings, boolean includeSorts) throws PlankException {
		// NOTE: This method depends on the Plank.g4 format.
		try {
			out.append(prefix);
			if (includeSorts) {
				out.append("<");
				sort().appendSort(out, namings);
				out.append(">");
			}
			out.append(name);
			if (sub.length > 0) {
				if (prefix.startsWith("\n")) prefix += "  ";
				String sep = "(";
				for (Term s : sub) {
					out.append(sep);
					s.appendTerm(out, prefix, namings, includeSorts);
					sep = ", ";
				}
				out.append(")");
			}
		} catch (IOException e) {
			throw new PlankException(e);
		}
	}
}