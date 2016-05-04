package ch.inftec.ju.util;

import java.net.URL;
import java.util.Properties;

/**
 * Utility methods related to Maven.
 * @author martin.meyer@inftec.ch
 *
 */
public class MavenUtils {
	private static String MAVEN_META_INF = "META-INF/maven";
	
	public static String getVersion(String groupId, String artifactId) {
		try {
			// Get the version by the pom.properties in the maven meta-inf folder
			URL pomProps = JuUrl.existingResource(String.format("%s/%s/%s/pom.properties", MavenUtils.MAVEN_META_INF, groupId, artifactId));
			Properties props = new IOUtil().loadPropertiesFromUrl(pomProps);
			return props.getProperty("version");
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't evaluate maven version", ex);
		}
	}
}
