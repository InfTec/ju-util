package ch.inftec.ju.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.w3c.dom.Document;

import ch.inftec.ju.util.xml.XmlUtils;

public class TestUtilsTest {
	/**
	 * Maven will find the nested class and execute the test, so we need a flag
	 * to make sure it fails only when it should.
	 */
	private static boolean doTestFail = false;
	
	@Test
	public void canRunJUnitTests() {
		SuccessfulTest.run = false;
		TestUtils.runJUnitTests(SuccessfulTest.class);
		Assert.assertTrue(SuccessfulTest.run);
	}
	
	@Test(expected=JuRuntimeException.class)
	public void failingTests_fail() {
		try {
			doTestFail = true;
			TestUtils.runJUnitTests(FailingTest.class);
		} finally {
			doTestFail = false;
		}
	}
	
	public static class SuccessfulTest {
		public static boolean run = false;
		
		@Test
		public void setRun_toTrue() {
			run = true;
		}
	}
	
	public static class FailingTest {
		@Test
		public void throw_juRuntimeException() {
			Assume.assumeTrue(doTestFail);
			throw new JuRuntimeException("failing");
		}
	}
	
	@Test
	public void canCompare_xmlDocument_toResource() {
		Document doc = XmlUtils.buildXml("root").addChild("child").endChild().getDocument();
		TestUtils.assertEqualsXmlResource("TestUtilsTest_canCompare_xmlDocument_toResource.xml", doc);
	}
	
	@Test
	public void canCompare_xml_toResource() {
		String xml = "<root><child/></root>";
		TestUtils.assertEqualsXmlResource("TestUtilsTest_canCompare_xmlDocument_toResource.xml", xml);
	}

	@Test
	public void assertion_canCompare_xml() {
		String xml = "<root><child/></root>";

		TestUtils.assertion().xml()
				.expectedResource(JuUrl.existingResourceRelativeToAndPrefixed("canCompare_xmlDocument_toResource.xml", this.getClass()))
				.actualXml(xml)
				.assertEquals();
	}

	@Test(expected = Throwable.class)
	public void assertion_withUnequalXml_throwsError() {
		String xml = "<root><child2/></root>";

		TestUtils.assertion().xml()
				.expectedResource(JuUrl.existingResourceRelativeToAndPrefixed("canCompare_xmlDocument_toResource.xml", this.getClass()))
				.actualXml(xml)
				.assertEquals();
	}

	@Test
	public void assertion_canExport_xml() {
		String xml = "<root/>";

		Path exportPath = Paths.get("target/TestUtilsTest_assertion_canExport_xml.xml");
		IOUtil.deleteFile(exportPath);

		//@formatter:off
		TestUtils.assertion().xml()
				// Don't specify expected resource URL for export
				.actualXml(xml)
				.export()
					.enable(true)
					.exportFilePath(exportPath)
					.done()
				.assertEquals();
		//@formatter:on

		// Should have created a file
		Assert.assertTrue(IOUtil.exists().file(exportPath));

		// Check contents of file
		String exportedXml = new IOUtil().loadTextFromUrl(JuUrl.toUrl(exportPath));
		Assert.assertEquals(xml, exportedXml);

		IOUtil.deleteFile(exportPath);
	}
}
