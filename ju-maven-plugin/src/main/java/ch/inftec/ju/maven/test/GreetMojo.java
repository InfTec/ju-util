package ch.inftec.ju.maven.test;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Testing Mojo that can output a greeting.
 * <p>
 * Configure the greeting using either <code>configuration/greeting</code>
 * or the property <code>ju.greeting</code>
 * @author Martin
 *
 */
@Mojo(name = "greet")
public class GreetMojo extends AbstractMojo {
	@Parameter(property="ju.greeting", defaultValue="World")
	private String greeting;
	
//	@Parameter(defaultValue="${project}") // Alternative notation
	@Component
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info(String.format("Hello there, %s!", this.greeting));
	}
	
	public String getGreeting() {
		return this.greeting;
	}
	
	public MavenProject getProject() {
		return this.project;
	}
}
