package ch.inftec.ju.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class SystemPropertyTempSetterTest {
	private static final String PROP = "ju-util.SystemPropertyTempSetterTest";
	private static final String DEFINED_ENV = "PATH";
	private static final String UNDEFINED_ENV = "SOME_UNDEFINED_ENV";
	
	@Test
	public void canHandleStrings_property() {
		System.setProperty(PROP, "v1");
		
		try (SystemPropertyTempSetter s = new SystemPropertyTempSetter()) {
			s.setProperty(PROP, "v1b");
			Assert.assertEquals("v1b", System.getProperty(PROP));
		}
		
		Assert.assertEquals("v1", System.getProperty(PROP));
	}
	
	@Test
	public void canHandleNull1_property() {
		System.clearProperty(PROP);
		
		try (SystemPropertyTempSetter s = new SystemPropertyTempSetter()) {
			s.setProperty(PROP, "v2");
			Assert.assertEquals("v2", System.getProperty(PROP));
		}
		
		Assert.assertNull(System.getProperty(PROP));
	}
	
	@Test
	public void canHandleNull2_property() {
		System.setProperty(PROP, "v2");
		
		try (SystemPropertyTempSetter s = new SystemPropertyTempSetter()) {
			s.setProperty(PROP, null);
			Assert.assertNull(System.getProperty(PROP));
		}
		
		Assert.assertEquals("v2", System.getProperty(PROP));
	}
	
	@Test
	public void canHandleStrings_Env() {
		String origVal = System.getenv(DEFINED_ENV);
		Assert.assertTrue(StringUtils.isNotEmpty(origVal));
		
		try (SystemPropertyTempSetter s = new SystemPropertyTempSetter()) {
			s.setEnv(DEFINED_ENV, "test");
			Assert.assertEquals("test", System.getenv(DEFINED_ENV));
		}
		
		Assert.assertEquals(origVal, System.getenv(DEFINED_ENV));
	}
	
	@Test
	public void canHandleNull1_Env() {
		String undefinedEnv = System.getenv(UNDEFINED_ENV);
		Assert.assertNull(undefinedEnv);
		
		try (SystemPropertyTempSetter s = new SystemPropertyTempSetter()) {
			s.setEnv(UNDEFINED_ENV, "v");
			Assert.assertEquals("v", System.getenv(UNDEFINED_ENV));
		}
		
		Assert.assertNull(System.getenv(UNDEFINED_ENV));
	}
	
	@Test
	public void nullEnv_throwsException() {
		String origVal = System.getenv(DEFINED_ENV);
		
		try (SystemPropertyTempSetter s = new SystemPropertyTempSetter()) {
			s.setEnv(DEFINED_ENV, null);
			Assert.assertNull(System.getenv(DEFINED_ENV));
		}
		
		Assert.assertEquals(origVal, System.getenv(DEFINED_ENV));
	}
	
	@Test
	public void canHandleEmpty_Env() {
		String origVal = System.getenv(DEFINED_ENV);
		
		try (SystemPropertyTempSetter s = new SystemPropertyTempSetter()) {
			s.setEnv(DEFINED_ENV, "");
			Assert.assertTrue(System.getenv(DEFINED_ENV).isEmpty());
		}
		
		Assert.assertEquals(origVal, System.getenv(DEFINED_ENV));
	}
}
