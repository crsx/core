/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.loader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.crsx.plank.base.PlankException;
import org.crsx.plank.base.Var;
import org.crsx.plank.parser.PlankBaseVisitor;
import org.crsx.plank.parser.PlankLexer;
import org.crsx.plank.parser.PlankParser;
import org.crsx.plank.parser.PlankParser.AllAssocContext;
import org.crsx.plank.parser.PlankParser.AssocFormContext;
import org.crsx.plank.parser.PlankParser.AssocPieceContext;
import org.crsx.plank.parser.PlankParser.AssociationContext;
import org.crsx.plank.parser.PlankParser.AssociationsContext;
import org.crsx.plank.parser.PlankParser.BindersContext;
import org.crsx.plank.parser.PlankParser.BindersortsContext;
import org.crsx.plank.parser.PlankParser.ConsTermContext;
import org.crsx.plank.parser.PlankParser.DataDeclarationContext;
import org.crsx.plank.parser.PlankParser.DeclarationContext;
import org.crsx.plank.parser.PlankParser.FormContext;
import org.crsx.plank.parser.PlankParser.FormsContext;
import org.crsx.plank.parser.PlankParser.HscriptContext;
import org.crsx.plank.parser.PlankParser.InstanceSortContext;
import org.crsx.plank.parser.PlankParser.MapAssocContext;
import org.crsx.plank.parser.PlankParser.MetaTermContext;
import org.crsx.plank.parser.PlankParser.NameContext;
import org.crsx.plank.parser.PlankParser.NotAssocContext;
import org.crsx.plank.parser.PlankParser.OptContext;
import org.crsx.plank.parser.PlankParser.OptsContext;
import org.crsx.plank.parser.PlankParser.PiecesContext;
import org.crsx.plank.parser.PlankParser.PriorityContext;
import org.crsx.plank.parser.PlankParser.RawtermContext;
import org.crsx.plank.parser.PlankParser.RuleDeclarationContext;
import org.crsx.plank.parser.PlankParser.SchemeDeclarationContext;
import org.crsx.plank.parser.PlankParser.ScopeFormContext;
import org.crsx.plank.parser.PlankParser.ScopePieceContext;
import org.crsx.plank.parser.PlankParser.SortContext;
import org.crsx.plank.parser.PlankParser.SortannoContext;
import org.crsx.plank.parser.PlankParser.SortparamsContext;
import org.crsx.plank.parser.PlankParser.SortsContext;
import org.crsx.plank.parser.PlankParser.TermContext;
import org.crsx.plank.parser.PlankParser.TermsContext;
import org.crsx.plank.parser.PlankParser.VarSortContext;
import org.crsx.plank.parser.PlankParser.VarTermContext;
import org.crsx.plank.parser.PlankParser.VariableDeclarationContext;
import org.crsx.plank.sort.ConsForm;
import org.crsx.plank.sort.Sort;
import org.crsx.plank.term.Cons;
import org.crsx.plank.term.Meta;
import org.crsx.plank.term.Term;
import org.crsx.plank.term.Term.Kind;
import org.crsx.plank.util.LayeredMap;

import com.google.common.collect.ImmutableMap;

/**
 * Load a parsed plank script into a {@link Loader}.
 * This includes assigning sorts to all nodes.
 * @author krisrose
 */
public class PlankBuilder extends PlankBaseVisitor<Object> {

	/**
	 * Parse a plank script and return a context loaded with the script.
	 * @param input textual form of the script
	 */
	public static Loader parse(CharStream input) {
		PlankBuilder builder = new PlankBuilder();
		
		// Parse.
		PlankLexer lexer = new PlankLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		PlankParser parser = new PlankParser(tokens);
		//parser.setTrace(true);
		try {
			ParseTree tree = parser.hscript();
			System.out.println(tree.toStringTree(parser));
			builder.visit(tree); // this is where the real work is done...
		} catch (RecognitionException e) {
			String message = e.getMessage();
			Token token = e.getOffendingToken();
			builder.context().addError(originRange(token, token), "%s", message);
		}
		
		return builder.context(); // return result
	}

