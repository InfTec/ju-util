package ch.inftec.ju.util;

import java.util.Set;

/**
 * Interface for classes that evaluate properties based on keys.
 * <p>
 * Used by the PropertyChainBuilder class.
 * @author Martin
 *
 */
public interface PropertyEvaluator {
	/**
	 * Gets the property for the specified key. If the property isn't defined,
	 * null is returned.
	 * @param key Key name
	 * @return Property value or null if the property is not defined
	 */
	Object get(String key);
	
	/**
	 * Lists all keys in the evaluator, as far as this is possible.
	 * <p>
	 * If an evaluator cannot list an infinite number of keys, it may return null.
	 * @return Set of keys or null if the evaluator doesn't list keys
	 */
	Set<String> listKeys();
}
