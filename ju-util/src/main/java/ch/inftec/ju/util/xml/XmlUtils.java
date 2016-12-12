package ch.inftec.ju.util.xml;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import ch.inftec.ju.util.AssertUtil;
import ch.inftec.ju.util.IOUtil;
import ch.inftec.ju.util.JuException;
import ch.inftec.ju.util.JuRuntimeException;
import ch.inftec.ju.util.collection.Cache;
import ch.inftec.ju.util.collection.Caches;
import ch.inftec.ju.util.function.Function;

/**
 * Utility class containing XML related helper methods.
 * @author tgdmemae
 *
 */
public class XmlUtils {
	/**
     * Needed to create XMLGregorianCalendar instances
     */
    private static DatatypeFactory df = null;

    /**
     * Gets an instance of a DatatypeFactory. This is needed to create XMLGregorianCalendar instances.
     * @return DatatypeFactory instance
     */
    private static synchronized DatatypeFactory getDatetypeFactory() {
    	if (df == null) {
    		try {
                df = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException dce) {
                throw new IllegalStateException(
                    "Exception while obtaining DatatypeFactory instance", dce);
            }
    	}
    	return df;
    }

	// See org.apache.xalan.xsltc.trax.TransformerFactoryImpl (we don't want to add it to classpath)
	private static final String ATTR_INDENT_NUMBER = "indent-number";
	private static final int INDENT_NUMBER = 2;

	/**
	 * Don't instantiate.
	 */
	private XmlUtils() {
		throw new AssertionError("use only statically");
	}
	
	/**
	 * Loads and parses an XML into a DOM structure. This method doesn't perform schema
	 * validation.
	 * @param xmlUrl URL to the XML
	 * @return Document instance
	 * @throws JuException If the XML cannot be loaded
	 */
	public static Document loadXml(URL xmlUrl) throws JuException {
		return XmlUtils.loadXml(xmlUrl, null);
	}
	
	/**
	 * Loads the specified XML resource into a DOM structure and wraps it with an XPathGetter.
	 * @param xmlUrl URL to the XML
	 * @return XPathGetter instance on the document
	 * @throws JuException If the XML cannot be loaded
	 */
	public static XPathGetter loadXmlAsXPathGetter(URL xmlUrl) throws JuException {
		return new XPathGetter(XmlUtils.loadXml(xmlUrl));
	}
	
	/**
	 * Loads and parses an XML into a DOM structure.
	 * @param xmlUrl URL to the XML
	 * @param schemaUrl URL to an optional XML validation schema. If null, no validation is done.
	 * @return Document instance
	 * @throws JuException If the XML cannot be loaded
	 */
	public static Document loadXml(URL xmlUrl, URL schemaUrl) throws JuException {
		if (xmlUrl == null) {
			throw new NullPointerException("xmlUrl must not be null");
		}

		InputStream xmlStream = null;
		
		try {
			xmlStream = new BufferedInputStream(xmlUrl.openStream());
			
			return XmlUtils.loadXml(new InputSource(xmlStream), XmlUtils.loadSchema(schemaUrl), false);
		} catch (Exception ex) {
			throw new JuException(String.format("Couldn't load XML from URL: %s (Schema URL: %s)",
					xmlUrl, schemaUrl), ex);
		} finally {
			IOUtil.close(xmlStream);
		}
	}

	/**
	 * Loads and parses an XML from an InputStream into a DOM structure.
	 * <p>
	 * The input stream will be wrapped in a BufferedInputStream and closed at the end.
	 * @param inputStream InputStream of the XML
	 * @param schema Optional Schema. If null, no validation is performed
	 * @return Document instance
	 * @throws JuException If the XML cannot be loaded or fails validation
	 */
	public static Document loadXml(InputStream inputStream, Schema schema) throws JuException {
		InputStream xmlStream = null;
		
		try {
			xmlStream = new BufferedInputStream(inputStream);			
			return XmlUtils.loadXml(new InputSource(xmlStream), schema, false);
		} catch (Exception ex) {
			throw new JuException("Couldn't load XML from InputStream", ex);
		} finally {
			IOUtil.close(xmlStream);
		}
	}
	
