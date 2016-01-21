package ch.inftec.ju.util.xml.jaxb;

import java.math.BigInteger;

import javax.xml.bind.MarshalException;
import javax.xml.bind.UnmarshalException;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.inftec.ju.util.IOUtil;
import ch.inftec.ju.util.JuRuntimeException;
import ch.inftec.ju.util.JuUrl;
import ch.inftec.ju.util.TestUtils;
import ch.inftec.ju.util.xml.XmlUtils;
import ch.inftec.ju.util.xml.jaxb.player.ObjectFactory;
import ch.inftec.ju.util.xml.jaxb.player.Player;
import ch.inftec.ju.util.xml.jaxb.player.SalutationType;
import ch.inftec.ju.util.xml.ns.main.ComplexRoot;
import ch.inftec.ju.util.xml.ns.ref.RefNameType;

/**
 * Contains XmlUtils tests related to JAXB.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 */
public class XmlUtilsJaxbTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void canCreateXml_fromJaxbObject() {
		String xmlString = XmlUtils.marshaller().marshalToString(this.getMichael());
		TestUtils.assertEqualsResource("XmlUtilsTest_canCreateXml_fromJaxbObject.xml", xmlString);
	}
	
	@Test
	public void canCreateXml_fromJaxbObject_usingFormattedOutput() {
		String xmlString = XmlUtils.marshaller().formattedOutput(true).marshalToString(this.getMichael());
		TestUtils.assertEqualsResource("XmlUtilsTest_canCreateXml_fromJaxbObject_usingFormattedOutput.xml", xmlString);
	}
	
	@Test
	public void canCreate_object_fromXml() throws Exception {
		String xmlString = new IOUtil().loadTextFromUrl(JuUrl.resourceRelativeTo("XmlUtilsTest_canCreateXml_fromJaxbObject.xml", XmlUtilsJaxbTest.class));
		Object o = XmlUtils.marshaller().unmarshalRaw(xmlString, Player.class);
		this.assertMichael((Player) o);
	}
	
	@Test
	public void canCreate_typedOject_fromXml() throws Exception {
		String xmlString = new IOUtil().loadTextFromUrl(JuUrl.resourceRelativeTo("XmlUtilsTest_canCreateXml_fromJaxbObject.xml", XmlUtilsJaxbTest.class));
		Player p = XmlUtils.marshaller().unmarshal(xmlString, Player.class);
		this.assertMichael(p);
	}
	
	@Test
	public void canNot_createObject_fromXml_withoutNamespace() throws Exception {
		exception.expect(JuRuntimeException.class);
		exception.expectCause(Is.isA(UnmarshalException.class));
		
		String xmlString = new IOUtil().loadTextFromUrl(JuUrl.resourceRelativeTo("XmlUtilsTest_michael_noNamespace.xml", XmlUtilsJaxbTest.class));
		Object o = XmlUtils.marshaller().unmarshalRaw(xmlString, Player.class);
		this.assertMichael((Player) o);
	}
	
	@Test
	public void canMarshal_invalidObject_whenNotUsingSchema() {
		XmlUtils.marshaller().marshalToString(this.getMichael(100));
	}
	
	@Test
	public void canNotMarshal_invalidObject_whenUsingSchema() {
		exception.expect(JuRuntimeException.class);
		exception.expectCause(Is.isA(MarshalException.class));
		
		XmlUtils.marshaller()
			.schema(JuUrl.resourceRelativeTo("player.xsd", this.getClass()))
			.marshalToString(this.getMichael(100));
	}
	
	@Test
	public void canUnmarshal_invalidXml_whenNotUsingSchema() throws Exception {
		String xmlString = new IOUtil().loadTextFromUrl(JuUrl.resourceRelativeTo("XmlUtilsTest_michael_invalid.xml", this.getClass()));
		XmlUtils.marshaller().unmarshal(xmlString, Player.class);
	}
	
	@Test
	public void canNotUnmarshal_invalidXml_whenUsingSchema() throws Exception {
		exception.expect(JuRuntimeException.class);
		exception.expectCause(Is.isA(UnmarshalException.class));
		
		String xmlString = new IOUtil().loadTextFromUrl(JuUrl.resourceRelativeTo("XmlUtilsTest_michael_invalid.xml", this.getClass()));
		
		XmlUtils.marshaller()
			.schema(JuUrl.resourceRelativeTo("player.xsd", this.getClass()))
			.unmarshal(xmlString, Player.class);
	}
	
	private void assertMichael(Player p) {
		this.assertMichael(p, 33);
	}
	
	private void assertMichael(Player p, int age) {
		Assert.assertEquals("Michael", p.getFirstName());
		Assert.assertEquals("Jordan", p.getLastName());
		Assert.assertEquals(SalutationType.MR, p.getSalutation());
		Assert.assertEquals(age, p.getAge());
	}
	
	private Player getMichael(){
		return this.getMichael(33);
	}
	
	private Player getMichael(int age) {
		ObjectFactory playerFactory = new ObjectFactory();
		
		Player p = playerFactory.createPlayer();
		p.setFirstName("Michael");
		p.setLastName("Jordan");
		p.setSalutation(SalutationType.MR);
		p.setAge(age);
		
		return p;
	}
	
	@Test
	public void canMarshall_object_withMultipleNamespaces() {
		String xml = XmlUtils.marshaller()
			.marshalToString(this.getComplexRoot());
		
		TestUtils.assertEqualsXmlResource("XmlUtilsJaxbTest_canMarshall_object_withMultipleNamespaces.xml", xml);
	}
	
	@Ignore("Cannot set prefix mapper on all platforms...")
	@Test
	public void canMarshall_object_withMultipleNamespaces_andCustomPrefixes() {
		String xml = XmlUtils.marshaller()
				.setNamespacePrefix("m", "urn:inftec.ch/ju/util/xml/ns/main")
				.setNamespacePrefix("r", "urn:inftec.ch/ju/util/xml/ns/ref")
				.marshalToString(this.getComplexRoot());
			
			TestUtils.assertEqualsXmlResource("XmlUtilsJaxbTest_canMarshall_object_withMultipleNamespaces_andCustomPrefixes.xml", xml);
	}
	
	@Ignore("Cannot set prefix mapper on all platforms...")
	@Test
	public void canMarshall_object_withMultipleNamespaces_andCustomDefaultPrefix() {
		String xml = XmlUtils.marshaller()
				.setNamespacePrefix(null, "urn:inftec.ch/ju/util/xml/ns/ref")
				.marshalToString(this.getComplexRoot());
			
			TestUtils.assertEqualsXmlResource("XmlUtilsJaxbTest_canMarshall_object_withMultipleNamespaces_andCustomDefaultPrefix.xml", xml);
	}
	
	private ComplexRoot getComplexRoot() {
		ComplexRoot cr = new ComplexRoot();
		cr.setName("name");
		cr.setAge(BigInteger.valueOf(17L));
		cr.setRefName(new RefNameType());
		cr.getRefName().setFirstName("firstName");
		cr.getRefName().setLastName("lastName");
		return cr;
	}
}

