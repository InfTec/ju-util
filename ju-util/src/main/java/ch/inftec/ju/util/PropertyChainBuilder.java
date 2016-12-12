package ch.inftec.ju.util;

import java.io.BufferedReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.inftec.ju.security.JuSecurityUtils;
import ch.inftec.ju.security.JuTextEncryptor;
import ch.inftec.ju.util.PropertyChain.PropertyInfo;

/**
 * Builder to create PropertyChain instances.
 * <p>
 * A PropertyChain can evaluate properties using multiple PropertyEvaluators that can
 * be arranged in a chain, priorizing the property evaluation by the order they are added to
 * the chain.
 * <p>
 * The PropertyChain can also be configured to allow interpolation, e.g. to replace ${name} with the value
 * of the property 'name'.
 * @author Martin
 *
 */
public class PropertyChainBuilder {
	private Logger logger = LoggerFactory.getLogger(PropertyChainBuilder.class);
	
	/**
	 * List of folders on file system we will search for resources.
	 */
	private final List<Path> resourceFolders = new ArrayList<>();
	
	private final List<PropertyEvaluator> evaluators = new ArrayList<>();
	
	private InterpolationBuilder interpolationBuilder = new InterpolationBuilder();
	
	/**
	 * Set of values we shouldn't display values of in log (e.g. sensitive data like
	 * passwords).
	 */
	private final Set<String> hiddenValueKeys = new HashSet<>();
	
	/**
	 * String that will be logged for an encrypted value.
	 */
	private static final String ENCRYPTED_VALUE_LOGGING_STRING = "***";
	
	/**
	 * TextEncryptor instance to decrypt encrypted texts.
	 */
	private JuTextEncryptor decryptor;
	
	// Attributes of the PropertyChain
	private boolean defaultThrowExceptionIfUndefined = false;
	
