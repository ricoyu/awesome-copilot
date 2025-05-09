package com.awesomecopilot.json.collections;
/*
 * Copyright (c) 2017 Pierantonio Cangianiello
 * 
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 *
 * @author Pierantonio Cangianiello
 * @param <K> the Key type
 * @param <V> the Value type
 */
public interface ExpiringMap<K, V> extends Map<K, V> {

	/**
	 * Renews the specified key, setting the life time to the initial value.
	 *
	 * @param key
	 * @return true if the key is found, false otherwise
	 */
	public boolean renewKey(K key);

	/**
	 * Associates the given key to the given value in this map, with the specified
	 * life times in milliseconds.
	 *
	 * @param key
	 * @param value
	 * @param lifeTimeMillis
	 * @return a previously associated object for the given key (if exists).
	 */
	public V put(K key, V value, long lifeTimeMillis);
	
	public V put(K key, V value, long ttl, TimeUnit timeUnit);

	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);
	
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction, long lifeTimeMillis, TimeUnit timeUnit);
}