package ch.inftec.ju.util;


import org.junit.Assert;
import org.junit.Test;

public class PropertyChainBuilderTest {
	@Test
	public void buildsSystemPropertyEvaluator() {
		PropertyChain chain = new PropertyChainBuilder()
			.addSystemPropertyEvaluator()
			.getPropertyChain();
		
		String key = "ch.inftec.ju.util.PropertyChainTest.prop1";
		System.setProperty(key, "val1");
		Assert.assertEquals("val1", chain.get(key));
	}
	
	@Test
	public void buildsResourcePropertyEvaluator() {
		PropertyChain chain = new PropertyChainBuilder()
			.addResourcePropertyEvaluator("ch/inftec/ju/util/PropertyChainTest.properties", false)
			.getPropertyChain();
		
		Assert.assertEquals("val1", chain.get("prop1"));
	}
	
	@Test
	public void canBuild_propertyChain_byPropertiesFiles_onClasspath() {
		PropertyChain chain = new PropertyChainBuilder()
			.addEvaluatorsByChainFiles()
				.name("ch/inftec/ju/util/propertyChain/classpathProps.files")
				.resolve()
			.getPropertyChain();
		
		Assert.assertEquals("classpathPropVal", chain.get("prop"));
	}
	
	@Test
	public void canBuild_propertyChain_byPropertiesFiles_onFileSystem() {
		PropertyChain chain = new PropertyChainBuilder()
			.addResourceFolder(JuUrl.existingFolder("src/test/nonCpResources"))
			.addEvaluatorsByChainFiles()
				.name("ch/inftec/ju/util/propertyChain/fileSystemProps_prop.files")
				.resolve()
			.getPropertyChain();
		
		Assert.assertEquals("fileSystemPropVal", chain.get("propFs"));
		Assert.assertEquals("classpathPropVal_onFs", chain.get("propCpFs"));
		Assert.assertEquals("classpathPropVal", chain.get("propCp"));
	}
	
	@Test
	public void supports_SystemProperties_inPropertiesFiels() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("key", "tempProp");
			
			PropertyChain chain = new PropertyChainBuilder()
				.addEvaluatorsByChainFiles()
					.name("ch/inftec/ju/util/propertyChain/classpathProps_sys.files")
					.resolve()
				.getPropertyChain();
		
			Assert.assertEquals("tempProp", chain.get("key"));
		}
	}
	
	@Test
	public void canFind_resoures_inFileSystem() {
		PropertyChain chain = new PropertyChainBuilder()
			.addResourceFolder(JuUrl.existingFolder("src/test/nonCpResources"))
			.addEvaluatorsByChainFiles()
				.name("ch/inftec/ju/util/propertyChain/fileSystemProps_prop.files")
				.resolve()
			.getPropertyChain();

		Assert.assertEquals("fileSystemPropVal", chain.get("propFs"));
	}
	
	@Test
	public void canAdd_listPropertyEvaluator() {
		PropertyChain chain = new PropertyChainBuilder()
			.addListPropertyEvaluator("p1", "val1")
			.getPropertyChain();

		Assert.assertEquals("val1", chain.get("p1"));
	}
	
	@Test
	public void canAdd_propertyChainPropertyEvaluator() {
		PropertyChain chain = new PropertyChainBuilder()
			.addListPropertyEvaluator("p1", "val1")
			.getPropertyChain();
		
		PropertyChain chain2 = new PropertyChainBuilder()
			.addPropertyChainPropertyEvaluator(chain)
			.getPropertyChain();

		Assert.assertEquals("val1", chain2.get("p1"));
	}
	
	@Test
	public void canInterpolate_inPropertiesFile_usingPercentageSign() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("propKey", "interpolate");
			
			PropertyChain chain = new PropertyChainBuilder()
				.addEvaluatorsByChainFiles()
					.name("ch/inftec/ju/util/propertyChain/interpolatePercentageSign.files")
					.resolve()
				.getPropertyChain();
		
			Assert.assertEquals("interpolated", chain.get("key"));
		}
	}
	
	@Test
	public void canInterpolate_inPropertiesFile_usingDollarSign() {
		try (SystemPropertyTempSetter ts = new SystemPropertyTempSetter()) {
			ts.setProperty("propKey", "interpolate");
			
			PropertyChain chain = new PropertyChainBuilder()
				.addEvaluatorsByChainFiles()
					.name("ch/inftec/ju/util/propertyChain/interpolateDollarSign.files")
					.resolve()
				.getPropertyChain();
		
			Assert.assertEquals("interpolated", chain.get("key"));
		}
	}
}
