package ch.inftec.ju.maven.test;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ch.inftec.ju.util.JuUrl;
import ch.inftec.ju.util.xml.XPathGetter;
import ch.inftec.ju.util.xml.XmlUtils;

/**
 * JUnit3 test cases for plugin tests.
 * <p>
 * <b>Note:</b> mvn install needs to be executed before these test cases will succeed in an IDE
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 */
@Ignore("Somehow not working on Bamboo after Maven upgrade...")
public class GreetMojoJUnit3Test extends AbstractMojoTestCase {
//	protected void setUp() throws Exception {
//		super.setUp();
//	}
//
//	protected void tearDown() throws Exception {
//		super.tearDown();
//	}
	
	/**
	 * Tests the injection of a property using a pom testing file.
	 */
	public void testGreetingFromPom() throws Exception {
		GreetMojo greetMojo = (GreetMojo) this.lookupMojo("greet", new File("src/test/resources/test-poms/greetMojoTest/pom.xml"));
		Assert.assertEquals("MojoTestWorld", greetMojo.getGreeting());
		
		greetMojo.execute();
	}

	/**
	 * Tests the injection of a maven project stub implementation.
	 * <p>
	 * When testing, objects like ${project} have to be supplied as stubs if they are
	 * required by the plugin.
	 */
	public void testProjectInjection() throws Exception {
		GreetMojo greetMojo = (GreetMojo) this.lookupMojo("greet", new File("src/test/resources/test-poms/greetMojoTest/pom.xml"));
		
		Assert.assertNotNull(greetMojo.getProject());
		Assert.assertEquals(GreetMojoTest_MavenProject.class, greetMojo.getProject().getClass());
	}

	@Test
	public void testGreetingWithoutPom() throws Exception {
		// lookupMojo needs the actual version as it seems to use the Maven local repo to lookup stuff...
		String version = new XPathGetter(XmlUtils.loadXml(JuUrl.toUrl(JuUrl.existingFile("pom.xml")))).getSingle("project/parent/version");
		
		// Default value setting will not work when using explicit (or no) PlexusConfiguration
		PlexusConfiguration config = new DefaultPlexusConfiguration("configuration");
		config.addChild("greeting", "NoPomWorld");
		GreetMojo greetMojo = (GreetMojo) this.lookupMojo("ch.inftec.ju", "ju-maven-plugin", version, "greet", config);
		Assert.assertEquals("NoPomWorld", greetMojo.getGreeting());
		greetMojo.execute();
	}
}
