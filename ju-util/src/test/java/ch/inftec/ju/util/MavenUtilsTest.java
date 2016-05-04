package ch.inftec.ju.util;


import org.junit.Assert;
import org.junit.Test;

public class MavenUtilsTest {
	@Test
	public void canGetVersion() {
		String version = MavenUtils.getVersion("org.slf4j", "slf4j-api");
		
		// Version is not fix - we'll check if major version is equal to or greater than 1...
		Long majorVersion = Long.parseLong(version.substring(0, version.indexOf(".")));
		Assert.assertTrue(majorVersion >= 1);
	}
}
