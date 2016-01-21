package ch.inftec.ju.util.xml;

import java.util.List;

import org.junit.Test;

import ch.inftec.ju.util.IOUtil;
import ch.inftec.ju.util.JuUrl;
import ch.inftec.ju.util.TestUtils;

public class XPathGetterTest {
	@Test
	public void canGetNodeNames() throws Exception {
		XPathGetter xg = XmlUtils.loadXmlAsXPathGetter(JuUrl.resource().relativeTo(XPathGetterTest.class).get("simple.xml"));
		
		List<String> nodeNames = xg.getNodeNames("root/element/*");
		TestUtils.assertCollectionEquals(nodeNames, "childElement", "childElement", "textElement");
	}
}