	// State.
	
	/** Context to construct. */
	private final Loader _loader = new Loader();
	
	/** Separate pattern and contraction processing. */ 
	private boolean _inPattern;
	
	/** Variable names used during parsing of a declaration (scoped for rules). */
	private final LayeredMap<String, Var> _varsScope = new LayeredMap<>();
	
	/** The substitute-sorts of meta-variables in a rule. */
	private final Map<String, Form> _metaSubstSorts = new HashMap<>();

	/** The sorts of free variables in a rule. */
	private final Map<Var, Sort> _freeSort = new HashMap<>();
	
	/** The sorts of bound variables in a rule. */
	private final Map<Var, Sort> _boundSort = new HashMap<>();

//	/** Rules contexts collected during visitation for processing after. */
//	private final List<RuleDeclarationContext> _ruleDeclarations = new ArrayList<>();

	boolean _rulesPass;
	
	/** Current context sorts during term generation in rules. */
	private final Deque<List<Form>> _contextFormsStack = new ArrayDeque<>();
	
	// Constructor.
	
	/** Instantiate the builder. */
	private PlankBuilder() {	}

	// Methods.
	
	/** Return the generated context. */
	private Loader context() {
		return _loader;
	}
	
	// PlankVisitor...
	
	// hscript controls the two passes: first a declarations pass, then process the collected rules.

	@Override
	public Void visitHscript(HscriptContext ctx) {
		// Process declarations.
		_rulesPass = false;
		for (DeclarationContext dc : ctx.declaration())
			visit(dc);
		// Process delayed rules.
		_rulesPass = true;
		for (DeclarationContext dc : ctx.declaration())
			visit(dc);
		return null;
	}
	
	// declaration returns nothing but side effect on context.

	@Override
	public Void visitDataDeclaration(DataDeclarationContext ctx) {
		if (_rulesPass) return null;
		_varsScope.clear();
		String origin = originRange(ctx.start, ctx.stop);
		Sort sort = (Sort) visit(ctx.sort());
		String cons = ctx.CONS().getText();

		@SuppressWarnings("unchecked")
		List<Form> forms = (List<Form>) visit(ctx.forms());

		try {
			ConsForm consForm = Form.mkConsForm(origin, sort, cons, forms, false);
			_loader.addConsDeclaration(consForm);
		} catch (PlankException e) {
			_loader.addError(origin, e.getMessage());
		}
		return null;
	}

	@Override
	public Void visitSchemeDeclaration(SchemeDeclarationContext ctx) {
		if (_rulesPass) return null;
		_varsScope.clear();
		String origin = originRange(ctx.start, ctx.stop);
		Sort sort = (Sort) visit(ctx.sort());
		String cons = ctx.CONS().getText();

		@SuppressWarnings("unchecked")
		List<Form> forms = (List<Form>) visit(ctx.forms());

		try {
			ConsForm consForm = Form.mkConsForm(origin, sort, cons, forms, true);
			_loader.addConsDeclaration(consForm);
		} catch (PlankException e) {
			_loader.addError(origin, e.getMessage());
		}
		return null;
	}

	@Override
	public Void visitVariableDeclaration(VariableDeclarationContext ctx) {
		if (_rulesPass) return null;
		_varsScope.clear();

		Sort sort = (Sort) visit(ctx.sort());

		try {
			_loader.addVariableDeclaration(sort);
		} catch (PlankException e) {
			_loader.addError(originRange(ctx.start, ctx.stop), e.getMessage());
		}
		return null;
	}

