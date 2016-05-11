package ch.inftec.ju.json;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.inftec.ju.util.JuRuntimeException;

/**
 * Utility class containing JSON related helper methods.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 */
public class JsonUtils {
	private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
	
	/**
	 * Formats (pretty prints) the specified JSON String.
	 * @param json JSON String
	 * @return JSON String with indentations and line breaks
	 */
	public static String formatJson(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Object obj = new ObjectMapper().readValue(json, Object.class);
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("JSON String: " + json);
			}
			throw new JuRuntimeException("Couldn't format JSON (see debug log for JSON String)", ex);
		}
	}
	
	/**
	 * Returns a new MarshallerBuilder to convert Java Objects to JSON and vice versa.
	 * @return MarshallerBuilder
	 */
	public static MarshallerBuilder marshaller() {
		return new MarshallerBuilder();
	}
	
	public static final class MarshallerBuilder {
		private boolean formattedOutput = false;
		
		/**
    	 * Whether to produce formatted output.
    	 * <p>
    	 * Default is false.
    	 * @param formattedOutput If true, JSON output will be formatted / indented.
    	 * @return
    	 */
		public MarshallerBuilder formattedOutput(boolean formattedOutput) {
			this.formattedOutput = formattedOutput;
			return this;
		}
		
		/**
		 * Marshalls the specified object to a JSON String.
		 * <p>
		 * This will order the properties of the object alphabetically to produce
		 * repeatable output.
		 * @param obj Object to be marshalled
		 * @return JSON representation of the object
		 */
		public String marshalToString(Object obj) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY, true);
				ObjectWriter writer = this.formattedOutput
					? mapper.writerWithDefaultPrettyPrinter()
					: mapper.writer();
					
				return writer.writeValueAsString(obj);
			} catch (Exception ex) {
				throw new JuRuntimeException("Marshalling of object to JSON failed", ex);
			}
		}
	}
}
