package ch.inftec.ju.maven.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ch.inftec.ju.util.AssertUtil;

/**
 * Debug mojo providing Assert functionality.
 * <p>
 * Binds to the initialize phase by default.
 * @author martin.meyer@inftec.ch
 *
 */
@Mojo(name="assert", defaultPhase=LifecyclePhase.INITIALIZE)
public class AssertMojo extends AbstractMojo {
//	@Parameter(defaultValue="${project}") // Alternative notation
	@Component
	private MavenProject project;

	/**
	 * Object that holds properties to configure the printing of properties.
	 */
	@Parameter(alias="assert")
	private AssertConfig assertProp;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.assertProp != null) {
			for (ExpectedActualProperty prop: this.assertProp.equals) {
				AssertUtil.assertEquals(prop.expected, prop.actual);
			}
		}
	}
	
	public static final class AssertConfig {
		/**
		 * List of equals expressions.
		 */
		@Parameter
		private List<ExpectedActualProperty> equals = new ArrayList<>();
	}
	
	public static class ExpectedActualProperty {
		/**
		 * Expected value
		 */
		@Parameter
		public String expected;
		
		/**
		 * Actual value
		 */
		@Parameter
		public String actual;
	}
}
