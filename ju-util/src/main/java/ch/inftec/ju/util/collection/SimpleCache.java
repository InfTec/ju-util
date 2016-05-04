package ch.inftec.ju.util.collection;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ch.inftec.ju.util.function.Function;

/**
 * Simple implementation of the Cache interface.
 * <p/>
 * This class is thread safe.
 * <p/>
 * For heavy use, it is recommended to use a 'professional' Cache implementation, e.g. from Guava library.
 */
class SimpleCache<K, V> implements Cache<K, V> {
	private final int maxSize;
	private final Function<K, V> unknownValueGetter;

	private final Map<K, V> items = new LinkedHashMap<>();

	/**
	 * Creates a new Cache with the specified max size and an unknown value getter function. If Cache.get is called with an unknown key,
	 * the function is used to create it.
	 * <p/>
	 * If max size is reached, the value longest not used will be discarded after a new value was added.
	 * @param maxSize Maximum size of the Cache
	 * @param unknownValueGetter Function to retrieve unknown items when get is called
	 */
	SimpleCache(int maxSize, Function<K, V> unknownValueGetter) {
		Validate.isTrue(maxSize > 0, "maxSize must be greater than 0, but was %d", maxSize);
		Validate.notNull(unknownValueGetter, "unknownValueGetter must be specified");

		this.maxSize = maxSize;
		this.unknownValueGetter = unknownValueGetter;
	}

	@Override
	public synchronized V get(K key) {
		if (!items.containsKey(key)) {
			V value = unknownValueGetter.apply(key);
			items.put(key, value);
		} else {
			// Re-add the value again to move it to the end of the linked hashmap
			// This is not terribly performing, but meats the requirements of the Cache
			V value = items.get(key);
			items.remove(key);
			items.put(key, value);
		}

		truncateCache();

		return items.get(key);
	}

	@Override
	public synchronized void put(K key, V value) {
		items.put(key, value);

		truncateCache();
	}

	@Override
	public synchronized long size() {
		return items.size();
	}

	private synchronized void truncateCache() {
		while (items.size() > maxSize) {
			K oldestKey = items.keySet().iterator().next();

			items.remove(oldestKey);
		}
	}
}
