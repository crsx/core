/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CharStream;
import org.crsx.plank.loader.Loader;
import org.crsx.plank.loader.PlankBuilder;

/**
 * Main program to run a plank script.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public class Plank {

	/**
	 * Run each script file in the command line.
	 * @param args names of script files
	 */
	public static void main(String[] args) {
		for (String arg : args) {
			try {
				CharStream input = new ANTLRFileStream(arg);
				Loader loader = PlankBuilder.parse(input);
				
				System.out.print(loader.toString());

				// TODO: also execute...
				
//			} catch (PlankException pe) {
//				System.err.println("Plank execution error in file " + arg + fnfe.getMessage());
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}
}