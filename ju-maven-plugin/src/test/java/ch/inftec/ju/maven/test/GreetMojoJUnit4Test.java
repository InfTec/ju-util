package ch.inftec.ju.maven.test;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;


public class GreetMojoJUnit4Test {
	@Rule
	public MojoRule rule = new MojoRule();
	
	@Ignore("Somehow not working on Bamboo after Maven upgrade...")
	@Test
	public void canLookupMojo_fromPom() throws Exception {
		GreetMojo greetMojo = (GreetMojo) rule.lookupMojo("greet", new File("src/test/resources/test-poms/greetMojoTest/pom.xml"));
		Assert.assertEquals("MojoTestWorld", greetMojo.getGreeting());
		
		greetMojo.execute();
	}
	
	@Test
	public void canConfigureMojo_fromPom() throws Exception {
		GreetMojo greetMojo = (GreetMojo) rule.configureMojo(new GreetMojo(), "ju-maven-plugin", new File("src/test/resources/test-poms/greetMojoTest/pom.xml"));
		Assert.assertEquals("MojoTestWorld", greetMojo.getGreeting());
	}
	
	/**
	 * Default values are not set when providing an explicit config...
	 */
	@Test
	public void defaultValues_areNotSet_usingConfig() throws Exception {
		PlexusConfiguration config = new DefaultPlexusConfiguration("configuration");
		
		GreetMojo greetMojo = (GreetMojo) rule.configureMojo(new GreetMojo(), config);
		Assert.assertNull(greetMojo.getGreeting());
	}
	
	@Test
	public void canConfigureMojo_usingConfig() throws Exception {
		PlexusConfiguration config = new DefaultPlexusConfiguration("configuration");
		config.addChild("greeting", "configWorld");
		
		GreetMojo greetMojo = (GreetMojo) rule.configureMojo(new GreetMojo(), config);
		Assert.assertEquals("configWorld", greetMojo.getGreeting());
	}
	
	@Test
	public void canInjectProject_usingConfig() throws Exception {
		PlexusConfiguration config = new DefaultPlexusConfiguration("configuration");
		XmlPlexusConfiguration xmlConfig = new XmlPlexusConfiguration("project");
		xmlConfig.setAttribute("implementation", "ch.inftec.ju.maven.test.GreetMojoTest_MavenProject");
		config.addChild(xmlConfig);
		
		GreetMojo greetMojo = (GreetMojo) rule.configureMojo(new GreetMojo(), config);
		
		Assert.assertNotNull(greetMojo.getProject());
		Assert.assertEquals(GreetMojoTest_MavenProject.class, greetMojo.getProject().getClass());
	}
	
//	/**
//	 * Tests the injection of a property using a pom testing file.
//	 */
//	public void testGreetingFromPom() throws Exception {
//		GreetMojo greetMojo = (GreetMojo) this.lookupMojo("greet", new File("src/test/resources/test-poms/greetMojoTest/pom.xml"));
//		Assert.assertEquals("MojoTestWorld", greetMojo.getGreeting());
//	}
//
//	/**
//	 * Tests the injection of a maven project stub implementation.
//	 * <p>
//	 * When testing, objects like ${project} have to be supplied as stubs if they are
//	 * required by the plugin.
//	 */
//	public void testProjectInjection() throws Exception {
//		GreetMojo greetMojo = (GreetMojo) this.lookupMojo("greet", new File("src/test/resources/test-poms/greetMojoTest/pom.xml"));
//		
//		Assert.assertNotNull(greetMojo.getProject());
//		Assert.assertEquals(GreetMojoTest_MavenProject.class, greetMojo.getProject().getClass());
//	}
}
