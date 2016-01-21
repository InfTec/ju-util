package ch.inftec.ju.util.xml;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ch.inftec.ju.util.IOUtil;
import ch.inftec.ju.util.JuRuntimeException;

/**
 * Helper class to load data from an OutputStream into an XML document.
 * <p>
 * Submit the OutputStream instance from getOutputStream to any method writing XML
 * data and use the getDocument afterwards to load the data to a Document.
 * @author Martin
 *
 */
public class XmlOutputConverter {
	private final Logger logger = LoggerFactory.getLogger(XmlOutputConverter.class);
	
	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	
	/**
	 * Gets the OutputStream that can be passed to any function writing XML data
	 * to an output stream.
	 * @return OutputStream implementation
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}
	
	/**
	 * Gets the DOM document that was written to the OutputStream.
	 * <p>
	 * Note that a new Document gets created every time we call this method, i.e. no caching is applied.
	 * @return Document
	 * @throws JuRuntimeException If the document cannot be loaded from the stream
	 */
	public Document getDocument() throws JuRuntimeException {
		try {
			IOUtil.close(outputStream);
			byte[] bytes = outputStream.toByteArray();
			
			ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
			return XmlUtils.loadXml(inputStream, null);
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't convert byte stream to XML document", ex);
		}
	}
	
	/**
	 * Gets the XML as a String, using UTF-8 encoding.
	 * @return
	 */
	public String getXmlString() {
		try {
			return this.outputStream.toString("utf-8");
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't convert byte stream to String", ex);
		}
	}
	
	/**
	 * Writes the XML to a file.
	 * @param file Path of the file
	 */
	public void writeToXmlFile(Path file) {
		logger.debug("Writing dataset to XML file: " + file);
		
		try (final OutputStream stream = new BufferedOutputStream(
						new FileOutputStream(file.toFile()))) {

			stream.write(this.outputStream.toByteArray());
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't write DB data to file " + file, ex);
		}
	}
}