	/**
	 * Specifies that the value for this key should not be displayed plainly. Can be used
	 * to protect sensitive data like passwords.
	 * @param key Key
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder hideValueForKey(String key) {
		this.hiddenValueKeys.add(key);
		return this;
	}
	
	/**
	 * Adds an evaluator that evaluates system properties.
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder addSystemPropertyEvaluator() {
		return this.addPropertyEvaluator(new SystemPropertyEvaluator());
	}
	
	/**
	 * Adds a resource folder we will use for resource lookup.
	 * <p>
	 * Note that the folder must be added BEFORE the corresponding resource is configured using
	 * the builder.
	 * <p>
	 * All resource folders will be scanned before we look for the resource on the classpath
	 * <p>
	 * If the folder doesn't exist or is not a folder, it will be ignored issuing a warning log.
	 * @param resourceFolder Resource folder to look for resource on file system
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder addResourceFolder(Path resourceFolder) {
		if (JuUrl.path().isExistingDirectory(resourceFolder)) {
			this.resourceFolders.add(resourceFolder);
		} else {
			logger.warn("Not an existing directory. Folder will be ignored: {}", resourceFolder.toAbsolutePath());
		}
		
		return this;
	}
	
	/**
	 * Adds an evaluator that reads properties from property files.
	 * @param resourceUrl URL to property file resource
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder addResourcePropertyEvaluator(URL resourceUrl) {
		try {
			return this.addPropertyEvaluator(new PropertiesPropertyEvaluator(resourceUrl));
		} catch (JuException ex) {
			throw new JuRuntimeException("Couldn't load properties from url " + resourceUrl, ex);
		}
	}
	
	/**
	 * Adds an evaluator that reads properties from a property file.
	 * @param resourceName Name of the resource
	 * @param ignoreMissingResource If true, no evalautor will be added and no exception will
	 * be thrown if the resource doesn't exist
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder addResourcePropertyEvaluator(String resourceName, boolean ignoreMissingResource) {
		try {
			URL resourceUrl = JuUrl.resource(resourceName);
			return this.addPropertyEvaluator(new PropertiesPropertyEvaluator(resourceUrl));
		} catch (JuException ex) {
			if (ignoreMissingResource) {
				logger.debug(String.format("Ignoring missing resource %s (Exception: %s)", resourceName, ex.getMessage()));
				return this;
			} else {
				throw new JuRuntimeException("Couldn't load properties from resource " + resourceName, ex);
			}
		}
	}
	
	/**
	 * Adds an evaluator that reads properties from the specified Properties object.
	 * @param props Properties instance
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder addPropertiesPropertyEvaluator(Properties props) {
		return this.addPropertyEvaluator(new PropertiesPropertyEvaluator(props));
	}
	
	/**
	 * Adds an evaluator that will evalute from the static list of key/value pairs provided.
	 * <p>
	 * Each even index is a key and each odd keyIndex+1 is it's value.
	 * @param keyValuePairs List of key value pairs
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder addListPropertyEvaluator(String... keyValuePairs) {
		Properties props = new Properties();
		
		for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
			props.put(keyValuePairs[i], keyValuePairs[i + 1]);
		}
		
		return this.addPropertiesPropertyEvaluator(props);
	}
	
	/**
	 * Adds a PropertyChain as nested PropertyEvaluator.
	 * @param chain PropertyChain
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder addPropertyChainPropertyEvaluator(PropertyChain chain) {
		return this.addPropertyEvaluator(new PropertyChainPropertyEvaluator(chain));
	}
	/**
	 * Adds an evaluator that reads properties from a CSV (comma separated value) file.
	 * <p>
	 * The profile name will map to the column we want to use.
	 * @param resourceUrl Resource URL to the csv resource
	 * @param profileName Name of the profile (i.e. column identified by it's header / first row) to be used
	 * @param defaultColumnName Name of the column that contains default values if a value is not defined in the profile column
	 * @return
	 */
	public PropertyChainBuilder addCsvPropertyEvaluator(URL resourceUrl, String profileName, String defaultColumnName) {
		CsvPropertyEvaluator csvEvaluator = new CsvPropertyEvaluator(resourceUrl, profileName, defaultColumnName);
		return this.addPropertyEvaluator(csvEvaluator);
	}
	
	/**
	 * Adds a custom implementation of a PropertyEvaluator.
	 * @param evaluator PropertyEvaluator implementation
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder addPropertyEvaluator(PropertyEvaluator evaluator) {
		this.evaluators.add(evaluator);
		return this;
	}

	/**
	 * Sets the default exception throwing behaviour if a property is undefined.
	 * <p>
	 * Initial value is false, i.e. no exceptions are thrown if a property is undefined and null
	 * is returned.
	 * @param defaultThrowExceptionIfUndefined True if by default, an exception should be thrown if a property is undefined
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder setDefaultThrowExceptionIfUndefined(boolean defaultThrowExceptionIfUndefined) {
		this.defaultThrowExceptionIfUndefined = defaultThrowExceptionIfUndefined;
		return this;
	}
	
	/**
	 * Sets a TextEncryptor to decrypt encrypted text like ENC(xxxx).
	 * <p>
	 * Use the {@link ch.inftec.ju.security.JuSecurityUtils} to build an encryptor.
	 * @param decryptor TextEncryptor
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder setDecryptor(JuTextEncryptor decryptor) {
		this.decryptor = decryptor;
		return this;
	}
	
	/**
	 * Sets a decryptor for encrypted text like ENC(xxxx) by resolving the password from a resource.
	 * <p>
	 * The resource will be lookup up using the chain builders resource folders first and then the classpath.
	 * @param passwordResourceName Resource name containing the encryption password
	 * @return This builder to allow for chaining
	 */
	public PropertyChainBuilder setDecryptorByResource(String passwordResourceName, boolean strongEncryption) {
		URL resourceUrl = this.getFileResource(passwordResourceName, false);
		this.setDecryptor(JuSecurityUtils.buildEncryptor()
				.passwordByUrl(resourceUrl)
				.strong(strongEncryption)
				.createTextEncryptor());
		
		return this;
	}
	
