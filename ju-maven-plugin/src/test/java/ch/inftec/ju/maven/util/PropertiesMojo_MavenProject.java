package ch.inftec.ju.maven.util;

import java.io.File;
import java.util.Properties;

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

/**
 * Stub for the MavenProject object used for testing.
 * @author Martin
 *
 */
public class PropertiesMojo_MavenProject extends MavenProjectStub {
	private final Properties properties = new Properties();
	
	@Override
	public File getBasedir() {
		return new File("src/test/resources/test-poms/propertiesMojoTest");
	}
	
	public Properties getProperties() {
		return this.properties;
	}
}
