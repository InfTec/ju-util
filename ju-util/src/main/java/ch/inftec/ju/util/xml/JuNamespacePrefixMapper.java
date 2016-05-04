package ch.inftec.ju.util.xml;

import java.util.HashMap;
import java.util.Map;

import ch.inftec.ju.util.AssertUtil;
import ch.inftec.ju.util.xml.XmlUtils.MarshallerBuilder.NamespacePrefixMapperAdapter.PrefixMapper;

class JuNamespacePrefixMapper implements PrefixMapper {
	private final Map<String, String> prefixes = new HashMap<>();
	
	void setPrefix(String prefix, String namespaceUri) {
		String actualPrefix = prefix != null ? prefix : "";
		
		AssertUtil.assertFalse("URI already registered: " + namespaceUri, this.prefixes.containsKey(namespaceUri));
		AssertUtil.assertFalse("Prefix already registered: " + prefix, this.prefixes.containsValue(actualPrefix));
		
		this.prefixes.put(namespaceUri, actualPrefix);
	}
	
	@Override
	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
		if (this.prefixes.containsKey(namespaceUri)) {
			return this.prefixes.get(namespaceUri);
		} else {
			// Make sure we don't return a suggestion that was registered for another namespace
			String namespace = suggestion;
			while (this.prefixes.containsValue(namespace) || (namespace == null && this.prefixes.containsValue(""))) {
				if (namespace == null) namespace = "ns";
				else namespace = namespace + "2";
			}
			
			return namespace;
		}
	}
	
	// Problems with JDKs on Linux as we don't seem to have the class com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper
//	static class DefaultNamespacePrefixMapperAdapter implements NamespacePrefixMapperAdapter {
//		@Override
//		public String getPropertyName() {
//			return "com.sun.xml.internal.bind.namespacePrefixMapper";
//		}
//
//		@Override
//		public Object getNamespacePrefixMapperImplementation(final PrefixMapper prefixMapper) {
//			return new NamespacePrefixMapper() {
//				@Override
//				public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
//					return prefixMapper.getPreferredPrefix(namespaceUri, suggestion, requirePrefix);
//				}
//			};
//		}
//	}
}
