package ch.inftec.ju.maven.util;

import java.io.File;
import java.util.Properties;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import ch.inftec.ju.util.SystemPropertyTempSetter;


public class PropertiesMojoTest {
	@Rule
	public MojoRule rule = new MojoRule();
	
	@Test
	public void canConfigureMojo_fromPom() throws Exception {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("ju.mojoProp", "origValue");
			
			PropertiesMojo propertiesMojo = (PropertiesMojo) rule.configureMojo(new PropertiesMojo(), "ju-maven-plugin", new File("src/test/resources/test-poms/propertiesMojoTest/pom.xml"));
			propertiesMojo.execute();
			
			Assert.assertEquals("mojoVal", propertiesMojo.getProject().getProperties().get("ju.mojoProp"));
		}
	}
	
	@Test
	public void canFilterProperties() throws Exception {
		PropertiesMojo propertiesMojo = (PropertiesMojo) rule.configureMojo(new PropertiesMojo(), "ju-maven-plugin", new File("src/test/resources/test-poms/propertiesMojoTest-filter/pom.xml"));
		propertiesMojo.execute();
		
		Properties props = propertiesMojo.getProject().getProperties();
		Assert.assertEquals(2, props.size());
		Assert.assertEquals("val1", props.get("incProp1"));
		Assert.assertEquals("val2", props.get("incProp2"));
	}
	
	@Test
	public void supports_relativeResourceNames() throws Exception {
		PropertiesMojo propertiesMojo = (PropertiesMojo) rule.configureMojo(new PropertiesMojo(), "ju-maven-plugin", new File("src/test/resources/test-poms/propertiesMojoTest-relative/pom.xml"));
		propertiesMojo.execute();
		
		Properties props = propertiesMojo.getProject().getProperties();
		Assert.assertEquals(1, props.size());
		Assert.assertEquals("relProp", props.get("relKey"));
	}
	
	@Test
	public void canSpecify_decryptor() throws Exception {
		PropertiesMojo propertiesMojo = (PropertiesMojo) rule.configureMojo(new PropertiesMojo(), "ju-maven-plugin", new File("src/test/resources/test-poms/propertiesMojoTest-decryptor/pom.xml"));
		propertiesMojo.execute();
		
		Properties props = propertiesMojo.getProject().getProperties();
		Assert.assertEquals(2, props.size());
		Assert.assertEquals("String", props.get("encProperty"));
	}
	
	@Test
	public void canExport_property_toSystemProperties() throws Exception {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("propYesSysYes", "p1");
			ts.setProperty("propYesSysNo", "p2");
			ts.setProperty("propNoSysYes", "p3");
			ts.setProperty("propNoSysNo", "p4");
			
			PropertiesMojo propertiesMojo = (PropertiesMojo) rule.configureMojo(new PropertiesMojo(), "ju-maven-plugin", new File("src/test/resources/test-poms/propertiesMojoTest-exportSystemProperty/pom.xml"));
			propertiesMojo.execute();
			
			// Check maven property export
			Assert.assertEquals(2, propertiesMojo.getProject().getProperties().size());
			Assert.assertEquals("propYesSysYesVal", propertiesMojo.getProject().getProperties().get("propYesSysYes"));
			Assert.assertEquals("propYesSysNo", propertiesMojo.getProject().getProperties().get("propYesSysNo"));
			
			// Check system property export
			Assert.assertEquals("propYesSysYesVal", System.getProperty("propYesSysYes"));
			Assert.assertEquals("p2", System.getProperty("propYesSysNo"));
			Assert.assertEquals("p3", System.getProperty("propNoSysYes"));
			Assert.assertEquals("p4", System.getProperty("propNoSysNo"));
		}
	}
	
	@Test
	public void canSet_andInterpolate_properties() throws Exception {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("propSys", "p1");
			ts.setProperty("propMav", "p2");
			ts.setProperty("myKey", "p3");
			
			PropertiesMojo propertiesMojo = (PropertiesMojo) rule.configureMojo(new PropertiesMojo(), "ju-maven-plugin", new File("src/test/resources/test-poms/propertiesMojoTest-setAndInterpolateProperty/pom.xml"));
			propertiesMojo.execute();
			
			// Check maven property export
			Assert.assertEquals(5, propertiesMojo.getProject().getProperties().size()); // 2 from properties file, 3 from configuration
			Assert.assertEquals("myKey", propertiesMojo.getProject().getProperties().get("keyProp"));
			Assert.assertEquals("myVal", propertiesMojo.getProject().getProperties().get("valProp"));
			Assert.assertEquals("valSys", propertiesMojo.getProject().getProperties().get("propSys"));
			Assert.assertEquals("myVal", propertiesMojo.getProject().getProperties().get("propMav"));
			Assert.assertEquals("myVal", propertiesMojo.getProject().getProperties().get("myKey"));
			
			// Check system property export
			Assert.assertEquals("valSys", System.getProperty("propSys"));
			Assert.assertEquals("p2", System.getProperty("propMav"));
			Assert.assertEquals("p3", System.getProperty("myKey"));
		}
	}
	
	@Test
	public void canSet_properties_inPomOnly() throws Exception {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			// Make sure the property doesn't get overwritten by a System Property unless we want to
			ts.setProperty("myProp", "sysVal");
			
			PropertiesMojo propertiesMojo = (PropertiesMojo) rule.configureMojo(new PropertiesMojo(), "ju-maven-plugin", new File("src/test/resources/test-poms/propertiesMojoTest-setPropertiesInPomOnly/pom.xml"));
			propertiesMojo.execute();
				
			// Check maven property export
			Assert.assertEquals(1, propertiesMojo.getProject().getProperties().size());
			Assert.assertEquals("myVal", propertiesMojo.getProject().getProperties().get("myProp"));
		}
	}
	
	@Test
	public void canSpecify_systemPropertyChainElment() throws Exception {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			// Make sure the property doesn't get overwritten by a System Property unless we want to
			ts.setProperty("myProp", "sysVal");
			
			PropertiesMojo propertiesMojo = (PropertiesMojo) rule.configureMojo(new PropertiesMojo(), "ju-maven-plugin", new File("src/test/resources/test-poms/propertiesMojoTest-systemPropertyChainElement/pom.xml"));
			propertiesMojo.execute();
				
			// Check maven property export
			Assert.assertEquals(1, propertiesMojo.getProject().getProperties().size());
			Assert.assertEquals("sysVal", propertiesMojo.getProject().getProperties().get("myProp"));
		}
	}
	
	@Test
	public void canIgnore_missingDecryption() throws Exception {
		PropertiesMojo propertiesMojo = (PropertiesMojo) rule.configureMojo(new PropertiesMojo(), "ju-maven-plugin", new File("src/test/resources/test-poms/propertiesMojoTest-ignoreMissingDecryption/pom.xml"));
		propertiesMojo.execute();
		
		// Expect no exception, check if property was successfully set
		Assert.assertEquals(1, propertiesMojo.getProject().getProperties().size());
		Assert.assertEquals("myVal", propertiesMojo.getProject().getProperties().get("myProp"));
	}
}