package ch.inftec.ju.security;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jasypt.util.text.BasicTextEncryptor;
import org.jasypt.util.text.StrongTextEncryptor;
import org.jasypt.util.text.TextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import ch.inftec.ju.util.AssertUtil;
import ch.inftec.ju.util.IOUtil;
import ch.inftec.ju.util.JuException;
import ch.inftec.ju.util.JuRuntimeException;
import ch.inftec.ju.util.JuStringUtils;
import ch.inftec.ju.util.JuUrl;
import ch.inftec.ju.util.RegexUtil;

/**
 * Utility class providing security related functionality, like encryption and decryption algorithms.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 */
public final class JuSecurityUtils {
	/**
	 * RegexUtil consisting of the Regex pattern to find encrypted properties.
	 * <p>
	 * Group 1 (index 0) contains the encrypted value.
	 */
	private static final RegexUtil ENCRYPTION_REGEX = new RegexUtil("ENC\\((.*)\\)");
	
	/**
	 * Gets a new EncryptorBuilder to configure and build encryptor instances.
	 * @return EncryptorBuilder
	 */
	public static EncryptorBuilder buildEncryptor() {
		return new EncryptorBuilder();
	}
	
	/**
	 * Gets a new EncryptionBuilder to perform encryption, e.g. of property files.
	 * @return EncryptionBuilder
	 */
	public static EncryptionBuilder performEncryption() {
		return new EncryptionBuilder();
	}
	
	/**
	 * Decrypts the specified value (in case it is encrypted and has the form
	 * ENC(value). Otherwise, the value is just returned.
	 * @param value Value to be decrypted
	 * @param encryptor TextEncryptor to decrypt the value if necessary
	 * @return Decrypted value or same value
	 */
	public static String decryptTaggedValueIfNecessary(String value, JuTextEncryptor encryptor) {
		if (encryptor != null && isEncryptedByTag(value)) {
			String encryptedMessage = ENCRYPTION_REGEX.getMatches(value)[0].getGroups()[0];
			return encryptor.decrypt(encryptedMessage);
		} else {
			return value;
		}
	}
	
	/**
	 * Gets whether the specified value is encrypted by an ENC(value) tag.
	 * @param value Value
	 * @return True if value is encrypted using ENC(value), false otherwise
	 */
	public static boolean isEncryptedByTag(String value) {
		return ENCRYPTION_REGEX.matches(value);
	}
	
	private static JuTextEncryptor asJuTextEncryptor(final TextEncryptor encryptor) {
		return new JuTextEncryptor() {
			@Override
			public String decrypt(String encryptedMessage) {
				return encryptor.decrypt(encryptedMessage);
			}
			
			@Override
			public String encrypt(String message) {
				return encryptor.encrypt(message);
			}
		};
	}
	
	/**
	 * Helper class to build encryptors.
	 * @author Martin Meyer <martin.meyer@inftec.ch>
	 *
	 */
	public static final class EncryptorBuilder {
		private boolean strongEncryption = false;
		private String password;
		
		/**
		 * Creates a strong (and more CPU intensive) encryptor.
		 * <p>
		 * May need the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files.
		 * @return This builder
		 */
		public EncryptorBuilder strong() {
			return this.strong(true);
		}
		
		/**
		 * Sets whether to use strong (and more CPU intensive) encryption.
		 * <p>
		 * May need the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files.
		 * @param strong If true, strong encryption will be used, if false, basic encryption will be used
		 * @return This builder
		 */
		public EncryptorBuilder strong(boolean strong) {
			this.strongEncryption = strong;
			return this;
		}
		
		/**
		 * Sets the password for the encryptor.
		 * @param password Password
		 * @return This builder
		 */
		public EncryptorBuilder password(String password) {
			this.password = password;
			return this;
		}
		
