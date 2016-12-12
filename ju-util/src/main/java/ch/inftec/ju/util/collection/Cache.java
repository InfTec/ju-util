package ch.inftec.ju.util.collection;

/**
 * Simple interface for a Cache.
 * <p>
 * This acts as a simplified replacement for real use caches like Google Guava AbstractCache.
 */
public interface Cache<K, V> {
	V get(K key);
	void put(K key, V value);
	long size();
}
