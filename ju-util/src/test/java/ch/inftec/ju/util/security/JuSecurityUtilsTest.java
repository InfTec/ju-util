package ch.inftec.ju.util.security;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.junit.Assert;
import org.junit.Test;

import ch.inftec.ju.security.JuSecurityUtils;
import ch.inftec.ju.security.JuTextEncryptor;
import ch.inftec.ju.util.IOUtil;
import ch.inftec.ju.util.JuUrl;
import ch.inftec.ju.util.RegexUtil;
import ch.inftec.ju.util.io.NewLineReader;

public class JuSecurityUtilsTest {
	@Test
	public void canEncryptText_usingBasicEncryptor() {
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor()
				.password("secret")
				.createTextEncryptor();
		
		String encryptedString = encryptor.encrypt("String"); // The encrypted String will not be constant...
		Assert.assertNotNull(encryptor.encrypt("String"));
		Assert.assertEquals("String", encryptor.decrypt(encryptedString));
	}
	
	@Test
	public void canDencryptText_usingBasicEncryptor() {
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor().password("secret").createTextEncryptor();
		Assert.assertEquals("String", encryptor.decrypt("8vu+etsGrzZK30MCEBjTzg=="));
	}
	
	@Test(expected=EncryptionOperationNotPossibleException.class)
	public void throwsException_usingBasicEncryptor_withWrongPassword() {
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor().password("wrongSecret").createTextEncryptor();
		Assert.assertEquals("String", encryptor.decrypt("8vu+etsGrzZK30MCEBjTzg=="));
	}
	
	@Test
	public void decryptTaggedValueIfNecessary_decryptsEncryptedString() {
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor().password("secret").createTextEncryptor();
		Assert.assertEquals("String", JuSecurityUtils.decryptTaggedValueIfNecessary("ENC(8vu+etsGrzZK30MCEBjTzg==)", encryptor));
	}
	
	@Test
	public void decryptTaggedValueIfNecessary_returnsUnencryptedString() {
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor().password("secret").createTextEncryptor();
		Assert.assertEquals("8vu+etsGrzZK30MCEBjTzg==", JuSecurityUtils.decryptTaggedValueIfNecessary("8vu+etsGrzZK30MCEBjTzg==", encryptor));
	}
	
	@Test
	public void decryptTaggedValueIfNecessary_returnsString_forEmptyEncrptor() {
		Assert.assertEquals("ENC(TEST)", JuSecurityUtils.decryptTaggedValueIfNecessary("ENC(TEST)", null));
	}
	
	@Test
	public void isEncryptedByTag_returnsTrue_forEncryptedValue() {
		Assert.assertTrue(JuSecurityUtils.isEncryptedByTag("ENC(xxx)"));
	}
	
	@Test
	public void isEncryptedByTag_returnsFalse_forUnencryptedValue() {
		Assert.assertFalse(JuSecurityUtils.isEncryptedByTag("xyz"));
	}
	
	@Test
	public void isEncryptedByTag_returnsFalse_forNull() {
		Assert.assertFalse(JuSecurityUtils.isEncryptedByTag(null));
	}
	
	// Moved to ju-testing, StrongSecurityTest as we need the JuAssumeUtils...
//	@Test
//	public void canEncryptText_usingStrongEncryptor() {}
	
	@Test
	public void canEncrypt_propertiesFile() throws Exception {
		URL srcProperties = JuUrl.resource("ch/inftec/ju/util/security/JuSecurityUtilsTest_toBeEncrypted.properties");
		
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor().password("secret").createTextEncryptor();
		String res = JuSecurityUtils.performEncryption()
			.propertyFile(srcProperties)
			.encryptor(encryptor)
			.encryptToString();
		
		List<String> lines = NewLineReader.createByString(res).getLines();
		Assert.assertEquals(5, lines.size());
		Assert.assertEquals("normalProperty=Normal Value", lines.get(0));
		Assert.assertEquals("", lines.get(1));
		Assert.assertEquals("# This is my little secret file...", lines.get(2));
		Assert.assertTrue(lines.get(3).startsWith("secretProperty=ENC("));
		
		// Check encrypted value
		String encryptedVal = new RegexUtil(".*ENC\\((.*)\\)").getMatches(lines.get(3))[0].getGroups()[0];
		Assert.assertEquals("myPrecious", encryptor.decrypt(encryptedVal));
		
		// Make sure already encrypted values are not changed
		Assert.assertEquals("anoterSecret=ENC(alreadyEncrypted)", lines.get(4));
	}
	