	@Override
	public Void visitRuleDeclaration(RuleDeclarationContext ctx) {
		if (!_rulesPass) return null;
		_varsScope.clear();
		_freeSort.clear();
		_metaSubstSorts.clear();
		_boundSort.clear();
		_contextFormsStack.clear();

		String origin = originRange(ctx.start, ctx.stop);
		@SuppressWarnings("unchecked")
		Map<String, String> options = (Map<String, String>) visit(ctx.opts());
		Sort sort = (Sort) visit(ctx.sort());

		_contextFormsStack.push(Arrays.asList(Form.mkScopeForm(null, sort))); // sort of both terms
		_inPattern = true;
		Term patternTerm = (Term) visit(ctx.term(0));
		_inPattern = false;
		Term contractum = (Term) visit(ctx.term(1));
		_contextFormsStack.pop();

		if (patternTerm.kind() != Kind.CONS) {
			_loader.addError(origin, "rule pattern is not a construction (%s)", ctx.term(0).getText());
			return null;
		}
		Cons pattern = patternTerm.cons();
		if (!pattern.form.scheme) {
			_loader.addError(origin, "rule pattern is not a scheme construction (%s)", ctx.term(0).getText());
			return null;
		}
		try {
			_loader.addRule(origin, sort, options, pattern, contractum);
		} catch (PlankException e) {
			_loader.addError(origin, "%s", e.getMessage());
		}
		return null;
	}

	// opts is Map<String, String> (rules stage).
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> visitOpts(OptsContext ctx) {
		Map<String, String> opts = new HashMap<>();
		for (OptContext oc : ctx.opt())
			opts.putAll((Map<String, String>) visit(oc));
		return opts;
	}
	
	// opt is Map<String, String>
	
	@Override
	public Map<String, String> visitPriority(PriorityContext ctx) {
		return ImmutableMap.of("priority", ctx.PRIORITY().getText());
	}

	@Override
	public Map<String, String> visitName(NameContext ctx) {
		return ImmutableMap.of("name", ctx.CONS().getText());
	}

	// forms is List<Form> (declarations stage).
	
	@Override
	public List<Form> visitForms(FormsContext ctx) {
		List<Form> forms = new ArrayList<Form>();
		if (ctx.form() != null) {
			for (FormContext fc : ctx.form())
				forms.add((Form) visit(fc));
		}
		return forms;
	}

	// form is Form (declarations stage).
	
	@Override
	public Form visitScopeForm(ScopeFormContext ctx) {
		@SuppressWarnings("unchecked")
		List<Sort> binders = (List<Sort>) visit(ctx.bindersorts());
		Sort sort = (Sort) visit(ctx.sort());
		return Form.mkScopeForm(binders, sort);
	}

	@Override
	public Form visitAssocForm(AssocFormContext ctx) {
		Sort key = (Sort) visit(ctx.sort(0));
		Sort value = (Sort) visit(ctx.sort(1));
		return Form.mkAssocForm(key, value);
	}

	// bindersorts is List<Sort> (declarations stage)
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Sort> visitBindersorts(BindersortsContext ctx) {
		return ctx.sorts() != null ? (List<Sort>) visit(ctx.sorts()) : new ArrayList<Sort>();
	}

	// sorts is List<Sort> (declarations stage and rule stage).
	
	@Override
	public List<Sort> visitSorts(SortsContext ctx) {
		List<Sort> sorts = new ArrayList<Sort>();
		if (ctx.sort() != null) {
			for (SortContext sc : ctx.sort())
				sorts.add((Sort) visit(sc));
		}
		return sorts;
	}

	// sort is Sort (declarations stage and rule stage).
	
	@Override
	public Sort visitInstanceSort(InstanceSortContext ctx) {
		String origin = originRange(ctx.start, ctx.stop);
		String name = ctx.CONS().getText();

		@SuppressWarnings("unchecked")
		List<Sort> paramSorts = (List<Sort>) visit(ctx.sortparams());

		Sort[] paramSort = paramSorts.toArray(new Sort[paramSorts.size()]);
		Sort sort = Sort.mkSortInstance(origin, name, paramSort);
		try {
			_loader.addSort(sort);
		} catch (PlankException e) {
			_loader.addError(origin, e.getMessage());
		}
		return sort;
	}