	/**
	 * Loads and parses an XML into a DOM structure.
	 * 
	 * @param xmlSource
	 *            InputStream of the XML
	 * @param schema
	 *            Optional Schema. If null, no validation is performed
	 * @return Document instance
	 * @throws JuRuntimeException
	 *             If the XML cannot be loaded or fails validation
	 */
	public static Document loadXml(InputSource xmlSource, Schema schema, boolean nameSpaceAware) {
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilderFactory.setNamespaceAware(nameSpaceAware);
			
			// There's a bug in JDK >= 6, ignoring whitespace is not working
			// docBuilderFactory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

			// Parse XML
			Document doc = docBuilder.parse(xmlSource);

			// Remove whitespace manually
			XmlUtils.removeWhitespaceNodes(doc.getDocumentElement());
			
			// Validate if we have a Schema
			if (schema != null) {
				schema.newValidator().validate(new DOMSource(doc));
			}

			return doc;
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't load XML", ex);
		}
	}
	
	/**
	 * Removes all Whitespace Noted from the specified element.
	 * @param e Element
	 */
	private static void removeWhitespaceNodes(Element e) {
		NodeList children = e.getChildNodes();
		for (int i = children.getLength() - 1; i >= 0; i--) {
			Node child = children.item(i);
			if (child instanceof Text
					&& ((Text) child).getData().trim().length() == 0) {
				e.removeChild(child);
			} else if (child instanceof Element) {
				removeWhitespaceNodes((Element) child);
			}
		}
	}
	
	/**
	 * Loads an XML from a String.
	 * @param xmlString XML String
	 * @param schema Optional Schema. If null, no validation is performed
	 * @return Document instance 
	 */
	public static Document loadXml(String xmlString, Schema schema) {
		return XmlUtils.loadXml(new InputSource(new StringReader(xmlString)), schema, false);
	}
	
	/**
	 * Loads an XML from a String.
	 * @param xmlString XML String
	 * @param schema Optional Schema. If null, no validation is performed
	 * @param nameSpaceAware whether the Document should be namespace aware
	 * @return Document instance 
	 * @throws JuException If the String cannot be converted to a DOM Document
	 */
	// Introduced this method as we had problems in ESW to validate a document unless it was nameSpaceAware...
	public static Document loadXml(String xmlString, Schema schema, boolean nameSpaceAware) throws JuException {
		return XmlUtils.loadXml(new InputSource(new StringReader(xmlString)), schema, nameSpaceAware);
	}
	
	/**
	 * Loads an XML Schema from the specified url.
	 * @param url URL to load schema from
	 * @return Schema instance or null if the url is null
	 */
	public static Schema loadSchema(URL url) {
		if (url == null) return null;
		
		try {
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			return schemaFactory.newSchema(url);
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't load XML schema: " + url, ex);
		}
	}
	
	/**
	 * Indents the specified XML.
	 * @param xmlString XML string to be indented
	 * @param includeXmlDeclaration If true, the &lt;?xml ... ?&gt; declaration is included.
	 * @return Indented XML
	 */
	public static String indentXml(String xmlString, boolean includeXmlDeclaration) {
		Document doc = XmlUtils.loadXml(xmlString, null);
		return XmlUtils.toString(doc, includeXmlDeclaration, true);
	}
	
	/**
	 * Converts an XML to a String.
	 * <p>
	 * This will create an XML string with encoding="UTF-8" and a standalone declaration,
	 * as specified in the Document.
	 * <p>
	 * Indentation will be two blanks - if true.
	 * @param document XML Document
	 * @param includeXmlDeclaration If true, the &lt;?xml ... ?&gt; declaration is included.
	 * @param indent If true, result will be indented (pretty-printed), using two blanks
	 * for child indentation
	 * @return String representation of the XML
	 * @throws JuRuntimeException If the conversion fails
	 */
	public static String toString(Document document, boolean includeXmlDeclaration, boolean indent) throws JuRuntimeException {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();

			// With JBoss implementation of Xalan, the OutputKeys.INDENT on Transformer wouldn't work. We have to set the INDENT_NUMBER
			// attribute on the factory instead...
			try {
				tf.setAttribute(ATTR_INDENT_NUMBER, INDENT_NUMBER);
			} catch (IllegalArgumentException ex) {
				// Ignore, probably not JBoss bundled factory...
			}

			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, (includeXmlDeclaration ? "no" : "yes"));
			
			if (indent) {
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(INDENT_NUMBER));
			}
			
			try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
				transformer.transform(new DOMSource(document), new StreamResult(os));
				return new String(os.toByteArray(), "UTF-8");
			}			
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't convert XML to String", ex);
		}
	}
	
	/**
	 * Creates a new XmlBuilder to construct an XML document based on DOM (document
	 * object model).
	 * @param rootElementName Name of the root element
	 * @return XmlBuilder of the root element to construct the XML
	 */
	public static XmlBuilder buildXml(String rootElementName) {
		return XmlBuilder.createRootBuilder(rootElementName, null, null);
	}
	
	/**
	 * Creates a new XmlBuilder to construct an XML document based on DOM (document
	 * object model) using the specified namespace
	 * @param rootElementName Name of the root element
	 * @return XmlBuilder of the root element to construct the XML
	 */
	public static XmlBuilder buildXml(String rootElementName, String namespacePrefix, String namespaceUri) {
		return XmlBuilder.createRootBuilder(rootElementName, namespacePrefix, namespaceUri);
	}

    
