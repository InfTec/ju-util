package ch.inftec.ju.json;

import org.junit.Test;

public class JsonTestUtilsTest {
	@Test
	public void canCompare_object_toFormattedJson() {
		JsonTestUtils.assertEqualsJsonResource("JsonTestUtilsTest_canCompare_object_toFormattedJson.json", new SimpleObject());
	}
	
	@Test
	public void canCompare_object_toFormattedJson_usingAbsolutePath() {
		JsonTestUtils.assertEqualsJsonResource("/ch/inftec/ju/json/JsonTestUtilsTest_canCompare_object_toFormattedJson.json", new SimpleObject());
	}
	
	@Test
	public void canCompare_object_toUnformattedJson() {
		JsonTestUtils.assertEqualsJsonResource("JsonTestUtilsTest_canCompare_object_toUnformattedJson.json", new SimpleObject());
	}
	
	static final class SimpleObject {
		public String getVal1() {
			return "myVal";
		}
		
		public String getVal2() {
			return "myVal2";
		}
	}
}
