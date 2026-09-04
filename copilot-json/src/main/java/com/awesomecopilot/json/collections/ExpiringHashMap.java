package com.awesomecopilot.json.collections;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A thread-safe implementation of a HashMap whose entries expire after the specified
 * life time. The life-time can be defined on a per-key basis, or using a default one,
 * that is passed to the constructor.
 *
 * <p>
 * Entries are lazily evicted: an expired entry is removed the next time it is touched,
 * or when {@link #size()}/{@link #isEmpty()}/{@link #containsValue(Object)} performs a
 * purge. Accessing a key ({@link #get(Object)}) renews its life time (sliding expiration),
 * which matches the original behavior.
 *
 * @author Pierantonio Cangianiello
 * @param <K> the Key type
 * @param <V> the Value type
 */
public class ExpiringHashMap<K, V> implements ExpiringMap<K, V> {

	private final ConcurrentHashMap<K, ExpiringEntry<V>> internalMap;

	/**
	 * The default max life time in milliseconds.
	 */
	private final long maxLifeTimeMillis;

	public ExpiringHashMap() {
		this(Long.MAX_VALUE);
	}

	public ExpiringHashMap(long defaultMaxLifeTimeMillis) {
		this(defaultMaxLifeTimeMillis, 16);
	}

	public ExpiringHashMap(long defaultMaxLifeTimeMillis, int initialCapacity) {
		this(defaultMaxLifeTimeMillis, initialCapacity, 0.75f);
	}

	public ExpiringHashMap(long defaultMaxLifeTimeMillis, int initialCapacity, float loadFactor) {
		this.internalMap = new ConcurrentHashMap<K, ExpiringEntry<V>>(initialCapacity, loadFactor);
		this.maxLifeTimeMillis = defaultMaxLifeTimeMillis;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		purgeExpired();
		return internalMap.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		purgeExpired();
		return internalMap.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsKey(Object key) {
		return getLive((K) key) != null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsValue(Object value) {
		purgeExpired();
		for (ExpiringEntry<V> entry : internalMap.values()) {
			if (Objects.equals(entry.value, value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public V get(Object key) {
		ExpiringEntry<V> entry = getLive((K) key);
		if (entry == null) {
			return null;
		}
		entry.renew(currentTimeMillis());
		return entry.value;
	}

	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		return computeIfAbsent(key, mappingFunction, maxLifeTimeMillis, TimeUnit.MILLISECONDS);
	}

	@Override
	public V computeIfAbsent(K key,
			Function<? super K, ? extends V> mappingFunction,
			long lifeTimeMillis,
			TimeUnit timeUnit) {
		long lifeTime = timeUnit.toMillis(lifeTimeMillis);
		ExpiringEntry<V> entry = internalMap.compute(key, (k, existing) -> {
			long now = currentTimeMillis();
			if (existing != null && !existing.isExpired(now)) {
				existing.renew(now);
				return existing;
			}
			V value = mappingFunction.apply(k);
			return new ExpiringEntry<V>(value, lifeTime, now);
		});
		return entry == null ? null : entry.value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V put(K key, V value) {
		return this.put(key, value, maxLifeTimeMillis);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V put(K key, V value, long lifeTimeMillis) {
		ExpiringEntry<V> newEntry = new ExpiringEntry<V>(value, lifeTimeMillis, currentTimeMillis());
		ExpiringEntry<V> oldEntry = internalMap.put(key, newEntry);
		return oldEntry == null ? null : oldEntry.value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V put(K key, V value, long lifeTimeMillis, TimeUnit timeUnit) {
		return put(key, value, timeUnit.toMillis(lifeTimeMillis));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V remove(Object key) {
		ExpiringEntry<V> entry = internalMap.remove(key);
		return entry == null ? null : entry.value;
	}

	/**
	 * Not supported.
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean renewKey(K key) {
		ExpiringEntry<V> entry = getLive(key);
		if (entry == null) {
			return false;
		}
		entry.renew(currentTimeMillis());
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		internalMap.clear();
	}

	/**
	 * Not supported.
	 */
	@Override
	public Set<K> keySet() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Not supported.
	 */
	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Not supported.
	 */
	@Override
	public Set<Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the entry for the given key if it is still alive, otherwise removes the
	 * expired entry and returns {@code null}.
	 */
	private ExpiringEntry<V> getLive(K key) {
		ExpiringEntry<V> entry = internalMap.get(key);
		if (entry == null) {
			return null;
		}
		if (entry.isExpired(currentTimeMillis())) {
			internalMap.remove(key, entry);
			return null;
		}
		return entry;
	}

	/**
	 * Removes all expired entries. O(n) in the number of entries.
	 */
	private void purgeExpired() {
		long now = currentTimeMillis();
		for (Entry<K, ExpiringEntry<V>> mapEntry : internalMap.entrySet()) {
			if (mapEntry.getValue().isExpired(now)) {
				internalMap.remove(mapEntry.getKey(), mapEntry.getValue());
			}
		}
	}

	private static long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	private static long computeExpireAt(long nowMillis, long lifeTimeMillis) {
		if (lifeTimeMillis >= Long.MAX_VALUE - nowMillis) {
			return Long.MAX_VALUE;
		}
		return nowMillis + lifeTimeMillis;
	}

	private static final class ExpiringEntry<V> {
		private final V value;
		private final long lifeTimeMillis;
		private volatile long expireAtMillis;

		private ExpiringEntry(V value, long lifeTimeMillis, long nowMillis) {
			this.value = value;
			this.lifeTimeMillis = lifeTimeMillis;
			this.expireAtMillis = computeExpireAt(nowMillis, lifeTimeMillis);
		}

		private boolean isExpired(long nowMillis) {
			return nowMillis >= expireAtMillis;
		}

		private void renew(long nowMillis) {
			expireAtMillis = computeExpireAt(nowMillis, lifeTimeMillis);
		}
	}

}
