package ch.inftec.ju.util;

import java.util.Set;

/**
 * Interface to access properties that can be arranged and priorized in a chain using the
 * PropertyChainBuilder.
 * <p>
 * If a property has the value null, it is considered undefined.
 * <p>
 * The PropertyChain interface provides getter methods for String and for generic types. Support for 
 * generic type conversion support may very depending on the implementation of the property chain.
 * 
 * @author Martin
 *
 */
public interface PropertyChain {
	/**
	 * Gets the property for the specified key
	 * @param key Key name
	 * @return Property or null if none is defined
	 */
	String get(String key);
	
	/**
	 * Gets the property with the specified key throwing an exception
	 * if it is not defined.
	 * @param key Key name
	 * @param throwExceptionIfNotDefined
	 * @return Property value
	 * @throws JuRuntimeException If the property doesn't exist
	 */
	String get(String key, boolean throwExceptionIfNotDefined);
	
	/**
	 * Gets the property with the specified key. If it doesn't exist,
	 * the default value is returned.
	 * @param key Key name
	 * @param defaultValue Value to return if the property doesn't exist
	 * @return Property value or default value if it doesn't exist
	 */
	String get(String key, String defaultValue);
	
	/**
	 * Gets the property for the specified key
	 * @param key Key name
	 * @return Property or null if none is defined
	 */
	<T> T get(String key, Class<T> clazz);
	
	/**
	 * Gets the property with the specified key throwing an exception
	 * if it is not defined.
	 * @param key Key name
	 * @param throwExceptionIfNotDefined
	 * @return Property value
	 * @throws JuRuntimeException If the property doesn't exist
	 */
	<T> T get(String key, Class<T> clazz, boolean throwExceptionIfNotDefined);
	
	/**
	 * Gets the property with the specified key. If it doesn't exist,
	 * the default value is returned.
	 * @param key Key name
	 * @param defaultValue Value to return if the property doesn't exist
	 * @return Property value or default value if it doesn't exist
	 */
	<T> T get(String key, Class<T> clazz, String defaultValue);
	
	/**
	 * Returns a list of all keys defined by this chain.
	 * <p>
	 * Note that not all implementations of Property evaluators might be able to return a finite
	 * list of their keys. In this case, they may chose to return no keys at all.
	 * @return Set of keys as far as they are available
	 */
	Set<String> listKeys();
	
	/**
	 * Gets the property info for the specified key.
	 * @param key Key
	 * @return PropertyInfo or null if the property is not defined
	 */
	PropertyInfo getInfo(String key);
	
	/**
	 * Detialed property information
	 * @author Martin Meyer <martin.meyer@inftec.ch>
	 *
	 */
	public interface PropertyInfo {
		/**
		 * Key that yielded the property
		 * @return Key name
		 */
		String getKey();
		
		/**
		 * Actual value of the property, after decryption, interpolation etc.
		 * @return
		 */
		String getValue();
		
		/**
		 * Display value of the property, i.e. the value that should be displayed in logs and the like.
		 * <p>
		 * This can be used for encrypted values / passwords to avoid printing the actual values in logs
		 * @return Display value of the property
		 */
		String getDisplayValue();
		
		/**
		 * Gets the raw value of the property, i.e. the value before decryption, interpolation etc.
		 * @return
		 */
		String getRawValue();
		
		/**
		 * Gets whether the property contains sensitive data.
		 * <p>
		 * For sensitive data, the DisplayValue should be used for logging.
		 * @return Whether the property contains sensitive data like encrypted data
		 */
		boolean isSensitive();
		
		/**
		 * Gets info about the evalutor that yielded the property.
		 * <p>
		 * The info string will be implementation dependent and should only be used for
		 * displaying purposes
		 * @return Evaluator info
		 */
		String getEvaluatorInfo();
	}
}