	@Override
	public Sort visitVarSort(VarSortContext ctx) {
		String origin = originRange(ctx.start, ctx.stop);
		Var var = mkVar(ctx.VAR().getText(), true, false);
		Sort sort = Sort.mkSortVar(origin, var);
		try {
			_loader.addSort(sort);
		} catch (PlankException e) {
			_loader.addError(origin, e.getMessage());
		}
		return sort;
	}

	// sortparams is List<Sort> (declarations stage and rule stage).
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Sort> visitSortparams(SortparamsContext ctx) {
		return ctx.sorts() != null ? (List<Sort>) visit(ctx.sorts()) : new ArrayList<Sort>();
	}

	// terms is List<Term> (rule stage).
	
	@Override
	public List<Term> visitTerms(TermsContext ctx) {
		String origin = originRange(ctx.start, ctx.stop);
		List<Form> forms = _contextFormsStack.peek();
		final int size = forms.size();
		List<Term> terms = new ArrayList<Term>(size);
		if (ctx.term() == null) {
			if (size != 0)
				_loader.addError(origin, "argument list expected");
		} else {
			if (size != ctx.term().size())
				_loader.addError(origin, "argument list has wrong arity (%s)", ctx.getText());

			for (int i = 0; i < size; ++i) {
				// Every subterm gets the corresponding subsort.
				_contextFormsStack.push(Arrays.asList(forms.get(i)));
				terms.add((Term) visit(ctx.term(i))); // recurse
				_contextFormsStack.pop();
			}
		}
		return terms;
	}
	
	// term is Term (rule stage)

	public Term visitTerm(TermContext ctx) {
		// TODO: use sortanno, if present.
		return (Term) visit(ctx.rawterm());
	}

	// rawterm is Term (rule stage).

	@Override
	public Term visitConsTerm(ConsTermContext ctx) {
		assert	_contextFormsStack.peek().size() == 1 : "Panic: single form for term lost?!?";
		Form contextForm = _contextFormsStack.peek().get(0); 
		assert	!contextForm.isAssoc() : "Panic: term processed with association sort?!?";
		String origin = originRange(ctx.start, ctx.stop);
		Sort contextSort = contextForm.sort;
		String cons = ctx.CONS().getText();
		ConsForm consForm = _loader.consForm(cons);
		try {
			contextSort = _loader.unify(consForm.sort, contextSort);
			
			// Now build Form for each piece sort of consForm!
			List<Form> piecesForms = new ArrayList<>();
			final int subCount = consForm.subSort.length;
			for (int i = 0; i < subCount; ++i) {
				Form subForm = Form.mkScopeForm(Arrays.asList(consForm.binderSort[i]), consForm.subSort[i]);
				piecesForms.add(subForm);
			}
			final int assocCount = consForm.keySort.length;
			for (int i = 0; i < assocCount; ++i) {
				Form assocForm = Form.mkAssocForm(consForm.keySort[i], consForm.valueSort[i]);
				piecesForms.add(assocForm);
			}

			_contextFormsStack.push(piecesForms);
			@SuppressWarnings("unchecked")
			List<Piece> pieces = (List<Piece>) visit(ctx.pieces()); // recurse
			_contextFormsStack.pop();
			
			return Piece.mkCons(origin, contextSort, consForm, cons, pieces);
		} catch (PlankException e) {
			_loader.addError(origin, e.getMessage());
			return Term.mkOccur(origin, consForm.sort, mkVar("badterm", true, false));
		}
	}

	@Override
	public Term visitVarTerm(VarTermContext ctx) {
		assert	_contextFormsStack.peek().size() == 1 : "Panic: single form for term lost?!?";
		assert	!_contextFormsStack.peek().get(0).isAssoc() : "Panic: term processed with association sort?!?";
		String origin = originRange(ctx.start, ctx.stop);
		String name = ctx.VAR().getText();
		Var var = mkVar(name, true, false); // unknown variables are free
		Sort sortBound = _boundSort.get(var);
		Sort sortFree = _freeSort.get(var);
		if (sortBound == null && sortFree == null)
			_loader.addError(origin, "variable is not defined (%s)", name);
		Sort sort = _contextFormsStack.peek().get(0).sort;
		try {
			sort = _loader.unify(sortBound != null ? sortBound : sortFree, sort);
		} catch (PlankException e) {
			_loader.addError(origin, e.getMessage());
		}
		return Term.mkOccur(origin, sort, var);
	}

