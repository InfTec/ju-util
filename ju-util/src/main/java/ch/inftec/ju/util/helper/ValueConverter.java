package ch.inftec.ju.util.helper;

/**
 * Helper interface that can be used whenever values can be returned in different formats by a single method.
 * @author martin.meyer@inftec.ch
 *
 */
public interface ValueConverter {
	/**
	 * Gets the value as an Object, unconverted.
	 * @return Object without any conversion
	 */
	Object get();
	
	/**
	 * Gets the object casted or converted to the specified type.
	 * @param clazz Return value type
	 * @return Value casted or converted to T
	 */
	<T> T get(Class<T> clazz);
}
