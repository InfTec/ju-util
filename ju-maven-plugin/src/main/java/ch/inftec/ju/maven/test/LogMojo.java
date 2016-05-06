package ch.inftec.ju.maven.test;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JU Maven Plugin used for testing purposes.
 * @author Martin
 *
 */
@Mojo(name = "log")
public class LogMojo extends AbstractMojo {
	private Logger logger = LoggerFactory.getLogger(LogMojo.class);
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		logger.trace("slf4j trace logging (most detailed)");
		logger.debug("slf4j debug logging (after trace)");
		logger.info("slf4j info logging (after trace, debug)");
		logger.warn("slf4j warn logging (after trace, debug, info");
		logger.error("slf4j error logging (after trace, debug, info, warn");
		
		this.getLog().debug("getLog debug logging (most detailed, after slf4j logs");
		this.getLog().info("getLog info logging (most detailed, after slf4j logs");
		this.getLog().warn("getLog warn logging (most detailed, after slf4j logs");
		this.getLog().error("getLog error logging (most detailed, after slf4j logs");
	}
}
