package ch.inftec.ju.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JuUtilsTest {
	// We need to clear the cached chain to avoid side effects.
	
	@Before
	public void clearPropertyChain_beforeTests() {
		JuUtils.clearPropertyChain();
	}
	
	@BeforeClass
	public static void clearPropertyChain_afterTests() {
		JuUtils.clearPropertyChain();
	}
	
	@Test
	public void propertyChain_isInitialized() {
		Assert.assertEquals("derby", JuUtils.getJuPropertyChain().get("ju-dbutil-test.profile"));
	}
	
	@Test
	public void propertyChain_priorizes_systemProperty() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju-dbutil-test.profile", "myDerby");
			
			Assert.assertEquals("myDerby", JuUtils.getJuPropertyChain().get("ju-dbutil-test.profile"));
		}
	}
	
	@Test
	public void profile_isResolved_forPropertyFile() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju-util.profile", "test");
			
			Assert.assertEquals("profileTestProps", JuUtils.getJuPropertyChain().get("ju-util.testProps"));
		}
	}
		
	@Test
	public void profile_isResolved_forCsvFile() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju-util.profile", "testCsv");
			
			Assert.assertEquals("profileTestCsv", JuUtils.getJuPropertyChain().get("ju-util.testCsv"));
		}
	}
	
	@Test
	public void profile_isResolved_forCsvFile_usingDefault() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju-util.profile", "testCsv");
			
			Assert.assertEquals("profileTestCsvDefault", JuUtils.getJuPropertyChain().get("ju-util.testCsvDefault"));
		}
	}
	
	@Test
	public void propertyChain_ignoresDecryption_ifPasswordFileIsNotSet() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju-util.profile", "testEncryption");
			
			Assert.assertEquals("ENC(8vu+etsGrzZK30MCEBjTzg==)", JuUtils.getJuPropertyChain().get("ju-util.encryptedString"));
		}
	}
	
	@Test
	public void propertyChain_supportDecryption_ifPasswordFileIsSet() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju-util.profile", "testEncryption");
			ts.setProperty("ju-util.propertyChain.encryption.passwordFile"
					, "src/test/resources/ch/inftec/ju/util/JuUtilsTest_encryptionPassword");
			
			Assert.assertEquals("String", JuUtils.getJuPropertyChain().get("ju-util.encryptedString"));
		}
	}
	
	@Test
	public void propertyChain_supportDecryption_ifPasswordPropertyIsSet() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju-util.propertyChain.encryption.password", "secretPassword");
			ts.setProperty("encrVal", "ENC(cCULeUjKiLfBwEgCOKC1g3BasxVDF85c)"); // secretVal
			
			Assert.assertEquals("secretVal", JuUtils.getJuPropertyChain().get("encrVal"));
		}
	}
	
	@Test
	public void defaultDecryptor_isAvailable_whenPassword_isSet() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju-util.propertyChain.encryption.password", "secretPassword");
			
			Assert.assertEquals("secretVal", JuUtils.getDefaultEncryptor().decrypt("cCULeUjKiLfBwEgCOKC1g3BasxVDF85c"));
		}
	}
	
	@Test
	public void defaultDecryptor_isNull_whenPassword_isNotSet() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju-util.propertyChain.encryption.password", null);
			
			Assert.assertNull(JuUtils.getDefaultEncryptor());
		}
	}
	
	@Test
	public void propertyChain_interpolates_byDefault() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju-util.propertyChain.interpolation", null);
			ts.setProperty("p1", "v1");
			ts.setProperty("p2", "v2 ${p1}");
			
			Assert.assertEquals("v2 v1", JuUtils.getJuPropertyChain().get("p2"));
		}
	}
	
	@Test
	public void propertyChain_doesnNotinterpolate_ifDeactivated() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju-util.propertyChain.interpolation", "false");
			ts.setProperty("p1", "v1");
			ts.setProperty("p2", "v2 ${p1}");
			
			Assert.assertEquals("v2 ${p1}", JuUtils.getJuPropertyChain().get("p2"));
		}
	}
	
	@Test
	public void canInterpolate_value() {
		PropertyChain pc = PropertyChainTest.createPropertiesChain(false,
				"p1", "World");
		
		Assert.assertEquals("Hello World", JuUtils.interpolate("Hello ${p1}", pc));
	}
	
	@Test
	public void canInterpolate_envValue() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setEnv("MY_ENV", "World");
			
			Assert.assertEquals("Hello World", JuUtils.interpolate("Hello ${env.MY_ENV}"));
		}
	}
}
