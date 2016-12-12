package ch.inftec.ju.maven.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ch.inftec.ju.security.JuSecurityUtils;
import ch.inftec.ju.util.PropertyChain;
import ch.inftec.ju.util.PropertyChain.PropertyInfo;
import ch.inftec.ju.util.PropertyChainBuilder;
import ch.inftec.ju.util.RegexUtil;

/**
 * Mojo that allows to load properties from a property chain.
 * 
 * @author martin.meyer@inftec.ch
 *
 */
@Mojo(name="properties")
public class PropertiesMojo extends AbstractMojo {
	@Parameter
	private ChainConfig chain;
	
	@Component
	private MavenProject project;
	
	private Map<FilterConfig, List<RegexUtil>> includedPatterns = new HashMap<>();
	
	private Map<FilterConfig, List<RegexUtil>> excludedPatterns = new HashMap<>();
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.chain != null) {
			PropertyChainBuilder chainBuilder = new PropertyChainBuilder();
			
			if (this.chain.resourceFolders.size() == 0) {
				chainBuilder.addResourceFolder(this.project.getBasedir().toPath());
			} else {
				for (File resourceFolder : this.chain.resourceFolders) {
					if (this.chain.silentlyIgnoreMissingResourceFolder && !resourceFolder.exists()) {
						// Ignore resource
					} else {
						chainBuilder.addResourceFolder(resourceFolder.toPath());
					}
				}
			}
				
			for (ChainElementConfig chainElement : this.chain.chainElements) {
				if (chainElement.lookupConfigFile != null && chainElement.lookupConfigFile.fileName != null) {
					chainBuilder.addEvaluatorsByChainFiles()
						.name(chainElement.lookupConfigFile.fileName)
						.resolve();
				}
				if (chainElement.systemProperties != null) {
					chainBuilder.addSystemPropertyEvaluator();
				}
			}
			
			Set<String> exportedProperty = new HashSet<>();
			
			// Add explicit properties (if any)
			if (this.chain.properties.size() > 0) {
				Properties props = new Properties();
				for (Property property : this.chain.properties) {
					String key = chainBuilder.peekChain().interpolate(property.key);
					String value = property.value;
					
					if (value == null) continue;
					
					props.put(key, value);
					
					if (property.exportToSystemProperty) {
						exportedProperty.add(key);
					}
				}
				
				chainBuilder.addPropertiesPropertyEvaluator(props);
			}
			
			// Set decryptor (if any)
			if (this.chain.decryptor != null) {
				if (!StringUtils.isEmpty(this.chain.decryptor.decryptionPasswordPropertyName)) {
					String decryptionPassword = chainBuilder.peekChain().get(this.chain.decryptor.decryptionPasswordPropertyName);
					chainBuilder.setDecryptor(JuSecurityUtils.buildEncryptor()
							.password(decryptionPassword)
							.strong(this.chain.decryptor.strongEncryption)
							.createTextEncryptor());
				} else if (!StringUtils.isEmpty(this.chain.decryptor.keyFilePathPropertyName)) {
					String keyFileName = chainBuilder.peekChain().get(this.chain.decryptor.keyFilePathPropertyName);
					try {
						chainBuilder.setDecryptorByResource(keyFileName, this.chain.decryptor.strongEncryption);
					} catch (Exception ex) {
						if (this.chain.decryptor.ignoreMissingDecryption) {
							this.getLog().warn("Couldn't set decryptor. Continuing as ignoreMissingDecryption is true: " + ex);
						} else {
							throw ex;
						}
					}
				}
			}
			
			PropertyChain chain = chainBuilder.getPropertyChain();
			Set<String> keys = chain.listKeys();
			
			for (String key : keys) {
				if (this.isIncluded(key, this.chain.filter, true)) {
					PropertyInfo pi = chain.getInfo(key);
					
					this.project.getProperties().put(key, pi.getValue());
					
					boolean exportToSystemProperties = this.isIncluded(key, this.chain.exportToSystemProperty, false)
							|| exportedProperty.contains(key);
					if (exportToSystemProperties) {
						System.setProperty(key, pi.getValue());
					}
					if (this.chain.logAddedKeys) {
						String addedKeyLog = String.format("Added key: %s=%s", key, pi.getDisplayValue());
						if (exportToSystemProperties) addedKeyLog += " (-> sys-export)";
						this.getLog().info(addedKeyLog);
					}
				}
			}
		}
	}
	
	private boolean isIncluded(String key, FilterConfig filter, boolean includeIfNoFilter) {
		if (filter != null) {
			List<RegexUtil> includedPatterns = this.includedPatterns.get(filter);
			List<RegexUtil> excludedPatterns = this.excludedPatterns.get(filter);
			if (includedPatterns == null) {
				includedPatterns = new ArrayList<>();
				this.includedPatterns.put(filter, includedPatterns);
				excludedPatterns = new ArrayList<>();
				this.excludedPatterns.put(filter, excludedPatterns);
				
				if (this.chain != null && filter != null) {
					for (String includeFilter : filter.includes) {
						includedPatterns.add(new RegexUtil(includeFilter));
					}
					for (String excludeFilter : filter.excludes) {
						excludedPatterns.add(new RegexUtil(excludeFilter));
					}
				}
			}
			
			for (RegexUtil ru :excludedPatterns) {
				if (ru.matches(key)) return false;
			}
			if (includedPatterns.size() == 0) {
				return true;
			} else {
				for (RegexUtil ru : includedPatterns) {
					if (ru.matches(key)) return true;
				}
				return false;
			}
		} else {
			return includeIfNoFilter;
		}
	}
	
	public MavenProject getProject() {
		return this.project;
	}
	
	public static class ChainConfig {
		/**
		 * List of resource folder to use to lookup resources.
		 * <p>
		 * If none is specified, maven basedir will be used.
		 */
		@Parameter
		private List<File> resourceFolders = new ArrayList<>();
		
		@Parameter(property="ju.prop.silentlyIgnoreMissingResourceFolder")
		private boolean silentlyIgnoreMissingResourceFolder;
		
		@Parameter
		private List<ChainElementConfig> chainElements = new ArrayList<>();
		
		/**
		 * Explicit list of properties that will be added at the end of the chain.
		 * <p>
		 * Interpolation will be used on both keys and values.
		 */
		@Parameter
		private List<Property> properties = new ArrayList<>();
		
		/**
		 * Allows to define filters to define which property keys to include or exclude.
		 * <p>
		 * If only include filters are specified, only matching keys are included.
		 * <p>
		 * If only exclude filters are specified, all non-matchin keys are included.
		 * <p>
		 * If both filter types are specified, all matching includes that don't have a matching exclude are included.
		 */
		@Parameter
		private FilterConfig filter;
		
		/**
		 * Allows to define filters to export properties from the chain to system properties as well (in addition
		 * to Maven properties.
		 * <p>
		 * See the filter parameter for details on how to use excludes and includes.
		 * <p>
		 * Note that only keys passing the chain.filter will be eligable for system propery export...
		 */
		@Parameter
		private FilterConfig exportToSystemProperty;
		
		/**
		 * Flag to activate logging of added keys.
		 */
		@Parameter(property="ju.prop.logAddedKeys", defaultValue="false")
		private boolean logAddedKeys;
		
		@Parameter
		private DecryptorConfig decryptor;
	}
	
	public static class ChainElementConfig {
		@Parameter
		private LookupConfigFileConfig lookupConfigFile;
		
		/**
		 * If specified, properties will be loaded from the system properties
		 */
		@Parameter
		private SystemPropertiesConfig systemProperties;
	}
	
	public static class LookupConfigFileConfig {
		@Parameter
		private String fileName;
	}
	
	public static class SystemPropertiesConfig {
		// This is just a tagging config without parameters
	}
	
	public static class Property {
		@Parameter
		public String key;
		
		@Parameter
		public String value;
		
		@Parameter(defaultValue="false")
		public boolean exportToSystemProperty;
	}
	
	public static class FilterConfig {
		@Parameter
		private List<String> includes = new ArrayList<>();
		
		@Parameter
		private List<String> excludes = new ArrayList<>();
	}
	
	public static class DecryptorConfig {
		@Parameter(property="ju.prop.decryptor.decryptionPasswordPropertyName")
		private String decryptionPasswordPropertyName;
		
		@Parameter(property="ju.prop.decryptor.keyFilePathPropertyName")
		private String keyFilePathPropertyName;
		
		@Parameter(property="ju.prop.decryptor.strongEncryption")
		private boolean strongEncryption;
		
		/**
		 * If true, missing decryption (for any reason, e.g. missing decryption file) will
		 * be ignored. A warning will be output though.
		 */
		@Parameter(property="ju.prop.decryptor.ignoreMissingDecryption")
		private boolean ignoreMissingDecryption;
	}
}