	/**
	 * Returns an InterpolationBuilder to configure interpolation.
	 * <p>
	 * By default, interpolation is turned on.
	 * @return InterpolationBuilder
	 */
	public InterpolationBuilder interpolation() {
		return this.interpolationBuilder;
	}
	
	/**
	 * Gets a PropertyChain to peek at properties, i.e. evaluate them using the chain as configured so far.
	 * <p>
	 * Note that this method will have the throwExceptionIfUndefined property set to false, regardless of the setting of
	 * <i>throwExceptionIfUndefined</i> of the builder.
	 * @return PropertyChain to peek at properties
	 */
	public InterpolatingPropertyChain peekChain() {
		return new PropertyChainImpl(this, false);
	}
	
	/**
	 * Returns a ChainFilesResolver to add evaluators by chain files that contain
	 * chain informations.
	 * <p>
	 * See <code>ju-util/ju.properties.files</code> for a reference on chain files. 
	 * @return ChainFilesResolver
	 */
	public ChainFilesResolver addEvaluatorsByChainFiles() {
		return new ChainFilesResolver();
	}
	
	/**
	 * Gets the PropertyChain that was built using this builder.
	 * <p>
	 * This property chain will support interpolation (though it has to be enabled
	 * to actually interpolate...
	 * @return PropertyChain instance
	 */
	public InterpolatingPropertyChain getPropertyChain() {
		return new PropertyChainImpl(this, this.defaultThrowExceptionIfUndefined);
	}
	
	/**
	 * Helper class to evaluate chains by chain files.
	 * @author martin.meyer@inftec.ch
	 *
	 */
	public final class ChainFilesResolver {
		private List<URL> propFiles = new ArrayList<>();
		
		/**
		 * Evaluates chain files by name
		 * @param resourceName Absolute path of the resources containing chain information, e.g. 
		 * <code>config/properties.files</code>
		 * <p>
		 * If resourceFolders have been defined on the parent chain builder, resources will be looked up on the
		 * file system first and then on the classpath.
		 * @return This resolver
		 */
		public ChainFilesResolver name(String resourceName) {
			List<URL> files = new ArrayList<>();
			
			// Search resource folders
			for (Path resourceFolder : resourceFolders) {
				Path fsResource = resourceFolder.resolve(resourceName);
				if (JuUrl.path().isExistingFile(fsResource)) {
					files.add(JuUrl.toUrl(fsResource));
				}
			}
			
			// Search classpath
			files.addAll(JuUrl.resource().getAll(resourceName, false));
			this.propFiles.addAll(files);
			
			return this;
		}
		
