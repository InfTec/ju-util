package ch.inftec.ju.util;


import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ch.inftec.ju.security.JuSecurityUtils;
import ch.inftec.ju.security.JuTextEncryptor;
import ch.inftec.ju.util.PropertyChain.PropertyInfo;

public class PropertyChainTest {
	@Test
	public void resourcePropertyEvaluator_ignoresMissingResource() {
		PropertyChain chain = new PropertyChainBuilder()
			.addResourcePropertyEvaluator("xyz", true)
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest.properties", true)
			.getPropertyChain();
		
		Assert.assertEquals("val1", chain.get("prop1"));
	}
	
	@Test
	public void propertyChain_chainsEvaluators() {
		PropertyChain chain = new PropertyChainBuilder()
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest-2.properties", false)
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest.properties", false)
			.addSystemPropertyEvaluator()
			.getPropertyChain();
		
		String key = "ch.inftec.ju.util.PropertyChainTest.prop1";
		System.setProperty(key, "val1");
		Assert.assertEquals("val1b", chain.get("prop1"));
		Assert.assertEquals("val1", chain.get(key));
	}
	
	@Test
	public void propertyChain_listsKeys() {
		PropertyChain chain = new PropertyChainBuilder()
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest-2.properties", false)
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest.properties", false)
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest-3.properties", false)
			.addCsvPropertyEvaluator(JuUrl.resource("ch/inftec/ju/util/PropertyChainTest_listKeys.csv"), "", "")
			.getPropertyChain();
		
		Set<String> keys = chain.listKeys();
		TestUtils.assertCollectionEquals(keys, "prop1", "prop2", "prop3", "csvKey");
	}
	
	@Test
	public void propertyChain_convertsInteger() {
		PropertyChain chain = new PropertyChainBuilder()
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest_conversion.properties", false)
			.getPropertyChain();
		
		Assert.assertEquals(new Integer(1), chain.get("intProp1", Integer.class));
		Assert.assertEquals(new Integer(-1), chain.get("intProp2", Integer.class));
	}
	
	@Test
	public void propertyChain_convertsBoolean() {
		PropertyChain chain = new PropertyChainBuilder()
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest_conversion.properties", false)
			.getPropertyChain();
		
		Assert.assertTrue(chain.get("booleanProp1", Boolean.class));
		Assert.assertFalse(chain.get("booleanProp2", Boolean.class));
	}
	
	@Test
	public void propertyChain_decryptsEncryptedValues() {
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor()
			.password("secret")
			.createTextEncryptor();

		//encryptor.encrypt("secret String");
		//Sample value: bSmw4g8BdopiLClgC7zU2Kwr0LyRqj79
		
		PropertyChain chain = new PropertyChainBuilder()
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest_encryption.properties", false)
			.setDecryptor(encryptor)
			.getPropertyChain();
		
		Assert.assertEquals("secret String", chain.get("encString"));
	}
	
	@Test
	public void propertyChain_leavesNormalValues() {
		//JuSecurityUtils.buildEncryptor().password("secret").createTextEncryptor().encrypt("secret String");
		//Sample value: bSmw4g8BdopiLClgC7zU2Kwr0LyRqj79
		
		PropertyChain chain = new PropertyChainBuilder()
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest_encryption.properties", false)
			.getPropertyChain();
		
		Assert.assertEquals("normal String", chain.get("normString"));
	}
	
	@Test
	public void propertyChain_leavesValuesIfEncryptedValIsSubstring() {
		//JuSecurityUtils.buildEncryptor().password("secret").createTextEncryptor().encrypt("secret String");
		//Sample value: bSmw4g8BdopiLClgC7zU2Kwr0LyRqj79
		
		PropertyChain chain = new PropertyChainBuilder()
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest_encryption.properties", false)
			.getPropertyChain();
		
		Assert.assertEquals("SubString ENC(bSmw4g8BdopiLClgC7zU2Kwr0LyRqj79)", chain.get("encSubString"));
	}
	
	@Test
	public void propertyChain_doesNotInterpolateProperty_ifDeactivated() {
		PropertyChain pc = createPropertiesChain(false,
				"prop1", "World",
				"prop2", "Hello ${prop1}");
		
		Assert.assertEquals("Hello ${prop1}", pc.get("prop2"));
	}
	
	@Test
	public void propertyChain_canInterpolateProperty_ifActivated() {
		PropertyChain pc = createPropertiesChain(true,
				"prop1", "World",
				"prop2", "Hello ${prop1}");
		
		Assert.assertEquals("Hello World", pc.get("prop2"));
	}
	