//    private static class SchemaLoader {
//    	@SuppressWarnings("unchecked") // TODO: Use new apache commons collection API
//    	private final Map<URL, Schema> schemaCache = new LRUMap(XmlUtils.SCHEMA_CACHE_MAX_SIZE);
//    	
//    	synchronized Schema getSchema(URL schemaUrl) throws ServiceDbRuntimeException {    		
//    		// Try to get the schema
//    		Schema schema = this.schemaCache.get(schemaUrl);
//    		
//    		try {
//	    		if (schema == null) {
//	    			_log.info(String.format("Loading XML Schema: %s (current cache size: %d)", schemaUrl, schemaCache.size()));
//	                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//	                schema = schemaFactory.newSchema(schemaUrl);
//	                this.schemaCache.put(schemaUrl, schema);
//	    		}
//	    		
//	    		return schema;
//    		} catch (Exception ex) {
//    			throw new ServiceDbRuntimeException("Couldn't load XML Schema: " + schemaUrl, ex);
//    		}
//    	}
//    }
    
    /**
     * Returns a MarshallerBuilder that can be used to perform XML marshalling and unmarshalling.
     */
    public static MarshallerBuilder marshaller() {
    	return new MarshallerBuilder();
    }
    
    public static class MarshallerBuilder {
    	private final Logger logger = LoggerFactory.getLogger(MarshallerBuilder.class);
    	
    	private boolean cacheJaxbContext = true;
    	private static Cache<String, JAXBContext> cache;
    	private static int MAX_CACHE_SIZE = 100;
    	
    	private boolean formattedOutput = false;
    	private Schema schema;
    	
    	private JuNamespacePrefixMapper prefixMapper;
    	private NamespacePrefixMapperAdapter prefixMapperAdapter;
    	
    	/**
    	 * Whether to produce formatted output.
    	 * <p>
    	 * Default is false.
    	 * @param formattedOutput If true, XML output will be formatted / indented.
    	 */
    	public MarshallerBuilder formattedOutput(boolean formattedOutput) {
    		this.formattedOutput = formattedOutput;
    		return this;
    	}
    	
    	/**
    	 * Specifies an XML Schema to be used to validate the XML when marshalling or
    	 * unmarshalling.
    	 * @param schemaUrl URL to XML Schema (XSD)
    	 */
    	public MarshallerBuilder schema(URL schemaUrl) {
    		this.schema = XmlUtils.loadSchema(schemaUrl);
    		return this;
    	}
    	
    	/**
    	 * Sets the prefix for the specified namespace URI.
    	 * <p>
    	 * <strong>NOTE:</strong> This relies on an SUN internal implementation class of JAXB so it might not
    	 * work on all JDKs. If platform independent behavior is required, you should specify the namespaces
    	 * using the package annotation <i>package-info.java</i>, e.g.
    	 * <pre>
    	 * <code>
    	 * {@literal @}javax.xml.bind.annotation.XmlSchema(
		 * namespace = "urn:inftec.ch/ju/util/xml/ns/main"
		 * , elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED
		 * , xmlns = {
		 * {@literal @}XmlNs(namespaceURI = "urn:inftec.ch/ju/util/xml/ns/main", prefix="def")}
		 * )
		 * package ch.inftec.ju.util.xml.ns.main;
	     * import javax.xml.bind.annotation.XmlNs;
		 * </code>
		 * </pre>
    	 * @param prefix Namespace prefix, e.g. 'ns'. Use null for default namespace
    	 * @param namespaceUri Namespace URI, e.g. 'urn:inftec.ch/ns'
    	 */
    	public MarshallerBuilder setNamespacePrefix(String prefix, String namespaceUri) {
    		if (this.prefixMapper == null) {
    			this.prefixMapper = new JuNamespacePrefixMapper();
    		}
    		
    		this.prefixMapper.setPrefix(prefix, namespaceUri);
    		
    		return this;
    	}
    	
    	/**
    	 * Sets a NamespacePrefixMapper adapter. This can/must be used if the JAXB implementation in use is not compatible
    	 * with the JAXB implementation used by JU.
    	 */
    	public MarshallerBuilder setNamespacePrefixMapper(NamespacePrefixMapperAdapter prefixMapperAdapter) {
    		this.prefixMapperAdapter = prefixMapperAdapter;
    		return this;
    	}
    	
    	/**
    	 * Marshals the specified Object to a String, i.e. converts it to
    	 * an XML.
    	 * @param o Object to be marshalled
    	 * @return XML String
    	 */
    	public String marshalToString(Object o) {
        	try (StringWriter writer = new StringWriter()) {
                Object obj = o;
                if (o instanceof JAXBElement) {
                	JAXBElement<?> e = (JAXBElement<?>)o;
                	obj = e.getValue();
                }
                
                logger.debug("Marshalling " + obj.getClass());
                
                JAXBContext context = this.loadContext(obj.getClass().getPackage().getName());
                javax.xml.bind.Marshaller marshaller = context.createMarshaller();
                
                // Use the Schema to make sure the marshalled String is valid according the the Schema (if any)
                marshaller.setSchema(this.schema);
                
                // Configure Marshaller
                
                if (this.formattedOutput) {
                    marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE); 
                }
                
                if (this.prefixMapper != null) {
                	// Use the default namespace prefix mapper adapter if none has been specified explicitly
                	AssertUtil.assertNotNull("Prefix Mapper must be specified when using prefix mapping", this.prefixMapperAdapter);
//                	if (this.prefixMapperAdapter == null) {
//                		AssertUtil.
//                		this.prefixMapperAdapter = new DefaultNamespacePrefixMapperAdapter();
//                	}
                	
                	marshaller.setProperty(
                			this.prefixMapperAdapter.getPropertyName()
                			, this.prefixMapperAdapter.getNamespacePrefixMapperImplementation(this.prefixMapper));
                }
                
                
                marshaller.marshal(o, writer);
        
                String xmlString = writer.toString();

                logger.debug("Marshalling done");
                return xmlString;
            } catch (Exception ex) {
            	throw new JuRuntimeException("Couldn't marshal object %s (using Schema: %s)", ex, o, null);//this.schema), ex);
            }
    	}
    	
        /**
         * Unmarshals the specified XML string, i.e. converts it into an object instance.
         * <p>
         * Use this method if you're not sure which objects you will get.
         * @param xmlString XML string to be unmarshalled
         * @param objClass Object of the same package as the expected object, used to
         * initialize the JAXBContext.
         * @return Unmarshalled object. If by unmarshalling, we get a JAXBElement, the actual object
         * will be unwrapped and returned
         */
        public Object unmarshalRaw(String xmlString, Class<?> objClass) {
            try (StringReader reader = new StringReader(xmlString)) {
                JAXBContext context = this.loadContext(objClass.getPackage().getName());
                Unmarshaller unmarshaller = context.createUnmarshaller();
                
                // Set Schema to validate (if any)
                unmarshaller.setSchema(this.schema);
                
                Object obj = unmarshaller.unmarshal(reader);                
                if (obj instanceof JAXBElement) {
                	// Unwrap object
                	obj = ((JAXBElement<?>)obj).getValue(); 
                }
                
                return obj; 
            } catch (Exception ex) {
            	logger.debug("Marshalling of String failed: " + xmlString);
                throw new JuRuntimeException("Couldn't unmarshal XML String (XML is dumped as debug log)", ex);
            }
        }
        
        private JAXBContext loadContext(String contextPath) throws JAXBException {
        	if (this.cacheJaxbContext) {
        		// We'll cache the JAXBContext to avoid loading it every time we need it.
				// JAXBContext is ThreadSave, but we'll need to create a separate Marshaller / Unmarshaller
				// every time we do marshalling / unmarshalling and must not use the
        		// convenience method context.marshal / unmarshal.
        		synchronized(this) {
	        		if (cache == null) {
						cache = Caches.simpleBoundedCache(MAX_CACHE_SIZE, new Function<String, JAXBContext>() {
								@Override
								public JAXBContext apply(String key) {
									return createContext(key);
								}
		        			});
	        		}
	        	}

				return cache.get(contextPath);
        	} else {
        		return createContext(contextPath);
        	}
        }
        
        private JAXBContext createContext(String contextPath) {
        	logger.debug("Creating JAXBContext for path {}", contextPath);

			try {
				return JAXBContext.newInstance(contextPath);
			} catch (JAXBException ex) {
				throw new JuRuntimeException("Couldn't create JAXB context", ex);
			}
        }
        
        /**
         * Unmarshals the specified XML string, i.e. converts it into an object instance.
         * @param xmlString XML string to be unmarshalled
         * @param objClass Expected Object class used to build JAXBContext and evaluate validation Schema
         * @return Unmarshalled object
         * @param <T> Type of expected object
         */
        public <T> T unmarshal(String xmlString, Class<T> objClass) {
            @SuppressWarnings("unchecked")
            T object = (T)this.unmarshalRaw(xmlString, objClass);
            
            return object; 
        }
        
        /**
         * Unmarshals the specified XML, i.e. converts it into an object instance.
         * @param xmlUrl URL to XML to be unmarshalled
         * @param objClass Expected Object class used to build JAXBContext and evaluate validation Schema
         * @return Unmarshalled object
         * @param <T> Type of expected object
         */
        public <T> T unmarshal(URL xmlUrl, Class<T> objClass) {
        	String xmlString = new IOUtil().loadTextFromUrl(xmlUrl);
        	
            @SuppressWarnings("unchecked")
            T object = (T)this.unmarshalRaw(xmlString, objClass);
            
            return object; 
        }
        
        /**
         * Helper interface to provide a JAXB implementation specific instance of the NamespacePrefixMapper.
         * @author martin.meyer@inftec.ch
         *
         */
        public interface NamespacePrefixMapperAdapter {
        	/**
        	 * Gets the property name for the NamespacePrefixMapper property of the JAXB marshaller.
        	 */
        	String getPropertyName();
        	
        	/**
        	 * Gets an implementation of the NamespacePrefixMapper.
        	 * <p>
        	 * A JU specific PrefixMapper will be provided as a parameter, so the call to getPreferredPrefix
        	 * can just be forwarded to this mapper.
        	 * @param prefixMapper JU specific Prefix mapper
        	 * @return JAXB implementation specific mapper
        	 */
        	Object getNamespacePrefixMapperImplementation(PrefixMapper prefixMapper);

        	/**
        	 * JU specific prefix mapper.
        	 * @author martin.meyer@inftec.ch
        	 *
        	 */
        	interface PrefixMapper {
        		/**
        		 * Evaluates the preferred prefix
        		 * @param namespaceUri XSD namespace URI
        		 * @param suggestion Suggested prefix
        		 * @param requirePrefix If a prefix is required
        		 * @return Preferred prefix
        		 */
        		String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix);
        	}
        }
    }
    
    /**
     * Validates the specified XML String using the XSD Schema.
     * @param xml XML to validate
     * @param schema Schema instance
     * @throws JuException If the validation fails
     */
    public static void validate(String xml, Schema schema) throws JuException {
    	try { 
    		schema.newValidator().validate(new DOMSource(XmlUtils.loadXml(xml, null)));
    	} catch (SAXParseException ex) {
    		throw new JuException("Parse exception: " + ex.getMessage(), ex);
    	} catch (Exception ex) {
    		throw new JuException("Validation failed", ex);
    	}
    }
    
    /**
     * Validates the specified XML String using the XSD Schema.
     * @param xml XML to validate
     * @param schema Schema instance
     * @param nameSpaceAware Whether validation should be namespace aware
     * @throws JuException If the validation fails
     */
    public static void validate(String xml, Schema schema, boolean nameSpaceAware) throws JuException {
    	try { 
    		schema.newValidator().validate(new DOMSource(XmlUtils.loadXml(xml, null, nameSpaceAware)));
    	} catch (SAXParseException ex) {
    		throw new JuException("Parse exception: " + ex.getMessage(), ex);
    	} catch (Exception ex) {
    		throw new JuException("Validation failed", ex);
    	}
    }
    
    /**
     * Converts a java.util.Date into an instance of XMLGregorianCalendar.
     * <p>
     * The timezone of the calendar will be set to GMT to enforce Zulu-Time-Format
     * in the XML messages.
     * @param date Instance of java.util.Date or a null reference
     * @return XMLGregorianCalendar instance whose value is based upon the
     *  value in the date parameter. If the date parameter is null then
     *  this method will simply return null.
     */
    public static XMLGregorianCalendar asXMLGregorianCalendar(java.util.Date date) {
        if (date == null) {
            return null;
        } else {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(date);
            gc.setTimeZone(TimeZone.getTimeZone("GMT")); // Set time zone to GMT to enforce Zulu-Time in XML
            return getDatetypeFactory().newXMLGregorianCalendar(gc);
        }
    }

    /**
     * Converts an XMLGregorianCalendar to an instance of java.util.Date
     *
     * @param xgc Instance of XMLGregorianCalendar or a null reference
     * @return java.util.Date instance whose value is based upon the
     *  value in the xgc parameter. If the xgc parameter is null then
     *  this method will simply return null.
     */
    public static java.util.Date asDate(XMLGregorianCalendar xgc) {
        if (xgc == null) {
            return null;
        } else {
            return xgc.toGregorianCalendar().getTime();
        }
    }
}