		/**
		 * Resolves all evaluator using the chain file specified and returns the parent
		 * property chain builder.
		 * @return Parent property chain builder
		 */
		public PropertyChainBuilder resolve() {
			logger.debug("Resolving PropertyChain by chain files");
			
			// Process contents of prop files
			
			XString duplicatePrios = new XString();
			Map<Integer, String[]> props = new TreeMap<>();
			for (URL propFile : this.propFiles) {
				logger.debug("Processing property file: " + propFile);
				
				XString filteredContents = new XString("Filtered contents: " );
				filteredContents.increaseIndent();
				
				try (BufferedReader r = new IOUtil().createReader(propFile)) {
					String line = r.readLine();
					while (line != null) {
						String lineParts[] = JuStringUtils.split(line, ",", true);
						if (lineParts.length > 0 && !lineParts[0].startsWith("#")) {
							AssertUtil.assertTrue("Invalid line: " + line, lineParts.length > 1);
							// Process line
							filteredContents.addLine(line);
							
							int priorization = Integer.parseInt(lineParts[0]);
							if (props.containsKey(priorization)) {
								duplicatePrios.addLineFormatted("Duplicate priorization in %s: %d", propFile, priorization);
							}
							
							props.put(priorization, Arrays.copyOfRange(lineParts, 1, lineParts.length));
						} else {
							// Ignore line
						}
						
						line = r.readLine();
					}
				} catch (Exception ex) {
					throw new JuRuntimeException("Couldn't process property file %s", ex, propFile);
				}
				
				logger.debug(filteredContents.toString());
				
				if (!duplicatePrios.isEmpty()) {
					throw new JuRuntimeException(duplicatePrios.toString());
				}
			}
			
			// Build property chain from read info
			
			PropertyChainBuilder chainBuilder = PropertyChainBuilder.this;
			
			XString chainInfo = new XString("Evaluated property chain:");
			chainInfo.increaseIndent();
			for (int prio : props.keySet()) {
				chainInfo.addLine(prio + ": ");
				
				// Get the remaining line parts (without priorization)
				String[] lineParts = props.get(prio);
				
				String propType = lineParts[0];
				// Perform placeholder substitution
				for (int i = 1; i < lineParts.length; i++) {				
					String linePart = lineParts[i];
					// Do interpolation
					linePart = chainBuilder.peekChain().interpolate(linePart);
					
					// Legacy Placeholder support...
					XString part = new XString(linePart);
					// Substitute %propertyKey% with the appropriate values...
					for (String propertyKey : part.getPlaceHolders()) {
						String val = chainBuilder.peekChain().get(propertyKey);
						if (val != null) {
							part.setPlaceholder(propertyKey, val);
						} else {
							logger.debug("Couldn't replace placeholder: " + propertyKey);
						}
					}
					lineParts[i] = part.toString();
				}
				
				if ("sys".equals(propType)) {
					chainBuilder.addSystemPropertyEvaluator();
					chainInfo.addText("System Properties");
				} else if ("prop".equals(propType)) {
					AssertUtil.assertTrue("prop property type must be followed by a resource path", lineParts.length > 1);
					String resourcePath = lineParts[1];
					boolean optional = lineParts.length > 2 && "optional".equals(lineParts[2]);
					
					URL resourceUrl = getFileResource(resourcePath, optional);
					if (resourceUrl != null) {
						chainBuilder.addResourcePropertyEvaluator(resourceUrl);
						chainInfo.addText("Properties file: " + resourceUrl);
					} else {
						AssertUtil.assertTrue("Mandatory resource not found: " + resourcePath, optional);
						chainInfo.addText("Properties file:   >>> optional resource not found: " + resourcePath);
					}
				} else if ("csv".equals(propType)) {
					AssertUtil.assertTrue(
							"prop property type must be followed by a resource path and a profile property name", lineParts.length > 2);
					String resourcePath = lineParts[1];
					String profilePropertyName = lineParts[2];
					
					String profileName = chainBuilder.getPropertyChain().get(profilePropertyName);
					String defaultColumn = lineParts.length > 3
							? lineParts[2]
							: "default";
					
					URL resourceUrl = getFileResource(resourcePath, false);
					chainBuilder.addCsvPropertyEvaluator(resourceUrl, profileName, defaultColumn);
					chainInfo.addFormatted("CSV Properties: %s [profileName=%s, defaultColumn=%s]"
							, resourceUrl
							, profileName
							, defaultColumn);
				} else {
					throw new JuRuntimeException("Unsupported property type: " + propType);
				}
			}
			
			// Output evaluated chain
			logger.info(chainInfo.toString());
			
			return PropertyChainBuilder.this;
		}
	}
	
	private URL getFileResource(String resourceName, boolean optional) {
		// Try lookup in resource folders
		for (Path p : this.resourceFolders) {
			Path resourcePath = p.resolve(resourceName);
			if (JuUrl.path().isExistingFile(resourcePath)) {
				return JuUrl.toUrl(resourcePath);
			}
		}
		
		// Try lookup on classpath
		return JuUrl.resource().single().exceptionIfNone(!optional).get(resourceName, false);
	}
	
