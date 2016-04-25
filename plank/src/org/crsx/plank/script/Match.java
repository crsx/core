/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.script;

import java.util.HashMap;
import java.util.Map;

import org.crsx.plank.base.Origined;
import org.crsx.plank.base.Var;
import org.crsx.plank.term.Cons;
import org.crsx.plank.term.Meta;
import org.crsx.plank.term.Occur;
import org.crsx.plank.term.Term;

/**
 * Match builder.
 * @author krisrose
 */
public class Match extends Origined {

	// Setup.
	private final Term _pattern;
	private final Term _redex;
	
	/**
	 * Initialize match.
	 * @param origin of match
	 * @param pattern of match
	 * @param redex of match
	 */
	public Match(String origin, Term pattern, Term redex) {
		super(origin);
		_pattern = pattern;
		_redex = redex;
		match(_pattern, _redex); // do the actual matching!
	}

	// State.
	private boolean _done = false;
	private final Map<String, Sub> _sub = new HashMap<String, Match.Sub>();
	private final Map<Var, Var> _rename = new HashMap<Var, Var>();
	private boolean _reverse = false;
	private boolean _conflict = false;
	
	// 
	
	/**
	 * A "substituend" component of a match. 
	 * @author krisrose
	 */
	public static class Sub {
		private final Var[] _var;
		private final Term _body;
		/**
		 * Create substituend.
		 * @param var
		 * @param body
		 */
		public Sub(Var[] var, Term body) {
			_var = var;
			_body = body;
		}
		public final Var[] var() {
			return _var;
		}
		public final Term body() {
			return _body;
		}
	}
	
	/** Handle recursive matching of one term. */
	private void match(Term pattern, Term redex) {
		switch (pattern.kind()) {
		
		case CONS : {
			// Pattern is construction.
			Cons p = pattern.cons();
			switch (redex.kind()) {
			
			case CONS : {
				// Two constructions! Compare.
				Cons r = redex.cons();
				
				break;
			}
			
			case OCCUR :
				// Construction cannot match or unify variable.
				_conflict = true;
				break;
				
			case META :
				// Construction cannot match meta-application but perhaps unify.
				_reverse = true;
				break;
			}
			break;
		}
		
		case OCCUR : {
			// Pattern is variable.
			Occur p = pattern.occur();
			switch (redex.kind()) {
			case CONS : {
				
				break;
			}
			case OCCUR :
				_conflict = true;
				break;
			case META :
				_reverse = true;
				break;
			}
			
			break;
		}
		
		case META : {
			// Pattern is meta-application.
			Meta p = pattern.meta();
			switch (redex.kind()) {
			case CONS : {
				
				break;
			}
			case OCCUR :
				_conflict = true;
				break;
			case META :
				_reverse = true;
				break;
			}
			
			break;
		}
		}
	}
	
	// Interrogate result.

	/** Get substituends. */
	public final Map<String, Sub> sub() {
		return _sub;
	}
	
	/** Get renamings. */
	public final Map<Var, Var> rename() {
		return _rename;
	}
	
	/** Whether the match succeeded. */
	public final boolean success() {
		return !_reverse && !_conflict;
	}
	
	/** Whether the match may be half of a unification. */
	public final boolean mayUnify() {
		return !_conflict;
	}	
}