	@Test
	public void propertyChain_canInterpolate_multipleProperties() {
		PropertyChain pc = createPropertiesChain(true,
				"p1", "P1",
				"p2", "P2 (${p1})",
				"p3", "P3 ${p1} ${p2}");
		
		Assert.assertEquals("P3 P1 P2 (P1)", pc.get("p3"));
	}
	
	@Test
	public void propertyChain_canInterpolate_propertyWithHyphen() {
		PropertyChain pc = createPropertiesChain(true,
				"p1-p1", "P1",
				"ju-ee.junit.groups", "group",
				"p2", "${p1-p1} ${ju-ee.junit.groups}");
		
		Assert.assertEquals("P1 group", pc.get("p2"));
	}
	
	@Ignore("Couldn't get escaping working...")
	@Test
	public void propertyChain_canEscape_dollarSign() {
		PropertyChain pc = createPropertiesChain(true,
				"p1", "P1",
				"p2", "$${p1}");
		
		Assert.assertEquals("${p1}", pc.get("p2"));
	}
	
	@Test
	public void propertyChain_canScope_withRecursiveInterpolation() {
		PropertyChain pc = createPropertiesChain(true,
				"p1", "${p2}",
				"p2", "${p1}");
		
		Assert.assertEquals("${p2}", pc.get("p1"));
	}
	
	@Test
	public void canInterpolate_envVariable() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setEnv("MY_ENV", "myVal");
			
			PropertyChain chain = createPropertiesChain(true,
				"p1", "v1 ${env.MY_ENV}");
			
			Assert.assertEquals("v1 myVal", chain.get("p1"));
		}
	}

	@Test
	public void propertyInfo_isNull_forNonExistingKey() {
		PropertyChain pc = createPropertiesChain(true,
				"p1", "v1");
		
		PropertyInfo pi = pc.getInfo("p2");
		Assert.assertNull(pi);
	}
	
	@Test
	public void canGet_propertyInfo_forRegularProperty() {
		PropertyChain pc = createPropertiesChain(true,
				"p1", "v1");
		
		PropertyInfo pi = pc.getInfo("p1");
		
		// Check PropertyInfo
		Assert.assertEquals("p1", pi.getKey());
		Assert.assertEquals("v1", pi.getValue());
		Assert.assertEquals("v1", pi.getRawValue());
		Assert.assertEquals("v1", pi.getDisplayValue());
		Assert.assertFalse(pi.isSensitive());
		Assert.assertEquals("PropertyChainBuilder.PropertiesPropertyEvaluator[]", pi.getEvaluatorInfo());
	}
	
	@Test
	public void canGet_propertyInfo_forInterpolatedValue() {
		PropertyChain pc = createPropertiesChain(true,
				"p1", "${p2}",
				"p2", "v2");
		
		PropertyInfo pi = pc.getInfo("p1");
		
		// Check PropertyInfo
		Assert.assertEquals("p1", pi.getKey());
		Assert.assertEquals("v2", pi.getValue());
		Assert.assertEquals("${p2}", pi.getRawValue());
		Assert.assertEquals("v2", pi.getDisplayValue());
		Assert.assertFalse(pi.isSensitive());
		Assert.assertEquals("PropertyChainBuilder.PropertiesPropertyEvaluator[]", pi.getEvaluatorInfo());
	}
	
	@Test
	public void propertyChain_getPropertyInfo_forEncryptedValue() {
		JuTextEncryptor encryptor = JuSecurityUtils.buildEncryptor()
			.password("secret")
			.createTextEncryptor();

		//encryptor.encrypt("secret String");
		//Sample value: bSmw4g8BdopiLClgC7zU2Kwr0LyRqj79
		
		PropertyChain chain = new PropertyChainBuilder()
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest_encryption.properties", false)
			.setDecryptor(encryptor)
			.interpolation().enable(true).done()
			.getPropertyChain();
		
		PropertyInfo pi = chain.getInfo("encString");
		
		// Check PropertyInfo
		Assert.assertEquals("encString", pi.getKey());
		Assert.assertEquals("secret String", pi.getValue());
		Assert.assertEquals("ENC(bSmw4g8BdopiLClgC7zU2Kwr0LyRqj79)", pi.getRawValue());
		Assert.assertEquals("***", pi.getDisplayValue());
		Assert.assertTrue(pi.isSensitive());
		Assert.assertTrue(pi.getEvaluatorInfo().startsWith("PropertyChainBuilder.PropertiesPropertyEvaluator[url="));
	}
	
	static PropertyChain createPropertiesChain(boolean enableInterpolation, String... keyValuePairs) {
		return new PropertyChainBuilder()
			.addListPropertyEvaluator(keyValuePairs)
			.interpolation()
				.enable(enableInterpolation)
				.done()
			.getPropertyChain();
	}
}