	/**
	 * Helper class to configure property interpolation of a PropertyChain.
	 * @author martin.meyer@inftec.ch
	 *
	 */
	public final class InterpolationBuilder {
		private boolean enabled = true;
		private boolean envVariableInterpolation = true;
		
		/**
		 * Sets whether property interpolation is enabled.
		 * <p>
		 * This will enable property interpolation of type ${propName} as well as environmental variable
		 * interpolation of type ${env.ENV_NAME}.
		 * @param enableInterpolation If true, property interpolation is enabled. If false, it is disabled.
		 * @return Interpolation Builder
		 */
		public InterpolationBuilder enable(boolean enableInterpolation) {
			this.enabled = enableInterpolation;
			return this;
		}
		
		/**
		 * Finished interpolation configuration and returns the PropertyBuilder.
		 * @return PropertyChainBuilder to continue configuration
		 */
		public PropertyChainBuilder done() {
			return PropertyChainBuilder.this;
		}
	}
	
	private static class PropertyChainImpl implements InterpolatingPropertyChain {
		private Logger logger = LoggerFactory.getLogger(PropertyChainImpl.class);
		
		private final List<PropertyEvaluator> evaluators;
		private final JuTextEncryptor decryptor;
		private final boolean defaultThrowExceptionIfUndefined;
		private final Interpolator interpolator;
		private final Set<String> hiddenValueKeys;
		
		private PropertyChainImpl(PropertyChainBuilder builder, boolean defaultThrowExceptionIfUndefined) {
			this.defaultThrowExceptionIfUndefined =  defaultThrowExceptionIfUndefined;
			this.evaluators = new ArrayList<>(builder.evaluators);
			this.decryptor = builder.decryptor;
			this.hiddenValueKeys = new HashSet<>(builder.hiddenValueKeys);
			
			if (builder.interpolationBuilder.enabled) {
				this.interpolator = new RegexBasedInterpolator();
				if (builder.interpolationBuilder.envVariableInterpolation) {
					try {
						this.interpolator.addValueSource(new EnvarBasedValueSource());
					} catch (Exception ex) {
						throw new JuRuntimeException("Couldn't create EnvarBasedValueSource", ex);
					}
				}
				
				this.interpolator.addValueSource(new ChainValueSource());
			} else {
				this.interpolator = null;
			}
		}
		
		@Override
		public String get(String key) {
			return this.get(key, this.defaultThrowExceptionIfUndefined);
		}

		@Override
		public String get(String key, boolean throwExceptionIfNotDefined) {
			Object obj = this.getObject(key, null, throwExceptionIfNotDefined);
			return obj == null ? null : obj.toString();
		}

		@Override
		public String get(String key, String defaultValue) {
			String val = this.get(key, false);
			return val != null ? val : defaultValue;
		}
		
		@Override
		public <T> T get(String key, Class<T> clazz) {
			String val = this.get(key);
			return this.convert(val, clazz);
		}
		
		@Override
		public <T> T get(String key, Class<T> clazz, boolean throwExceptionIfNotDefined) {
			String val = this.get(key, throwExceptionIfNotDefined);
			return this.convert(val, clazz);
		}
		
		@Override
		public <T> T get(String key, Class<T> clazz, String defaultValue) {
			String val = this.get(key, defaultValue);
			return this.convert(val, clazz);
		}
		
		@SuppressWarnings("unchecked")
		private <T> T convert(String val, Class<T> clazz) {
			if (StringUtils.isEmpty(val)) return null;
			
			if (clazz == Integer.class) {
				return (T) new Integer(val);
			} else if (clazz == Boolean.class) {
				return (T) new Boolean(val);
			} else if (clazz == Object.class) {
				return (T) val;
			} else {
				throw new JuRuntimeException("Conversion not supported: " + clazz);
			}
		}
		
