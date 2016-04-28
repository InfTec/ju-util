package ch.inftec.ju.util;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Java (JVM) related utility functions.
 * @author martin.meyer@inftec.ch
 *
 */
public class JuJavaUtils {
	/**
	 * Dynamically adds the specified JAR to the system classpath.
	 * @param jarUrl URL to JAR to be added to the system classpath
	 */
	public static void addJarToClasspath(URL jarUrl) {
		try {
			URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			Class<?> sysclass = URLClassLoader.class;
	
			Method method = sysclass.getDeclaredMethod("addURL", new Class<?>[]{URL.class});
			method.setAccessible(true);
			method.invoke(sysloader, new Object[] { jarUrl });
		} catch (Exception ex) {
			throw new JuRuntimeException("Error, could not add URL to system classloader: " + jarUrl, ex);
		}
	}
}