		/**
		 * Sets the password for the encryptor from a resource (e.g. a file).
		 * <p>
		 * Encoding of the resource is supposed to be UTF-8
		 * @param keyFile Path to resource containing the password
		 * @return This builder
		 */
		public EncryptorBuilder passwordByUrl(URL keyFile) {
			try {
				String password = new IOUtil().loadTextFromUrl(keyFile);
				AssertUtil.assertNotEmpty("Encryption password must not be empty", password);
				
				// We'll trim the password to avoid problems with new lines and the like
				return this.password(password.trim());
			} catch (Exception ex) {
				throw new JuRuntimeException("Couldn't load password from resource %s", ex, keyFile);
			}
		}
		
		/**
		 * Creates a text encryptor / decryptor.
		 * <p>
		 * Encrypted text will be encoded using Base64 encoding {@link http://en.wikipedia.org/wiki/Base64}. This
		 * means that encrypted Strings will contain upper and lower case letters along with the characters
		 * '+', '/' and '='.
		 * @return JuTextEncryptor instance
		 */
		public JuTextEncryptor createTextEncryptor() {
			AssertUtil.assertFalse("Password must be set", StringUtils.isEmpty(this.password));
			
			if (this.strongEncryption) {
				StrongTextEncryptor encryptor = new StrongTextEncryptor();
				encryptor.setPassword(this.password);
				return asJuTextEncryptor(encryptor);
			} else {
				BasicTextEncryptor encryptor = new BasicTextEncryptor();
				encryptor.setPassword(this.password);
				return asJuTextEncryptor(encryptor);
			}
		}
	}
	
	public static final class EncryptionBuilder {
		private Logger logger = LoggerFactory.getLogger(EncryptionBuilder.class);
		
		
		public static char DEFAULT_CSV_DELIMITER = ';';
		
		private URL sourceUrl;
		private String unencryptedString;
		private JuTextEncryptor encryptor;
		
		// List of encrypted token, in order they appear in the String to be encrypted
		private List<EncryptionToken> encryptionTokens;

		private boolean outputDebugMessages = false;
		
		private static final class EncryptionToken {
			private String originalToken;
			private String unencryptedValue;
			
			private EncryptionToken(String originalToken, String unencryptedValue) {
				this.originalToken = originalToken;
				this.unencryptedValue = unencryptedValue;
			}
		}
		
		private RegexUtil doEncRegex = new RegexUtil(RegexUtil.WHITESPACE + "*doENC\\((.+)\\)" + RegexUtil.WHITESPACE + "*");
		
		/**
		 * Activates debug messages. Note that this will output the password and unencrypted
		 * values.
		 * @return Output Debug messages
		 */
		public EncryptionBuilder outputDebugMessages() {
			this.outputDebugMessages = true;
			return this;
		}
		
		/**
		 * Encrypts the specified property file.
		 * <p>
		 * Values to be encrypted must take the form <code>doENC(unencryptedValue)</code>
		 * <p>
		 * Encoding must be UTF-8.
		 * @param propertyFile URL to property file
		 * @return This builder
		 * @throws JuException if the property file cannot be read
		 */
		public EncryptionBuilder propertyFile(URL propertyFile) throws JuException {
			AssertUtil.assertNull("Source has already been defined", this.encryptionTokens);
			
			this.sourceUrl = propertyFile;
			this.unencryptedString = new IOUtil().loadTextFromUrl(propertyFile);
			
			// Load tokens
			
			this.encryptionTokens = new ArrayList<>();
			
			try (StringReader propReader = new StringReader(this.unencryptedString)) {
				Properties props = new Properties();
				props.load(propReader);
				
				for (Enumeration<?> names = props.propertyNames(); names.hasMoreElements(); ) {
					String val = props.getProperty(names.nextElement().toString());
					this.processUnencryptedValue(val, val);
				}
			} catch (IOException ex) {
				throw new JuException("Couldn't load properties", ex);
			}
			
			return this;
		}
		
		private void processUnencryptedValue(String unescapedValue, String escapedValue) {
			if (this.doEncRegex.matches(unescapedValue)) {
				String unencryptedValue = doEncRegex.getMatches(unescapedValue)[0].getGroups()[0];
				this.encryptionTokens.add(new EncryptionToken(escapedValue, unencryptedValue));
			}
		}
		