	@Override
	public Term visitMetaTerm(MetaTermContext ctx) {
		assert	_contextFormsStack.peek().size() == 1 : "Panic: single form for term lost?!?";
		assert	!_contextFormsStack.peek().get(0).isAssoc() : "Panic: term processed with association sort?!?";
		String origin = originRange(ctx.start, ctx.stop);
		String meta = ctx.META().getText();
		return metaApplication(origin, meta, ctx.terms());
	}
	/** Extract actual construction of meta-application so it can be called from elsewhere. */
	private Meta metaApplication(String origin, String meta, TermsContext termsc) {
		Sort contextSort = _contextFormsStack.peek().get(0).sort;
		final int arity = termsc .term().size();

		List<Sort> argsSorts; // set subSorts
		Sort resultSort;
		if (_inPattern) {
			// Meta-application in pattern must have only distinct already bound variables.
			argsSorts = new ArrayList<>();
			Set<Var> seen = new HashSet<>();
			if (termsc != null) {
				for (TermContext tc : termsc .term()) {
					RawtermContext rc = ((TermContext) tc).rawterm();
					Sort varSort = null;
					if (rc instanceof VarTermContext) {
						// We have a variable...
						String name = ((VarTermContext) rc).VAR().getText();
						Var var = mkVar(name, false, false);
						varSort = _boundSort.get(var);
						if (varSort == null)
							_loader.addError(originRange(tc.start, tc.stop), "parameter to pattern meta-application unbound variable (%s)", tc.getText());
						if (seen.contains(var))
							_loader.addError(originRange(tc.start, tc.stop), "parameter to pattern meta-application is duplicate bound variable (%s)", tc.getText());
						seen.add(var);
					} else {
						_loader.addError(originRange(tc.start, tc.stop), "parameter to pattern meta-application not bound variable (%s)", tc.getText());
					}
					argsSorts.add(varSort);
				}
			}
			// TODO: for repeated meta-applications (non-left-linear pattern) check that old and new subSorts are consistent.
			resultSort = contextSort;
		} else { // not _inPattern
			// Meta-application in contraction must be previously defined.
			Form subst = _metaSubstSorts.get(meta);
			if (subst == null)
				_loader.addError(origin, " meta-variable in meta-application used only in contraction (%s)", meta);
			if (subst.args.length != arity)
				_loader.addError(origin, " meta-application has inconsistent arity with previous use of meta-variable (%s)", meta);
			argsSorts = Arrays.asList(subst.args);
			try {
				resultSort = _loader.unify(contextSort, subst.sort);
			} catch (PlankException e) {
				_loader.addError(origin, e.getMessage());
				resultSort = contextSort; // recover as much as possible by using context sort
			}
		}
		_metaSubstSorts.put(meta, Form.mkScopeForm(argsSorts, resultSort)); // always update just in case sort got better

		List<Form> forms = new ArrayList<>(arity);
		for (Sort s : argsSorts)
			forms.add(Form.mkScopeForm(null, s));

		_contextFormsStack.push(forms);
		@SuppressWarnings("unchecked")
		List<Term> terms = termsc != null ? (List<Term>) visit(termsc ) : new ArrayList<Term>();
		_contextFormsStack.pop();
		
		return Term.mkMeta(origin, resultSort, meta, terms);
	}
	
	// sortanno is Sort (rule stage).

	@Override
	public Sort visitSortanno(SortannoContext ctx) {
		return ctx.sort() != null ? (Sort) visit(ctx.sort()) : Sort.mkSortVar("*", mkVar("badsort", true, false)); 
	}
	
	// pieces is List<Piece> (rule stage).

