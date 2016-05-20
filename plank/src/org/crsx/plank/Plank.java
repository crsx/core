/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.crsx.plank.base.PlankException;
import org.crsx.plank.execute.Executable;
import org.crsx.plank.loader.Loader;
import org.crsx.plank.loader.PlankBuilder;
import org.crsx.plank.term.Term;

/**
 * Main program to run a plank script.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public class Plank {

	/** Help. */
	static String USAGE = "Usage: Plank [--show-{script,inputs,sorts,parses}] scriptfile [termfile...]";
	
	/**
	 * Run script on inputs.
	 * @param args first argument should be plank script;
	 * 	rest of the arguments are input files, or if none, the standard input is processed
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("USAGE");
			System.exit(0);
		}
		
		// Process options and arguments.
		String scriptFile = null;
		List<String> termFiles = new ArrayList<>();
		boolean showScript = false;
		boolean showInputs = false;
		boolean showSorts = false;
		boolean traceParse = false;
		for (String arg : args) {
			if (arg.startsWith("-")) {
				switch (arg) {
				case "--show-script" :
					showScript = true;
					break;
				case "--show-inputs" :
					showInputs = true;
					break;
				case "--show-sorts" :
					showSorts = true;
					break;
				case "--show-parses" :
					traceParse = true;
					break;
				default :
					System.err.println("Unknown option (" + arg + ")\n" + USAGE);
					System.exit(1);
				}
			} else if (scriptFile == null) {
				scriptFile = arg;
			} else {
				termFiles.add(arg);
			}
		}

		try {
			PlankBuilder builder = new PlankBuilder();
			
			// Parse and load script.
			CharStream scriptStream = new ANTLRFileStream(scriptFile);
			Loader loader = builder.parseScript(scriptStream, traceParse);
			if (loader.hasErrors()) {
				loader.appendErrors(System.err);
				System.exit(1);
			}
			if (showScript) {
				System.out.println("/* LOADED SCRIPT: */\n");
				System.out.print(loader.toString());
			}
			
			// Process each input term.
			if (termFiles.isEmpty()) {
				CharStream termStream = new ANTLRInputStream(System.in);
				parseAndEvaluate(System.out, builder, loader, termStream, showInputs, showSorts, traceParse);
			} else {
				for (String termFile : termFiles) {
					CharStream termStream = new ANTLRFileStream(termFile);
					parseAndEvaluate(System.out, builder, loader, termStream, showInputs, showSorts, traceParse);
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Parse and use the script rules to evaluate a term.
	 * @param out where to send evaluated term to (errors go to standard error).
	 * @param builder that was used for parsing
	 * @param loader 
	 * @param termStream
	 * @param showInputs
	 * @param showSorts
	 * @param traceParse
	 * @throws PlankException
	 * @throws IOException 
	 */
	private static void parseAndEvaluate(Appendable out, PlankBuilder builder, Loader loader, CharStream termStream, boolean showInputs, boolean showSorts, boolean traceParse) throws PlankException, IOException {
		Term term = builder.parseTerm(termStream, traceParse); // note: side effects on loader! Ugly.
		if (loader.hasErrors()) {
			loader.appendErrors(System.err);
			System.exit(1);
		}
		if (showInputs) {
			out.append("\n/* INPUT */\n");
			term.appendTerm(out, "\n", new HashMap<>(), showSorts);
			out.append("\n\n/* OUTPUT */\n");
		}
		Executable executor = loader.executable();
		Term nf = executor.normalize(term);
		nf.appendTerm(out, "\n  ", new HashMap<>(), showSorts);
		out.append("\n");
	}
}