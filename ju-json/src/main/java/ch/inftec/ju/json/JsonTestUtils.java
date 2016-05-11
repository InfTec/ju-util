package ch.inftec.ju.json;

import org.junit.Assert;

import ch.inftec.ju.util.IOUtil;
import ch.inftec.ju.util.JuUrl;
import ch.inftec.ju.util.ReflectUtils;


public class JsonTestUtils {
	/**
	 * Asserts that the specified Object equals the string defined in the JSON resource. Expects the resource
	 * to be in UTF-8 encoding.
	 * <p>
	 * This will marshall the object to a JSON string, format both and then compare them as Strings.
	 * @param resourceName Name of the resource that has to be in the same package as the calling class
	 * @param Object obj to be conpared to the JSON resource
	 */
	public static void assertEqualsJsonResource(String resourceName, Object obj) {
		String resJson = new IOUtil("UTF-8").loadTextFromUrl(
				JuUrl.resource().exceptionIfNone().relativeTo(ReflectUtils.getCallingClass()).get(resourceName));
		
		String objJson = JsonUtils.marshaller().formattedOutput(true).marshalToString(obj);
		Assert.assertEquals(JsonUtils.formatJson(resJson), objJson);
	}
}