		public EncryptionBuilder csvFile(URL csvFile) throws JuException {
			AssertUtil.assertNull("Source has already been defined", this.encryptionTokens);
			
			this.sourceUrl = csvFile;
			this.unencryptedString = new IOUtil().loadTextFromUrl(csvFile);
			
			// Load tokens
			
			this.encryptionTokens = new ArrayList<>();
			
			try (Reader urlReader = new IOUtil().createReader(csvFile);
					CSVReader csvReader = new CSVReader(urlReader, DEFAULT_CSV_DELIMITER)) {
				List<String[]> allEntries = csvReader.readAll();
				for (String[] line : allEntries) {
					for (String cell : line) {
						// The CSVReader will return the actual String, but within the source CSV,
						// characters might have been escaped
						String escapedCell = cell.replaceAll("\"", "\"\"");
						this.processUnencryptedValue(cell, escapedCell);
					}
				}
			} catch (IOException ex) {
				throw new JuException("Error reading CSV File", ex);
			}
			
			return this;
		}
		
		/**
		 * Set the text encryptor used to perform the encryption
		 * @param encryptor Encryptor instance
		 * @return This builder
		 */
		public EncryptionBuilder encryptor(JuTextEncryptor encryptor) {
			this.encryptor = encryptor;
			return this;
		}
		
		private String getEncryptedToken(String encryptedVal) {
			return String.format("ENC(%s)", encryptedVal);
		}
		
		/**
		 * Encrypts the source to a String.
		 * @return String resource
		 * @throws JuException If the encryption fails
		 */
		public String encryptToString() throws JuException {
			AssertUtil.assertNotNull("No resource to encrypt specified", this.encryptionTokens);
			AssertUtil.assertNotNull("No encryptor to encrypt specified", this.encryptor);
			
			String encryptedString = this.unencryptedString;
			for (EncryptionToken token : this.encryptionTokens) {
				String encryptedValue = this.encryptor.encrypt(token.unencryptedValue);
				
				String newEncryptedString = encryptedString.replaceFirst(
						Pattern.quote(token.originalToken),
						this.getEncryptedToken(encryptedValue));
				
				if (newEncryptedString.equals(encryptedString)) {
					throw new JuException("Couldn't replace token: %s", token.unencryptedValue);
				} else {
					encryptedString = newEncryptedString;
				}
				
				if (this.outputDebugMessages) {
					logger.info("Encrypted token {}. Encrypted Value: {}"
							, token.originalToken
							, encryptedValue);
				}
			}
			
			return encryptedString;
		}
		
		/**
		 * Encrypts the source to the source file, overriding the doENC tokens with
		 * the ENC encrypted tokens.
		 * @throws JuException If source file cannot be written to
		 */
		public void encryptToSourceFile() throws JuException {
			this.encryptToSourceFile(null);
		}
		
		/**
		 * Encrypts the source to the source file, overriding the doENC tokens with
		 * the ENC encrypted tokens.
		 * <p>
		 * Prior to overwriting the source file, a backup with the original contents is created and
		 * and a Path to the backup file is returned
		 * @throws JuException If files cannot be written to
		 */
		public Path encryptToSourceFileWithBackup() throws JuException {
			Path srcFile = JuUrl.toPath(this.sourceUrl);
			Path backup = srcFile.getParent().resolve(srcFile.getFileName().toString() + "_backup_" 
					+ JuStringUtils.TIMESTAMP_FORMAT_SECONDS.format(new Date()));
			
			this.encryptToSourceFile(backup);
			return backup;
		}
		
		private void encryptToSourceFile(Path backupFile) throws JuException {
			Path sourceFile = JuUrl.toPath(this.sourceUrl);
			
			if (backupFile != null) {
				IOUtil.copyFile(sourceFile, backupFile, false);
			}
			
			String encryptedString = this.encryptToString();
			new IOUtil().writeTextToFile(encryptedString, sourceFile, true);
		}
	}
}