	@Override
	public List<Piece> visitPieces(PiecesContext ctx) {
		final String origin = originRange(ctx.start, ctx.stop);
		final List<Piece> pieces = new ArrayList<Piece>();
		if (ctx.piece() != null) {
			final int size = ctx.piece().size();
			List<Form> forms = _contextFormsStack.peek();
			if (size != forms.size())
				_loader.addError(origin, "argument list has wrong arity (%s)", ctx.getText());
			for (int i = 0; i < size; ++i) {
				// Every subpiece gets the corresponding form.
				Form form = forms.get(i);
				
				_contextFormsStack.push(Arrays.asList(form));
				pieces.add((Piece) visit(ctx.piece(i)));
				_contextFormsStack.pop();
				
			}
		}
		return pieces;
	}

	// piece is Piece (rule stage).
	
	@Override
	public Piece visitScopePiece(ScopePieceContext ctx) {
		assert	_contextFormsStack.peek().size() == 1 : "Panic: single form for piece lost?!?";
		assert	!_contextFormsStack.peek().get(0).isAssoc() : "Panic: scope processed with association sort?!?";
		String origin = originRange(ctx.start, ctx.stop);
		
		_varsScope.push();
		@SuppressWarnings("unchecked")
		// Note that we pass the "scope piece context sort" to both binders and term: this is safe! 
		List<Var> binders = (List<Var>) visit(ctx.binders());
		Term body = (Term) visit(ctx.term());
		_varsScope.pop();

		return Piece.mkScope(origin, binders, body);
	}

	@Override
	public Piece visitAssocPiece(AssocPieceContext ctx) {
		assert	_contextFormsStack.peek().size() == 1 : "Panic: single form for piece lost?!?";
		assert	_contextFormsStack.peek().get(0).isAssoc() : "Panic: association processed with scope sort?!?";
		return (Piece) visit(ctx.associations());
	}

	// binders is List<Var>
	
	@Override
	public List<Var> visitBinders(BindersContext ctx) {
		assert	_contextFormsStack.peek().size() == 1 : "Panic: single form for piece lost?!?";
		assert	!_contextFormsStack.peek().get(0).isAssoc() : "Panic: scope binders processed with association sort?!?";
		String origin = originRange(ctx.start, ctx.stop);
		List<Var> vars = new ArrayList<>();
		if (ctx.VAR() != null) {
			Sort[] sortBinders = _contextFormsStack.peek().get(0).args;
			final int size = ctx.VAR().size();
			if (size != sortBinders.length)
				_loader.addError(origin, "binders have arity different from declaration (%s)", ctx.getText());

			Set<String> seen = new HashSet<>(size);
			for (int i = 0; i < size; ++i) {
				String name = ctx.VAR(i).getText();
				if (seen.contains(name))
					_loader.addError(origin, "duplicate binder in scope (%s)", name);
				seen.add(name);
				Var var = mkVar(name, false, true);
				vars.add(var);
				_boundSort.put(var, sortBinders[i]);
			}
		}
		return vars;
	}

	// associations is Piece (rule stage).
	
	@Override
	public Piece visitAssociations(AssociationsContext ctx) {
		String origin = originRange(ctx.start, ctx.stop);
		List<Piece> pieces = new ArrayList<Piece>();
		if (ctx.association() != null) {
			for (AssociationContext ac : ctx.association())
				pieces.add((Piece) visit(ac));
		}
		return Piece.mkAssoc(origin, pieces);
	}

	// association is Piece (rule stage).
	