		private Object getObject(String key, Object defaultValue, boolean throwExceptionIfNotDefined) {
			PropertyInfoImpl pi = this.evaluteAndInterpolate(key);
			if (pi == null || pi.getValue() == null) {
				if (throwExceptionIfNotDefined) {
					throw new JuRuntimeException("Property undefined: " + key);
				} else {
					return defaultValue;
				}
			}
			return pi.getValue();
		}
		
		private PropertyInfoImpl evaluteAndInterpolate(String key) {
			PropertyInfoImpl pi = this.evaluate(key);
			
			if (this.interpolator != null && pi != null && pi.rawValue instanceof String) {
				try{ 
					String interpolatedValue = this.interpolator.interpolate(pi.getValue());
					pi.setValue(interpolatedValue);
					return pi;
				} catch (InterpolationException ex) {
					logger.warn("Couldn't interpolate " + pi.getValue(), ex);
					return pi;
				}
			} else {
				return pi;
			}
		}
		
		private PropertyInfoImpl evaluate(String key) {
			for (PropertyEvaluator evaluator : evaluators) {
				Object val = evaluator.get(key);
				if (val != null) {
					String stringVal = val.toString();

					PropertyInfoImpl pi = new PropertyInfoImpl(key, val, evaluator.toString());
					
					// Check if we should hide value
					if (this.hiddenValueKeys.contains(key)) {
						pi.setDisplayValue(ENCRYPTED_VALUE_LOGGING_STRING);
						pi.setSensitive(true);
					}
					
					// Check if the value is encrypted
					if (JuSecurityUtils.isEncryptedByTag(stringVal)) {
						if (decryptor != null) {
							pi.setValue(JuSecurityUtils.decryptTaggedValueIfNecessary(stringVal, decryptor));
							pi.setDisplayValue(ENCRYPTED_VALUE_LOGGING_STRING);
							pi.setSensitive(true);
						} else {
							logger.warn("Value seems to be encrypted, but no decrypted was set on the PropertyChain");
						}
					}
					
					logger.debug("Evaluated property: {}", pi);
					return pi;
				}
			}
			return null;
		}

		@Override
		public Set<String> listKeys() {
			Set<String> keys = new LinkedHashSet<>();
			
			for (PropertyEvaluator evaluator : evaluators) {
				keys.addAll(evaluator.listKeys());
			}
			
			return keys;
		}
		
		@Override
		public PropertyInfo getInfo(String key) {
			return this.evaluteAndInterpolate(key);
		}
		
		private class ChainValueSource extends AbstractValueSource {
			public ChainValueSource() {
				super(false);
			}
			
			@Override
			public Object getValue(String expression) {
				PropertyInfoImpl pi = PropertyChainImpl.this.evaluate(expression);
				return pi != null ? pi.getValue() : null;
			}
		}

		@Override
		public String interpolate(String expression) {
			if (this.interpolator != null) {
				try {
					return this.interpolator.interpolate(expression);
				} catch (Exception ex) {
					logger.warn("Couldn't interpolate expression: " + expression, ex);
				}
			}
			return expression;
		}
	}
	
	private static class SystemPropertyEvaluator implements PropertyEvaluator {
		@Override
		public Object get(String key) {
			return System.getProperty(key);
		};
		
		@Override
		public String toString() {
			return JuStringUtils.toString(this);
		}
		
		@Override
		public Set<String> listKeys() {
			return JuCollectionUtils.getKeyStrings(System.getProperties());
		}
	}
	
	private static class PropertiesPropertyEvaluator implements PropertyEvaluator {
		private final URL propertiesUrl;
		private final Properties props;
		
		public PropertiesPropertyEvaluator(Properties props) {
			this.propertiesUrl = null;
			this.props = props;
		}
		
