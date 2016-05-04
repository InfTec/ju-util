package ch.inftec.ju.util;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.inftec.ju.util.function.Predicate;

public class IOTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void loadTextFromUrl() throws Exception {
		String loadedString = new IOUtil("UTF-8").loadTextFromUrl(JuUrl.resource().relativeTo(IOTest.class).get("testResource.txt"));

		String expectedString = "First line\n";
		expectedString += "Second line\n";
		expectedString += "äöüéèãâ";

		assertEquals(loadedString, expectedString);
	}

	/**
	 * Tests the TestUtils.assertEqualsResource method.
	 */
	@Test
	public void assertEqualsResource() {
		String expectedString = "First line\n";
		expectedString += "Second line\n";
		expectedString += "äöüéèãâ";

		// Test assertEqualsResource method
		TestUtils.assertEqualsResource("testResource.txt", expectedString);

		// Test assertEqualsResource method with carriage return / line feed
		String expectedStringCrLf = "First line\r\n";
		expectedStringCrLf += "Second line\r\n";
		expectedStringCrLf += "äöüéèãâ";

		TestUtils.assertEqualsResource("testResource.txt", expectedStringCrLf);
	}

	@Test
	public void loadTextFromUrlReplacement() throws Exception {
		String loadedString = new IOUtil("UTF-8").loadTextFromUrl(
				JuUrl.resource().relativeTo(IOTest.class).get("testResource_replacements.txt"), "key1", "###val1###");

		String expectedString = "First line ###val1###\n";
		expectedString += "Second %key2% line\n";
		expectedString += "äöüéèãâ";

		assertEquals(loadedString, expectedString);

		// Test assertEqualsResource method
		TestUtils.assertEqualsResource("testResource_replacements.txt", expectedString, "key1", "###val1###");
	}

	@Test(expected = ComparisonFailure.class)
	public void assertEqualsResource_fail() {
		TestUtils.assertEqualsResource("testResource.txt", "blibla");
	}

	@Test
	public void toNewLineUnix() {
		Assert.assertEquals("someString", IOUtil.toNewLineUnix("someString"));

		String targetString = "line1\nline2";
		Assert.assertEquals(targetString, IOUtil.toNewLineUnix(targetString));
		Assert.assertEquals(targetString, IOUtil.toNewLineUnix("line1\r\nline2"));
		Assert.assertEquals(targetString, IOUtil.toNewLineUnix("line1\rline2"));
	}

	@Test
	public void writeTextToFile() throws Exception {
		String text = "First line\n";
		text += "Second line\n";
		text += "äöüéèãâ";

		Path tmpFile = IOUtil.getTemporaryFile();

		new IOUtil().writeTextToFile(text, tmpFile, true);

		String loadedText = new IOUtil().loadTextFromUrl(JuUrl.toUrl(tmpFile));

		TestUtils.assertEqualsResource("testResource.txt", loadedText);
	}

	/**
	 * Tests the loading of properties from a resource.
	 */
	@Test
	public void loadProperties() throws Exception {
		Properties props = new IOUtil("UTF-8").loadPropertiesFromUrl(JuUrl.resource().relativeTo(IOTest.class).get("test.properties"));
		Assert.assertEquals("First line", props.getProperty("prop1"));
		Assert.assertEquals("äöüéèãâ", props.getProperty("prop2"));
	}

	@Test
	public void canDeleteFile() throws Exception {
		Path tempFile = IOUtil.getTemporaryFile();

		Assert.assertTrue(Files.exists(tempFile));
		IOUtil.deleteFile(tempFile);
		Assert.assertFalse(Files.exists(tempFile));
	}

	@Test
	public void canWriteToFile_overwriting() throws Exception {
		Path tempFile = IOUtil.getTemporaryFile();
		new IOUtil().writeTextToFile("temp", tempFile, true);

		new IOUtil().writeTextToFile("overwrite", tempFile, true);

		String text = new IOUtil().loadTextFromUrl(JuUrl.toUrl(tempFile));
		Assert.assertEquals("overwrite", text);
	}

	@Test
	public void canOpenWriter_overwriting() throws Exception {
		Path tempFile = IOUtil.getTemporaryFile();
		new IOUtil().writeTextToFile("temp", tempFile, true);

		try (Writer w = new IOUtil().openWriter(tempFile, false, true)) {
			w.write("overwrite");
		}

		String text = new IOUtil().loadTextFromUrl(JuUrl.toUrl(tempFile));
		Assert.assertEquals("overwrite", text);
	}

	@Test
	public void canOpenWriter_appending() throws Exception {
		Path tempFile = IOUtil.getTemporaryFile();
		new IOUtil().writeTextToFile("temp", tempFile, true);

		try (Writer w = new IOUtil().openWriter(tempFile, true, false)) {
			w.write("append");
		}

		String text = new IOUtil().loadTextFromUrl(JuUrl.toUrl(tempFile));
		Assert.assertEquals("tempappend", text);
	}

	@Test
	public void canOpenWriter_newFile() throws Exception {
		Path tempFile = Paths.get("target/tmp/IOTest_canOpenWriter_newFile.txt");
		IOUtil.deleteFile(tempFile);

		try (Writer w = new IOUtil().openWriter(tempFile, true, false)) {
			w.write("new");
		}

		String text = new IOUtil().loadTextFromUrl(JuUrl.toUrl(tempFile));
		Assert.assertEquals("new", text);
	}

	@Test
	public void serializableClass_isSerializable() {
		Assert.assertTrue(IOUtil.isSerializable(3L));
	}

	@Test
	public void serializableClass_containingNonSerializableObjects_isNotSerializable() {
		Assert.assertFalse(IOUtil.isSerializable(new NonSerializableClass()));
	}

	@Test
	public void null_isSerializable() {
		Assert.assertTrue(IOUtil.isSerializable(null));
	}

	public static class NonSerializableClass implements Serializable {
		public Constructor<?> m = this.getClass().getConstructors()[0];
	}

	@Test
	public void canList_allFiles() {
		List<Path> allFiles = IOUtil.listFiles(Paths.get("src/test/resources/ch/inftec/ju/util/listFiles"));
		Assert.assertEquals(3, allFiles.size());
	}

	@Test
	public void canListFiles_UsingPredicates() {
		List<Path> xmlFiles = IOUtil.listFiles(Paths.get("src/test/resources/ch/inftec/ju/util/listFiles"), new Predicate<Path>() {
			@Override
			public boolean test(Path input) {
				return input.getFileName().getFileName().toString().endsWith(".xml");
			}
		});
		Assert.assertEquals(1, xmlFiles.size());
	}

	@Test
	public void canListFiles_byEnding() {
		List<Path> xmlFiles = IOUtil.listFiles(Paths.get("src/test/resources/ch/inftec/ju/util/listFiles"), ".xml");
		Assert.assertEquals(1, xmlFiles.size());
	}

	@Test
	public void exists_returnsTrue_forExistingFile_relativePath() {
		Assert.assertTrue(IOUtil.exists().file("src/test/resources/ju.properties.files"));
	}

	@Test
	public void exists_returnsTrue_forExistingFile_usingUrl() {
		URL url = JuUrl.toUrl(Paths.get("src/test/resources/ju.properties.files"));
		Assert.assertTrue(IOUtil.exists().file(url.toExternalForm()));
	}

	@Test
	public void exists_returnsFalse_forMissingFile_relativePath() {
		Assert.assertFalse(IOUtil.exists().file("fooBar"));
	}

	@Test
	public void exists_returnsFalse_forExistingDirectory_relativePath() {
		Assert.assertFalse(IOUtil.exists().file("src"));
	}

	@Test
	public void exists_returnsFalse_forInvalidPath() {
		Assert.assertFalse(IOUtil.exists().file("bla:"));
	}

	@Test
	public void exists_throwsException_forInvalidPath_ifActivated() {
		thrown.expect(JuRuntimeException.class);

		Assert.assertFalse(IOUtil.exists().noExceptions(false).file("file:?"));
	}
}