	@Override
	public Piece visitMapAssoc(MapAssocContext ctx) {
		assert	_contextFormsStack.peek().size() == 1 : "Panic: single form for piece lost?!?";
		assert	_contextFormsStack.peek().get(0).isAssoc() : "Panic: association processed with scope sort?!?";
		assert	_contextFormsStack.peek().size() == 1 : "Panic: single form for piece lost?!?";
		String origin = originRange(ctx.start, ctx.stop);
		String name = ctx.VAR().getText();
		Var var = mkVar(name, true, false);
		// TODO: check that key variables occur elsewhere in term.
		Form form = _contextFormsStack.peek().get(0);
		updateAssocKeySort(origin, var, form.key); // make sure key variable has a sort
		// Process term with valueSort.
		Sort valueSort = form.sort;
		_contextFormsStack.push(Arrays.asList(Form.mkScopeForm(null, valueSort)));
		Term term = (Term) visit(ctx.term());
		_contextFormsStack.pop();
		Map<Var, Term> map = new HashMap<>();
		map.put(var, term);
		return Piece.mkAssoc(origin, map, null, null);
	}

	@Override
	public Piece visitNotAssoc(NotAssocContext ctx) {
		assert	_contextFormsStack.peek().size() == 1 : "Panic: single form for piece lost?!?";
		assert	_contextFormsStack.peek().get(0).isAssoc() : "Panic: association processed with scope sort?!?";
		String origin = originRange(ctx.start, ctx.stop);
		String name = ctx.VAR().getText();
		Var var = mkVar(name, true, false); // if undefined define as free!
		// TODO: check that key variables occur elsewhere in term.
		updateAssocKeySort(origin, var, _contextFormsStack.peek().get(0).key); // make sure key variable has a sort
		Set<Var> omit = new HashSet<>();
		omit.add(var);
		return Piece.mkAssoc(origin, null, omit, null);
	}

	@Override
	public Object visitAllAssoc(AllAssocContext ctx) {
		assert	_contextFormsStack.peek().size() == 1 : "Panic: single form for piece lost?!?";
		assert	_contextFormsStack.peek().get(0).isAssoc() : "Panic: association processed with scope sort?!?";
		String origin = originRange(ctx.start, ctx.stop);
		String meta = ctx.META().getText();
		Meta term = metaApplication(origin, meta, ctx.terms());
		return Piece.mkAssoc(origin, null, null, Arrays.asList(term));
	}

	// Utilities.
	
	/**
	 * Create string suitable for use as 'origin' string from ANTLR token range.
	 * @param start of range
	 * @param stop of range
	 */
	private static String originRange(Token start, Token stop) {
		int startLine = start.getLine();
		int stopLine = stop.getLine();
		int startPos = start.getCharPositionInLine();
		int stopPos = stop.getCharPositionInLine();
		if (startLine == stopLine) 
			return "line " + startLine + " pos " + (startPos == stopPos ? "" + startPos : "" + startPos + "-" + stopPos);
		else
			return "line " + startLine + " pos " + startPos + " - line " + stopLine + " pos " + stopPos; 
	}

	/**
	 * Lookup variable or create fresh, as needed. 
	 * @param name base of name to use
	 * @param global whether variable should be declared in the global scope (or the most local)
	 * @param force whether an existing variable definition should be ignored
	 */
	private Var mkVar(String name, boolean global, boolean force) {
		Var v = _varsScope.get(name);
		if (v == null || force) {
			if (global)
				_varsScope.putGlobal(name,v = new Var(name));  
			else
				_varsScope.put(name,  v = new Var(name));
		}
		return v;
	}

	/**
	 * Helper to update the sort of a variable.
	 * @param origin of variable
	 * @param var the variable to update sort information for
	 * @param keySort context-imposed key sort for association
	 */
	private void updateAssocKeySort(String origin, Var var, Sort keySort) {
		try {
			Sort freeSort = _freeSort.get(var);
			Sort boundSort = _boundSort.get(var);
			Sort varSort; // the real sort we'll be using for the variable
			if (boundSort != null) {
				varSort = _loader.unify(boundSort, keySort);
				_boundSort.put(var, varSort); // possibly improve the type
			} else {
				if (freeSort != null) {
					varSort = _loader.unify(freeSort,  keySort);
				} else {
					varSort = keySort;
				}
				_freeSort.put(var, varSort); // possibly improve the type
			}
		} catch (PlankException e) {
			_loader.addError(origin, e.getMessage());
		}
	}
}