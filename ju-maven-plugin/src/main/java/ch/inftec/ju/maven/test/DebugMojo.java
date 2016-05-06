package ch.inftec.ju.maven.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Debug mojo providing Maven debugging functionality.
 * <p>
 * Binds to the initialize phase by default.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 */
@Mojo(name="debug", defaultPhase=LifecyclePhase.INITIALIZE)
public class DebugMojo extends AbstractMojo {
//	@Parameter(defaultValue="${project}") // Alternative notation
	@Component
	private MavenProject project;

	/**
	 * Object that holds properties to configure the printing of properties.
	 */
	@Parameter
	private PrintPropertiesConfig printProperties;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		boolean outputPerformed = false;

		if (this.printProperties != null) {
			if (this.isPrintMavenProperties()) {
				// Display all Maven properties
				this.printProperties("Maven", this.project.getProperties());
				outputPerformed = true;
			}
			
			if (this.isPrintSystemProperties()) {
				// Display all System properties
				this.printProperties("System", System.getProperties());
				outputPerformed = true;
			}
		}
		
		if (!outputPerformed) {
			this.getLog().warn("No output was configured on the JU Debug Plugin");
		}
			
	}
	
	private void printProperties(String name, Properties props) {
		// Display all Maven properties
		this.getLog().info(String.format("Total %s Properties: %d", name, props.size()));
		
		List<String> keys = new ArrayList<>();
		for (Enumeration<Object> e = props.keys(); e.hasMoreElements(); ) {
			Object key = e.nextElement();
			keys.add(ObjectUtils.toString(key));
		}
		if (this.isSortProperties()) {
			Collections.sort(keys);
		}
		
		for (String key : keys) {
			Object value = props.get(key);

			this.getLog().info(String.format("  %s=%s", key, value));			
		}
	}
	
	public boolean isPrintMavenProperties() {
		return this.printProperties != null && this.printProperties.maven;
	}
	
	public boolean isPrintSystemProperties() {
		return this.printProperties != null && this.printProperties.system;
	}
	
	public boolean isSortProperties() {
		return this.printProperties != null && this.printProperties.sort;
	}
	
	public static final class PrintPropertiesConfig {
		/**
		 * If true, Maven properties are printed to the log.
		 */
		@Parameter(defaultValue="${ju.debug.printProperties.maven}")
		private boolean maven;
		
		/**
		 * If true, Maven properties are printed to the log.
		 */
		@Parameter(defaultValue="${ju.debug.printProperties.system}")
		private boolean system;
		
		/**
		 * If true, property output is sorted by name. If false, they are returned just as they are
		 * retrieved from the Properties object.
		 */
		@Parameter(defaultValue="${ju.debug.printProperties.sort}")
		private boolean sort;
	}
}
