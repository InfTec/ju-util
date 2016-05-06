package ch.inftec.ju.maven.test;

import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.junit.Rule;
import org.junit.Test;


public class DebugPropertiesMojoTest {
	@Rule
	public MojoRule rule = new MojoRule();
	
	@Test
	public void canExecute_debugPropertiesMojo() throws Exception {
		PlexusConfiguration config = new DefaultPlexusConfiguration("configuration");
		XmlPlexusConfiguration xmlConfig = new XmlPlexusConfiguration("project");
		xmlConfig.setAttribute("implementation", "ch.inftec.ju.maven.test.GreetMojoTest_MavenProject");
		config.addChild(xmlConfig);
//		config.addChild("greeting", "configWorld");
		
		DebugMojo m = (DebugMojo) rule.configureMojo(new DebugMojo(), config);
		
		m.execute();
	}
}
