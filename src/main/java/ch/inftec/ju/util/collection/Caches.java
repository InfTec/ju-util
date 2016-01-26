package ch.inftec.ju.util.collection;

import ch.inftec.ju.util.function.Function;

/**
 * Factory class to create Cache instances.
 *
 */
public class Caches {
	/**
	 * Returns a simpel, thread safe Cache implementation with a maximum size and an getter function to create values for keys not
	 * available in the cache yet / anymore.
	 * @param maxSize Maximum size of the cache. If reached, the longest unused value will be removed
	 * @param unknownValueGetter Function to create values for keys not stored in the Cache
	 * @param <K> Key type
	 * @param <V> Value type
	 * @return Cache implementation
	 */
	public static <K, V> Cache<K, V> simpleBoundedCache(int maxSize, Function<K, V> unknownValueGetter) {
		return new SimpleCache<>(maxSize, unknownValueGetter);
	}
}
