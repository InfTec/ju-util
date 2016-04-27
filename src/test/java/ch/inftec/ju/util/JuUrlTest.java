package ch.inftec.ju.util;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JuUrlTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void nonExistingResource_returnsNull() {
		Assert.assertNull(JuUrl.resource("blibla"));
	}
	
	@Test
	public void nonExistingResource_returnsNull_withBuilder() {
		Assert.assertNull(JuUrl.resource().get("blibla"));
	}
	
	@Test
	public void resource_usingClassLookup() {
		Assert.assertNotNull(JuUrl.resource().relativeTo(JuUrlTest.class).get("testResource.txt"));
	}
	
	@Test
	public void existingResourceRelativeToAndPrefixed() {
		expectContents(
				"JuUrlTest_testResource.txt",
				JuUrl.existingResourceRelativeToAndPrefixed("testResource.txt", JuUrlTest.class));
	}

	private void expectContents(String expected, URL url) {
		String actual = new IOUtil().loadTextFromUrl(url);

		assertEquals(expected, actual);
	}

	@Test
	public void resource_usingClassLoader() {
		Assert.assertNotNull(JuUrl.resource().get("ch/inftec/ju/util/testResource.txt"));
	}
	
	@Test
	public void multipleResources() {
		Assert.assertEquals(2, JuUrl.resource().getAll("ju.properties.files").size());
	}
	
	@Test
	public void multipleResources_relativeToClass() {
		Assert.assertEquals(1, JuUrl.resource().relativeTo(JuUrlTest.class).getAll("testResource.txt").size());
	}
	
	@Test
	public void exception_ifSingle_withMultipleResources() {
		thrown.expect(JuRuntimeException.class);
		thrown.expectMessage("Found more than");
		thrown.expectMessage("ju.properties.files");
		
		JuUrl.resource().single().get("ju.properties.files");
	}
	
	@Test
	public void exception_ifExceptionIfNone_andNoResource() {
		thrown.expect(JuRuntimeException.class);
		thrown.expectMessage("Resource not found");
		thrown.expectMessage("blibla");
		
		JuUrl.resource().exceptionIfNone().get("blibla");
	}
	
	@Test
	public void existingFile() {
		Assert.assertTrue(Files.exists(JuUrl.existingFile("src/main/resources/ju.properties.files")));
	}
	
	@Test
	public void existingFolder() {
		Assert.assertTrue(Files.exists(JuUrl.existingFolder("src/main/resources")));
	}
	
	@Test(expected=JuRuntimeException.class)
	public void existingFolder_fails_onNonExistingPath() {
		Assert.assertTrue(Files.exists(JuUrl.existingFolder("src/main/resources/xyz")));
	}
	
	@Test
	public void existingFile_usingRelativeTo() {
		Assert.assertTrue(Files.exists(JuUrl.path().relativeTo("src/main/resources").get("ju.properties.files")));
	}
	
	@Test
	public void existingFileMethod_throwsException_ifFileNotExisting() {
		thrown.expect(JuRuntimeException.class);
		thrown.expectMessage("Path doesn't exist");
		thrown.expectMessage("blibla");
		
		JuUrl.existingFile("blibla");
	}
	
	@Test
	public void path_throwsException_ifPathIsNoFile() {
		thrown.expect(JuRuntimeException.class);
		thrown.expectMessage("Path is not a file");
		thrown.expectMessage("src");
		
		JuUrl.path().file().get("src/main");
	}
	
	/**
	 * Test to determine URL protocol for resource in JAR on classpath.
	 * <p>
	 * Motivation is to check for differences between resource lookup in a JBoss (vrf protocol)
	 * vs. resource lookup in standalone Java (jar:file). JBoss obviously cached the contents at startup which makes hot
	 * resource deployement impossible.
	 */
	@Test
	public void jarResourceLoading_test() {
		// JUnit JAR contains a LICENCE-junit.txt
		List<URL> urls = JuUrl.resource().getAll("LICENSE-junit.txt");
		URL jUnitLicense = null;
		for (URL url : urls) {
			if (url.toExternalForm().contains("junit")) {
				jUnitLicense = url;
				break;
			}
		}
		
		Assert.assertTrue(jUnitLicense.toExternalForm().startsWith("jar:file"));
	}
}