	@Test
	public void canEncrypt_simpleCsvFile() throws Exception {
		URL srcCsv = JuUrl.resource("ch/inftec/ju/util/security/JuSecurityUtilsTest_toBeEncrypted.csv");
		
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor().password("secret").createTextEncryptor();
		String res = JuSecurityUtils.performEncryption()
			.csvFile(srcCsv)
			.encryptor(encryptor)
			.encryptToString();
		
		List<String> lines = NewLineReader.createByString(res).getLines();
		Assert.assertEquals(4, lines.size());
		Assert.assertEquals("Col1;Col2;Col3", lines.get(0));
		Assert.assertEquals(";Normal Value;", lines.get(1));
		Assert.assertEquals(";;", lines.get(2));
		Assert.assertTrue(lines.get(3).startsWith("#Come Comment;ENC(alreadyEncrypted);"));
		
		// Check encrypted value
		String encryptedVal = new RegexUtil(".*ENC\\((.*)\\)$").getMatches(lines.get(3))[0].getGroups()[0];
		Assert.assertEquals("myPrecious", encryptor.decrypt(encryptedVal));
	}
	
	@Test
	public void canEncrypt_complexCsvFile() throws Exception {
		URL srcCsv = JuUrl.resource("ch/inftec/ju/util/security/JuSecurityUtilsTest_toBeEncrypted_complex.csv");
		
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor().password("secret").createTextEncryptor();
		String res = JuSecurityUtils.performEncryption()
			.csvFile(srcCsv)
			.encryptor(encryptor)
			.encryptToString();
		
		List<String> lines = NewLineReader.createByString(res).getLines();
		Assert.assertEquals(3, lines.size());
		Assert.assertEquals("NormalValue;", lines.get(0));
		
		// Check first encrypted value
		Assert.assertTrue(lines.get(1).startsWith("\"ENC("));
		Assert.assertTrue(lines.get(1).endsWith(")\";another value"));
		String encryptedVal1 = new RegexUtil("\"ENC\\((.*)\\)").getMatches(lines.get(1))[0].getGroups()[0];
		Assert.assertEquals("secret(;)", encryptor.decrypt(encryptedVal1));
		
		// Check second encrypted value
		Assert.assertTrue(lines.get(2).startsWith("\"ENC("));
		Assert.assertTrue(lines.get(2).endsWith(")\";"));
		String encryptedVal2 = new RegexUtil("\"ENC\\((.*)\\)").getMatches(lines.get(2))[0].getGroups()[0];
		Assert.assertEquals("\"secret;\"", encryptor.decrypt(encryptedVal2));
		
	}
	
	@Test
	public void canEncrypt_toSourceFile() throws Exception {
		Path srcFile = Paths.get("src/test/resources/ch/inftec/ju/util/security/JuSecurityUtilsTest_toBeEncrypted.properties");
		Path testCopy = Paths.get("target/tests").resolve(srcFile.getFileName());
		
		IOUtil.copyFile(srcFile, testCopy, true);
		
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor().password("secret").createTextEncryptor();
		JuSecurityUtils.performEncryption()
			.propertyFile(JuUrl.toUrl(testCopy))
			.encryptor(encryptor)
			.encryptToSourceFile();
		
		String res = new IOUtil().loadTextFromUrl(JuUrl.toUrl(testCopy));
		Assert.assertFalse(res.contains("doENC("));
		
		IOUtil.deleteFile(testCopy);
	}
	
	@Test
	public void canEncrypt_toSourceFile_withBackup() throws Exception {
		Path srcFile = Paths.get("src/test/resources/ch/inftec/ju/util/security/JuSecurityUtilsTest_toBeEncrypted.properties");
		String original = new IOUtil().loadTextFromUrl(JuUrl.toUrl(srcFile));
		Path testCopy = Paths.get("target/tests").resolve(srcFile.getFileName());
		
		IOUtil.copyFile(srcFile, testCopy, true);
		
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor().password("secret").createTextEncryptor();
		Path backupFile = JuSecurityUtils.performEncryption()
			.propertyFile(JuUrl.toUrl(testCopy))
			.encryptor(encryptor)
			.encryptToSourceFileWithBackup();
		
		String res = new IOUtil().loadTextFromUrl(JuUrl.toUrl(testCopy));
		Assert.assertFalse(res.contains("doENC("));
		
		String backup = new IOUtil().loadTextFromUrl(JuUrl.toUrl(backupFile));
		Assert.assertEquals(original, backup);
		
		IOUtil.deleteFile(testCopy);
		IOUtil.deleteFile(backupFile);
	}
}