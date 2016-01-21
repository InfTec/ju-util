package ch.inftec.ju.util;

import java.net.URL;
import java.util.List;

/**
 * Logging related utility functions.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 */
public class JuLogUtils {
	/**
	 * Helper method to output all log4j configuration files on the classpath to system.err
	 * <p>
	 * Helpful for debugging when we have multiple configuration files on the classpath
	 * <p>
	 * Note: Setting <strong>-Dlog4j.debug<strong>, Log4J will also output debug info on how it is configured
	 */
	public static void showLog4jConfigFiles() {
		List<URL> configFiles = JuUrl.resource().getAll("log4j.xml");
		
		System.err.println("Log4J Config Files on Classpath:");
		for (URL configFile : configFiles) {
			System.err.println(configFile);
		}
	}
}
