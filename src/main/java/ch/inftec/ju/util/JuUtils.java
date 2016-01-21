package ch.inftec.ju.util;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.inftec.ju.security.JuSecurityUtils;
import ch.inftec.ju.security.JuTextEncryptor;

/**
 * Utility class containing functions related to the JU library itself.
 * @author Martin
 *
 */
public class JuUtils {
	private static Logger logger = LoggerFactory.getLogger(JuUtils.class);
	
	private static PropertyChain juPropertyChain;
	
	private static JuTextEncryptor defaultEncryptor;
	
	/**
	 * Name of the files that contains the property files references.
	 */
	private static final String PROPERTIES_FILES_NAME = "ju.properties.files";
	
	/**
	 * Name of the property containing an optional encryption password.
	 * <p>
	 * If set, takes precedence over ENCRYPTION_PASSWORD_FILE_PROPERTY.
	 */
	private static final String ENCRYPTION_PASSWORD_PROPERTY = "ju-util.propertyChain.encryption.password";
	
	/**
	 * Name of the property containing the path to an optional encryption password file.
	 * <p>
	 * Only applies if ENCRYPTION_PASSWORD_PROPERTY is not set.
	 */
	private static final String ENCRYPTION_PASSWORD_FILE_PROPERTY = "ju-util.propertyChain.encryption.passwordFile";
	
	/**
	 * Name of the property that contains the strong encryption setting.
	 */
	private static final String STRONG_ENCRYPTION_PROPERTY = "ju-util.propertyChain.encryption.strong";
	
	/**
	 * Name of the property containing the interpolation flag.
	 */
	private static final String INTERPOLATION_PROPERTY = "ju-util.propertyChain.interpolation";
	
	/**
	 * Gets a PropertyChain to evaluate ju properties.
	 * <p>
	 * Evaluation of the properties is as follows:
	 * <ol>
	 *   <li><b>ju_module.properties</b>: Optional file to override properties in a module that we don't wan't
	 *       to override even with system properties. Is intended to be checked into source control.</li>
	 *   <li><b>System Property</b>: Overrides all other properties</li>
	 *   <li><b>ju_module_default.properties</b>: Optional file to override default properties in a module. Is intended
	 *       to be checked into source control. In this file, we should only add properties for
	 *       which we want to change the default value for a module (or whatever scope the
	 *       classpath has).</li>
	 *   <li><b>ju_user.properties</b>: Optional user properties file overriding default properties. 
	 *       Must be on the classpath and is not intended to be checked into source control</li>
	 *   <li><b>ju_default.properties</b>: Global default properties that are used when no 
	 *       overriding properties are specified or found. This file also contains the description
	 *       for all possible properties.</li>
	 * </ol>
	 * The PropertyChain of this method is cached, so it is loaded only the first time
	 * it is accessed.
	 * <p>
	 * The PropertyChain is configured <i>not</i> to throw exceptions by default if a property
	 * is undefined.
	 * <p>
	 * Encrypted properties can be decrypted automatically by setting a file containing the decryption password using
	 * the property <i>ju-util.propertyChain.encryption.passwordFile</>. This property will be evaluated using the Chain
	 * right before finalizing it.
	 * <p>
	 * The property chain can be configured by using <code>ju.properties.files</code> chain files on the classpath. See
	 * <code>ju-util/src/main/resources/ju.properties.files</code> for a reference on chain files.
	 * 
	 * @return PropertyChain implemenation to evaluate JU properties
	 */
	public static synchronized PropertyChain getJuPropertyChain() {
		if (juPropertyChain == null) {
			logger.debug("Initializing JU PropertyChain");
			
			PropertyChainBuilder chainBuilder = new PropertyChainBuilder();
			
			// Add hidden value keys
			chainBuilder.hideValueForKey(ENCRYPTION_PASSWORD_PROPERTY);
			
			// Add evaluators by chain file resources found on the classpath
			chainBuilder.addEvaluatorsByChainFiles()
				.name(JuUtils.PROPERTIES_FILES_NAME)
				.resolve();
			
			// Enable interpolation (unless deactivated)
			boolean interpolate = chainBuilder.peekChain().get(INTERPOLATION_PROPERTY, Boolean.class, "true");
			chainBuilder.interpolation().enable(interpolate);
			
			evaluateDefaultEncryptor(chainBuilder.peekChain());
			chainBuilder.setDecryptor(defaultEncryptor);
			
			juPropertyChain = chainBuilder.getPropertyChain();
		}

		return juPropertyChain;
	}
	
	/**
	 * Clears the cached property chain, forcing a reload.
	 */
	public static void clearPropertyChain() {
		juPropertyChain = null;
		
		// We'll also reset the default encryptor as it depends on values of the property chain
		defaultEncryptor = null;
	}
	
	private static synchronized void evaluateDefaultEncryptor(PropertyChain chainPeek) {
		// Check if an encryption password was set
		String encryptionPassword = chainPeek.get(ENCRYPTION_PASSWORD_PROPERTY);
		boolean strongEncryption = chainPeek.get(STRONG_ENCRYPTION_PROPERTY, Boolean.class, "false");
		if (StringUtils.isNotBlank(encryptionPassword)) {
			logger.debug("Password set, setting decryptor (strong encryption={})"
					, strongEncryption);
			
			defaultEncryptor = JuSecurityUtils.buildEncryptor()
					.strong(strongEncryption)
					.password(encryptionPassword)
					.createTextEncryptor();
		} else {
			// Check if an encryption password file was set
			String encryptionPasswordFile = chainPeek.get(ENCRYPTION_PASSWORD_FILE_PROPERTY);
			if (!StringUtils.isEmpty(encryptionPasswordFile)) {
				logger.debug("Password file set: {}. Loading password and setting decryptor (strong encryption={})"
						, encryptionPasswordFile
						, strongEncryption);
				
				Path p = JuUrl.existingFile(encryptionPasswordFile);
				
				defaultEncryptor = JuSecurityUtils.buildEncryptor()
						.strong(strongEncryption)
						.passwordByUrl(JuUrl.toUrl(p))
						.createTextEncryptor();
			}
		}
	}
	
	/**
	 * Gets the default TextEncryptor, i.e. the TextEncryptor specified using the JU properties of the
	 * default PropertyChain.
	 * <p>
	 * If none was specified, null is returned.
	 * @return JuTextEncryptor
	 */
	public static synchronized JuTextEncryptor getDefaultEncryptor() {
		// Get Property Chain. This will evaluate the TextEncryptor if necessary
		getJuPropertyChain();
		
		return defaultEncryptor;
	}
	
	/**
	 * Interpolates the specified value using the default PropertyChain returned by
	 * JuUtils.getJuPropertyChain().
	 * @param expression Expression to interpolate
	 * @return Interpolated value
	 */
	public static String interpolate(String expression) {
		return JuUtils.interpolate(expression, JuUtils.getJuPropertyChain());
	}
	
	/**
	 * Helper method to interpolate String using the specified PropertyChain as value source.
	 * @param expression Expression to interpolate
	 * @param chain PropertyChain value source
	 * @return
	 */
	public static String interpolate(String expression, PropertyChain chain) {
		InterpolatingPropertyChain propertyChain = new PropertyChainBuilder()
				.interpolation()
					.enable(true)
					.done()
				.addPropertyChainPropertyEvaluator(chain)
				.getPropertyChain();
		
		return propertyChain.interpolate(expression);
	}
}
