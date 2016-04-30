/*
 * Copyright © 2016  Kristoffer H. Rose <krisrose@crsx.org>
 * Available under the Apache 2.0 license.
 */
package org.crsx.plank.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple layered map (where scoped declarations can be forgotten).
 * @param <K> type of keys
 * @param <V> type of valuesXS
 * @author Kristoffer H. Rose <krisrose@crsx.org>
 */
public class LayeredMap<K,V> {

	// State.
	
	/** The stack of scopes. */
	final private List<Map<K,V>> _stack = new ArrayList<>();
	
	// Constructor.
	
	/**
	 * Create map with just an outermost scope. 
	 */
	public LayeredMap() {
		push();
	}
	
	// Methods.

	/** Create a new scope! */
	public void push() {
		_stack.add(new HashMap<>());
	}
	
	/** Pop the latest scope! */
	public void pop() {
		assert _stack.size() > 1 : "Panic: trying to remove last scope?!?";
		_stack.remove(_stack.size() - 1);
	}
	
	/** Clear just the top scope. */
	public void clearTop() {
		assert _stack.size() > 1 : "Panic: trying to remove last scope?!?";
		_stack.get(_stack.size() - 1).clear();
	}

	/** Completely clear the map, initializing back to just an outermost scope. */
	public void clear() {
		_stack.clear();
		push();
	}

	/** Check if a key is present. */
	public boolean containsKey(K key) {
		assert _stack.size() > 0 : "Panic: empty layered map?!?";
		for (int i = _stack.size() - 1; i >= 0; --i) {
			if (_stack.get(i).containsKey(key))
				return true;
		}
		return false;
	}
	
	/** Get the closest value corresponding to key. */
	public V get(K key) {
		assert _stack.size() > 0 : "Panic: empty layered map?!?";
		for (int i = _stack.size() - 1; i >= 0; --i) {
			V value = _stack.get(i).get(key);
			 if (value != null)
				 return value;
		}
		return null;
	}
	
	/** Update the value for a key in the top scope. */
	public void put(K key, V value) {
		assert _stack.size() > 0 : "Panic: empty layered map?!?";
		_stack.get(_stack.size() - 1).put(key, value);
	}
	
	/** Update the value for a key in the bottom (initial) scope. */
	public void putGlobal(K key, V value) {
		assert _stack.size() > 0 : "Panic: empty layered map?!?";
		_stack.get(0).put(key, value);
	}
}