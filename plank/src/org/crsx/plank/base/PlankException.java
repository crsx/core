/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.base;

/**
 * A problem has occurred during processing of the plank script.
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
@SuppressWarnings("serial")
public final class PlankException extends Exception {

	/** Create exception with message. */
	public PlankException(String format, Object... args) {
		super(args.length == 0 ? format : String.format(format, args));
	}

	/** Create exception with message and the original cause. */
	public PlankException(Throwable cause, String format, Object... args) {
		super(args.length == 0 ? format : String.format(format, args), cause);
	}

	/** Create exception with just the original cause. */
	public PlankException(Throwable cause) {
		super(cause);
	}

//	// Avoid printing stack traces?
//	public synchronized Throwable fillInStackTrace()  { return this; }
}