		public PropertiesPropertyEvaluator(URL propertiesUrl) throws JuException {
			this.propertiesUrl = propertiesUrl;
			this.props = new IOUtil().loadPropertiesFromUrl(propertiesUrl);
		}
		
		@Override
		public Object get(String key) {
			return this.props == null ? null : this.props.get(key);
		};
		
		@Override
		public String toString() {
			if (this.propertiesUrl != null) {
				return JuStringUtils.toString(this, "url", this.propertiesUrl);
			} else {
				return JuStringUtils.toString(this);
			}
		}
		
		@Override
		public Set<String> listKeys() {
			return JuCollectionUtils.getKeyStrings(props);
		}
	}
	
	private static class CsvPropertyEvaluator implements PropertyEvaluator {
		private final URL resourceUrl;
		private final String profile;
		private final CsvTableLookup csvTable;
		
		public CsvPropertyEvaluator(URL resourceUrl, String profile, String defaultColumn) {
			this.resourceUrl = resourceUrl;
			this.profile = profile;
			this.csvTable = CsvTableLookup.build()
					.from(resourceUrl)
					.defaultColumn(defaultColumn)
					.create();
		}
		
		@Override
		public Object get(String key) {
			return this.csvTable.get(key, this.profile);
		}
		
		@Override
		public String toString() {
			return JuStringUtils.toString(this
					, "url", this.resourceUrl
					, "profile", this.profile);
		}
		
		@Override
		public Set<String> listKeys() {
			Set<String> keys = new LinkedHashSet<String>();
			keys.addAll(this.csvTable.getKeys());
			
			return keys;
		}
	}
	
	private static class PropertyChainPropertyEvaluator implements PropertyEvaluator {
		private final PropertyChain nestedChain;
		
		public PropertyChainPropertyEvaluator(PropertyChain chain) {
			this.nestedChain = chain;
		}
		
		@Override
		public Object get(String key) {
			return this.nestedChain.get(key, Object.class);
		}

		@Override
		public Set<String> listKeys() {
			return this.nestedChain.listKeys();
		}
		
		@Override
		public String toString() {
			return JuStringUtils.toString(this
					, "nestedChain", this.nestedChain);
		}
	}
	
	private static class PropertyInfoImpl implements PropertyInfo {
		private final String key;
		private final Object rawValue;
		private final String evaluatorInfo;

		private Object value;
		private String displayValue;
		private boolean isSensitive;
		
		private PropertyInfoImpl(String key, Object rawValue, String evaluatorInfo) {
			this.key = key;
			this.rawValue = rawValue;
			this.evaluatorInfo = evaluatorInfo;
			
			this.value = rawValue;
			this.displayValue = rawValue == null ? null : rawValue.toString();
		}

		public void setDisplayValue(String displayValue) {
			this.displayValue = displayValue;
		}

		public void setSensitive(boolean isSensitive) {
			this.isSensitive = isSensitive;
		}

		@Override
		public String getKey() {
			return this.key;
		}

		private void setValue(Object val) {
			if (ObjectUtils.equals(this.value, this.displayValue)) {
				this.displayValue = (val == null ? null : val.toString());
			}
			this.value = val;
		}
		
		@Override
		public String getValue() {
			return this.value == null ? null : this.value.toString();
		}

		@Override
		public String getDisplayValue() {
			return this.displayValue == null ? null : this.displayValue;
		}

		@Override
		public String getRawValue() {
			return this.rawValue == null ? null : this.rawValue.toString();
		}

		@Override
		public boolean isSensitive() {
			return this.isSensitive;
		}

		@Override
		public String getEvaluatorInfo() {
			return this.evaluatorInfo;
		}
		
		@Override
		public String toString() {
			return String.format("%s=%s [using %s]", this.getKey(), this.getDisplayValue(), this.getEvaluatorInfo());
		}
	}
}
