package ch.inftec.ju.json;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.inftec.ju.util.JuCollectionUtils;
import ch.inftec.ju.util.TestUtils;

public class JsonUtilsTest {
	@Test
	public void canMarshall_simpleObject_toJson() {
		String json = JsonUtils.marshaller().marshalToString(new SimpleObject("val1"));
		TestUtils.assertEqualsResource("JsonUtilsTest_canMarshall_simpleObject_toJson.json",  json);
	}
	
	@Test
	public void canMarshall_complexObject_toJson() {
		String json = JsonUtils.marshaller().marshalToString(
				new ComplexObject("val1", "val2", "vala", "valb", "valc"));
		TestUtils.assertEqualsResource("JsonUtilsTest_canMarshall_complexObject_toJson.json",  json);
	}
	
	@Test
	public void canFormat_complexObject() {
		String json = JsonUtils.marshaller()
			.formattedOutput(true)
			.marshalToString(
				new ComplexObject("val1", "val2", "vala", "valb", "valc"));
		
		TestUtils.assertEqualsResource("JsonUtilsTest_canFormat_complexObject.json",  json);
	}
	
	@Test
	public void canFormat_jsonString() {
		String json = JsonUtils.marshaller()
				.marshalToString(
					new ComplexObject("val1", "val2", "vala", "valb", "valc"));
		
		String formattedJson = JsonUtils.formatJson(json);
		TestUtils.assertEqualsResource("JsonUtilsTest_canFormat_complexObject.json", formattedJson);
	}
	
	@Test
	public void marshalsProperties_alphabetically() {
		String json = JsonUtils.marshaller()
				.marshalToString(new UnsortedObject());
		TestUtils.assertEqualsResource("JsonUtilsTest_marshalsProperties_alphabetically.json", json);
	}
	
	@Test
	public void canEscape_specialCharacters() {
		String json = JsonUtils.marshaller()
				.marshalToString(new SimpleObject("\":{\\}"));
		TestUtils.assertEqualsResource("JsonUtilsTest_canEscape_specialCharacters.json", json);
	}
	
	static final class SimpleObject {
		private String val;
		
		public SimpleObject(String val) {
			this.val = val;
		}
		
		public String getVal() {
			return this.val;
		}
	}
	
	public static final class ComplexObject {
		private String stringVal;
		private SimpleObject simpleObject;
		private List<String> stringList = new ArrayList<>();
		
		public ComplexObject(String stringVal, String simpleObjectVal, String... vals) {
			this.stringVal = stringVal;
			this.simpleObject = new SimpleObject(simpleObjectVal);
			this.stringList.addAll(JuCollectionUtils.arrayList(vals));
		}
		
		public String getStringVal() {
			return this.stringVal;
		}
		
		public SimpleObject getSimpleObject() {
			return this.simpleObject;
		}
		
		public List<String> getStringList() {
			return this.stringList;
		}
	}
	
	public static final class UnsortedObject {
		public String getC() {
			return "c";
		}
		
		public String getA() {
			return "a";
		}
		
		public String getB() {
			return "b";
		}
	}
}
