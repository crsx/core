/*
 * Copyright © 2016 Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.base;

/**
 * Any artifact with an origin string.
 * @author krisrose
 */
public class Origined {
	
	private final String _origin;

	/**
	 * Assign origin.
	 * An origin string has a 
	 * @param origin location information as explained above
	 */
	public Origined(String origin) {
		_origin = origin;
	}
	
	/** The origin. */
	public final String origin() {
		return _origin;
	}

	/**
	 * Combine several origins into a single origin message.
	 * @param format format string
	 * @param origins the component origin messages
	 * @return new origin string
	 */
	public static String combine(String format, Origined... origins) {
		String[] enclosedOrigins = new String[origins.length];
		for (int i = 0; i < origins.length; ++i)
			enclosedOrigins[i] = "{" + origins[i].origin() + "}";
		return String.format(format, (Object[]) enclosedOrigins);
	}
}