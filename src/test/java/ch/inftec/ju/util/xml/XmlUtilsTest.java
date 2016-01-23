package ch.inftec.ju.util.xml;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ch.inftec.ju.util.JuException;
import ch.inftec.ju.util.JuUrl;
import ch.inftec.ju.util.TestUtils;

/**
 * Class containing XML related unit tests.
 * @author tgdmemae
 *
 */
public class XmlUtilsTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void loadXml() throws Exception {
		Document doc = XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("simple.xml"));
		
		Element root = doc.getDocumentElement();
		assertEquals(root.getTagName(), "root");
		
		Element element = (Element)root.getElementsByTagName("element").item(0);
		assertEquals(element.getNodeName(), "element");
				
		Element childElement1 = (Element)element.getElementsByTagName("childElement").item(0);
		assertEquals(childElement1.getNodeName(), "childElement");
		
		Node textElement = element.getElementsByTagName("textElement").item(0);
		assertEquals(textElement.getTextContent(), "This is a little text");
	}
	
	@Test
	public void loadXml_validated() throws Exception {
		XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("simpleValidated.xml"), JuUrl.resource().relativeTo(XmlUtilsTest.class).get("simple.xsd"));
	}
	
	@Test
	public void canValidate_validXml() throws Exception {
		String xml = "<root><childElement name=\"test\" value=\"test2\"/></root>";
		XmlUtils.validate(xml, XmlUtils.loadSchema(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("simple.xsd")));
	}
	
	@Test
	public void validate_throwsException_onInvalidXml() throws Exception {
		thrown.expect(JuException.class);
		thrown.expectMessage("rootbla");
		
		String xml = "<rootbla/>";
		XmlUtils.validate(xml, XmlUtils.loadSchema(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("simple.xsd")));
	}
	
	@Test(expected=JuException.class)
	public void loadXml_invalid() throws Exception {
		try {
			XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("simpleValidated.xml_invalid"));
		} catch (Exception ex) {
			Assert.fail(ex.toString());
		}
		
		XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("simpleValidated.xml_invalid"), JuUrl.resource().relativeTo(XmlUtilsTest.class).get("simple.xsd"));
	}
	
	private void xPathGetter(String resourceName) throws Exception {
		Document doc = XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get(resourceName));
		XPathGetter xg = new XPathGetter(doc);
		assertEquals("XPathGetter[node=#document,nodeValue=<null>]", xg.toString());
		
		assertArrayEquals(xg.getArray("//a1/*/@text"), new String[] {"B2", "B2", "B3"});
		assertArrayEquals(xg.getArrayLong("//*/@value"), new Long[] {1L, 2L, 2L});
		assertArrayEquals(xg.getDistinctArray("//b3/*/@value"), new String[] {"2"});
		assertArrayEquals(xg.getDistinctArrayLong("//b3/*/@value"), new Long[] {2L});
		assertEquals(xg.getGetter("//b3").getSingle("@text"), "B3");
		
		XPathGetter xgSubs[] = xg.getGetters("//b3/*");
		assertEquals(xgSubs.length, 2);
		assertEquals(xgSubs[0].getSingle("@text"), "C2");
		assertEquals("XPathGetter[node=c2,nodeValue=<null>]", xgSubs[0].toString());
		assertEquals(xgSubs[1].getSingle("@text"), "C3");
		assertEquals("XPathGetter[node=c3,nodeValue=<null>]", xgSubs[1].toString());
		
		assertEquals(xg.getNode("root"), doc.getDocumentElement());
		
		Node bNodes[] = xg.getNodes("//a1/*");
		assertEquals(bNodes.length, 3);
		assertEquals(bNodes[2].getNodeName(), "b3");
		
		assertEquals(xg.getSingle("/root/a1/@text"), "A1");
		assertEquals(xg.getSingleLong("/root/a1/b2/c1/@value"), new Long(1));
		
		assertEquals(3, xg.getCount("//a1/*"));
	}
	
	/**
	 * Test the XPathGetter on a XML without namespaces.
	 */
	@Test
	public void xPathGetter() throws Exception {
		this.xPathGetter("xPathGetter.xml");
	}
	
	/**
	 * Test the XPathGetter on a XML with namespace.
	 */
	@Test
	public void xPathGetter_namespace() throws Exception {
		this.xPathGetter("xPathGetter_namespace.xml");
	}
	
	/**
	 * Tests the toString method of XmlUtil.
	 */
	@Test
	public void xmlTtoString() {
		// Create XML document
		Document doc = XmlUtils.buildXml("root")
			.addChild("child1")
				.setAttribute("childAttr1", "val1")
				.setAttribute("childAttr2", "val2")
				.addText("Text1")
				.endChild()
			.addChild("child2")
				.addChild("subChild1")
				.addText("Text2")
				.endChild()
			.endChild()
		.getDocument();
		
		// Test simple XML, without declaration and indentation
		String simpleXml = XmlUtils.toString(doc, false, false);
		TestUtils.assertEqualsResource("xmlToString_simpleXml.xml", simpleXml);
		
		// Test complete XML, with declaration and indentation
		String fullXml = XmlUtils.toString(doc, true, true);
		TestUtils.assertEqualsResource("xmlToString_fullXml.xml", fullXml);
	}
	
	/**
	 * Tests the conversion of a String to an XML Document.
	 */
	@Test
	public void stringToXml() throws Exception {
		String xmlString = "<root><child>someText</child></root>";
		Document xmlDoc = XmlUtils.loadXml(xmlString, null);
		Assert.assertEquals(1, xmlDoc.getChildNodes().getLength());
		Assert.assertEquals("root", xmlDoc.getChildNodes().item(0).getNodeName());
		
		// Try to convert the doc to a String again
		Assert.assertEquals(xmlString, XmlUtils.toString(xmlDoc, false, false));
	}
	
	/**
	 * Tests XML comparison.
	 */
	@Test
	public void equalsXml() throws Exception {
		Document doc1 = XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("xmlToString_simpleXml.xml"));
		Document doc2 = XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("xmlToString_fullXml.xml"));
		TestUtils.assertEqualsXml(doc1, doc2);
		
		Document doc3 = XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("xmlToString_fullXml_diff.xml"));
		try {
			TestUtils.assertEqualsXml(doc1, doc3);
			Assert.fail("XMLs are not equal");
		} catch (AssertionError e) {
			// Expected
		}
	}
	
	/**
	 * Tests the XmlOutputConverter class.
	 */
	@Test
	public void xmlOutputConverter() throws Exception {
		Document doc1 = XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("simpleSpecialChars.xml"));
		String xml1 = XmlUtils.toString(doc1, true, true);
		
		// We need to work with input streams and bytes to make sure the encoding is not messed with
		ByteArrayInputStream is = new ByteArrayInputStream(xml1.getBytes("UTF-8"));
		XmlOutputConverter xmlConv1 = new XmlOutputConverter();
		IOUtils.copy(is, xmlConv1.getOutputStream());
		
		Document doc2 = xmlConv1.getDocument();
		
		TestUtils.assertEqualsXml(doc1, doc2);
		
		// Make sure special characters were handled correctly
		XPathGetter xg = new XPathGetter(doc1);
		Assert.assertEquals("This is a little text: äöü°+\"*ç%&/()=?`è!éà£><;:_,.-", xg.getSingle("//textElement"));
	}
	
	@Test
	public void canHandleSpecialCharacters_inAttributes() throws Exception {
		Document doc1 = XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("simpleSpecialChars.xml"));
		
		XPathGetter xg = new XPathGetter(doc1);
		Assert.assertEquals("This is a little attr-text: äöü°+\"*ç%&/()=?`è!éà£><;:_,.-", xg.getSingle("//attrElement/@textFull"));
	}
	
	@Test
	public void canLookForSpecialCharacters_inXPath() throws Exception {
		Document doc1 = XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("simpleSpecialChars.xml"));
		
		XPathGetter xg = new XPathGetter(doc1);
		Assert.assertEquals("", xg.getSingle("//attrElement[@textQuot='And ;']/textQuot"));
	}
	
	//@Test
	// XPath 2.0 is not supported by standard JDK. We would have to import a library like Saxon which is
	// very big
	public void xPathGetter_canEvaluateXPath2() throws Exception {
		Document doc = XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("xPathGetter.xml"));
		XPathGetter xg = new XPathGetter(doc);
		
		// The name() function is an XPath 2.0 function
		String name = xg.getSingle("root/a1/name()");
		Assert.assertEquals("a1", name);
	}
	
	@Test
	public void xPathGetter_reckognizesEmptyElements() throws Exception {
		Document doc = XmlUtils.loadXml(JuUrl.resource().relativeTo(XmlUtilsTest.class).get("xPathGetter_emptyElement.xml"));
		XPathGetter xg = new XPathGetter(doc);
		
		Assert.assertTrue(xg.isEmptyElement("//a1"));
		Assert.assertTrue(xg.isEmptyElement("//a5"));
		Assert.assertTrue(xg.isEmptyElement("//a6")); // Empty result
		
		Assert.assertFalse(xg.isEmptyElement("//a2"));
		Assert.assertFalse(xg.isEmptyElement("//a4"));
	}
	
	@Test
	public void canConvertDate_toGregorianCalendar() {
		XMLGregorianCalendar gc = XmlUtils.asXMLGregorianCalendar(getTestDateUtc_2000_1_2_mignidht());
		Assert.assertEquals("2000-01-02T00:00:00.000Z", gc.toXMLFormat());
	}

	private Date getTestDateUtc_2000_1_2_mignidht() {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(2000, Calendar.JANUARY, 2, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTime();
	}

	@Test
	public void canConvert_GregorianCalendar_toDate() {
		Date testDate = getTestDateUtc_2000_1_2_mignidht();
		XMLGregorianCalendar gc = XmlUtils.asXMLGregorianCalendar(testDate);
		Date dConv = XmlUtils.asDate(gc);
		
		Assert.assertEquals(testDate, dConv);
	}
	
	@Test
	public void canBuildXml_withNamespaces() {
		Document doc = XmlUtils.buildXml("test", "ns1", "http://inftec.ch/ns1")
			.addNamespace("ns2", "http://inftec.ch/ns2")
			.addChild("child")
				.endChild()
			.addChild("child", "ns2")
				.addChild("child", "ns1")
					.endChild()
				.endChild()
			.getDocument();
		
		TestUtils.assertEqualsXmlResource("XmlUtilsTest_canBuildXml_withNamespaces.xml", doc);
	}

}

