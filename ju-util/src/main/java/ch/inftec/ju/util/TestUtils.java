package ch.inftec.ju.util;

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ch.inftec.ju.util.comparison.EqualityTester;
import ch.inftec.ju.util.xml.XmlUtils;



/**
 * Class containing test related utility methods.
 * @author tgdmemae
 *
 */
public final class TestUtils {
	private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);
	
	/**
	 * Don't instantiate.
	 */
	private TestUtils() {
		throw new AssertionError("use only statically");
	}
	
	/**
	 * Runs internal tests as defined in a static (recommendedly protected) class method 'internalTests'.
	 * If the class doesn't have a static method internalTests, an exception is thrown.
	 * @param c Class
	 * @param params Array of 0-n parameters to pass to the internalTests method. If a parameter is
	 * null, it is supposed to be of type Object.
	 */
	public static void runInternalTests(Class<?> c, Object... params) {
		Method m = null;
		
		try {
			Class<?>[] paramTypes = ReflectUtils.getTypes(params);
			m = ReflectUtils.getDeclaredMethod(c, "internalTests", paramTypes);
			m.setAccessible(true);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Couldn't get static method 'internalTests' for class " + c.getName(), ex);
		}
		
		try {
			m.invoke(null, params);
		} catch (Exception ex) {
			throw new RuntimeException("internalTests raised exception for class " + c.getName(), ex);
		}
	}
	
	/**
	 * Runs all tests in the specified class using the default JUnit class runner.
	 * <p>
	 * If one or more tests have failed, a JuRuntimeException is thrown. Additionally, we'll log
	 * the exception stack traces and add the first exception as a cause to the runtime exception.
	 * @param clazz Class containing tests
	 */
	public static Result runJUnitTests(Class<?> clazz) {
		Computer computer = new Computer();
		Result res = new JUnitCore().run(computer, clazz);
		
		if (res.getFailureCount() > 0) {
			XString xs = new XString("Unit tests failed. Failure count: " + res.getFailureCount());
			for (Failure f : res.getFailures()) {
				 xs.addLineFormatted("%s: %s", f.getException().getClass(), f.getException().getMessage());
				 logger.error("Unit test failed: " + f.getMessage(), f.getException());
			}
			
			throw new JuRuntimeException(xs.toString(), res.getFailures().get(0).getException());
		}

		return res;
	}
	
	/**
	 * Asserts that the specified String equals the string defined in the text resource. Expects the resource
	 * to be in UTF-8 encoding.
	 * <p>
	 * This method will convert both the resource and the string to using line endings \n, so
	 * different line endings will be ignored.
	 * @param s String to be compared
	 * @param resourceName Name of the resource that has to be in the same package as the calling class
	 * @param replacements key, value replacement pairs. Key in the file has to be surrounded by percentage signs, e.g. %key%
	 */
	public static void assertEqualsResource(String resourceName, String s, String... replacements) {
		try {
			String resString = new IOUtil("UTF-8").loadTextFromUrl(
					JuUrl.resource().exceptionIfNone().relativeTo(ReflectUtils.getCallingClass()).get(resourceName)
					, replacements);
			String sUnix = IOUtil.toNewLineUnix(s);
			
			Assert.assertEquals(resString, sUnix);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Resource not found: " + resourceName, ex);
		}
	}
	
	/**
	 * Makes sure the specified Document equals the XML resource, 
	 * ignoring whitespace and formatting.
	 * @param xmlResource XML resource
	 * @param doc XML document
	 */
	public static void assertEqualsXmlResource(String xmlResource, Document doc) {
		try {
			Document resourceDoc = XmlUtils.loadXml(JuUrl.resourceRelativeTo(xmlResource, ReflectUtils.getCallingClass()));
			TestUtils.assertEqualsXml(resourceDoc, doc);
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't compare XML document to resource", ex);
		}
	}
	
	/**
	 * Makes sure the specified Document equals the XML resource, 
	 * ignoring whitespace and formatting.
	 * @param xmlResource XML resource
	 * @param actualXml XML document
	 */
	public static void assertEqualsXmlResource(String xmlResource, String actualXml) {
		TestUtils.assertEqualsXmlResource(xmlResource, actualXml, ReflectUtils.getCallingClass());
	}
	
	/**
	 * Makes sure the specified XML document equals the XML at the resource, ignoring whitespace
	 * and formatting.
	 * 
	 * @param xmlResource
	 *            URL to XML resource
	 * @param actualXml
	 *            Actual XML
	 */
	public static void assertEqualsXmlResource(URL xmlResource, String actualXml) {
		try {
			Document resourceDoc = XmlUtils.loadXml(xmlResource);
			TestUtils.assertEqualsXml(resourceDoc, XmlUtils.loadXml(actualXml, null));
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't compare XML document to resource", ex);
		}
	}

	/**
	 * Makes sure the specified Document equals the XML resource,
	 * ignoring whitespace and formatting.
	 * 
	 * @param xmlResource
	 *            XML resource
	 * @param actualXml
	 *            XML document
	 * @param clazz
	 *            Class the resource is relative to
	 */
	public static void assertEqualsXmlResource(String xmlResource, String actualXml, Class<?> clazz) {
		URL url = JuUrl.resourceRelativeTo(xmlResource, clazz);
		AssertUtil.assertNotNull(String.format("Couldn't load resource %s [relative to: %s]", xmlResource, clazz), url);

		try {
			Document resourceDoc = XmlUtils.loadXml(url);
			TestUtils.assertEqualsXml(resourceDoc, XmlUtils.loadXml(actualXml, null));
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't compare XML document to resource", ex);
		}
	}
	
	/**
	 * Asserts that two maps are equal. If they are not, Assert.fail is called.
	 * Uses the specified EqualityTester instance to check for equality of the
	 * values.
	 * @param mExpected Expected Map
	 * @param mActual Actual Map
	 * @param equalityTester EqualityTester instance to be used to test for equality. If null,
	 * a default tester will be used that uses the Object.equals method
	 */
	public static <K, V> void assertMapEquals(Map<K, V> mExpected, Map<K, V> mActual, EqualityTester<V> equalityTester) {
		if (!JuCollectionUtils.mapEquals(mExpected, mActual, equalityTester)) {
			Assert.fail("Maps are not equal. Expected: " + mExpected + "; Actual: " + mActual);
		}
	}
	
	/**
	 * Asserts that two maps are equal. If they are not, Assert.fail is called.
	 * @param mExpected Expected Map
	 * @param mActual Actual Map
	 */
	public static <K, V> void assertMapEquals(Map<K, V> mExpected, Map<K, V> mActual) {
		TestUtils.assertMapEquals(mExpected, mActual, null);
	}
	
	/**
	 * Asserts that two arrays are equal. If they are not, Assert.fail is called.
	 * Uses CollectionUtil.arrayEquals to perform the test.
	 * @param aExpected Expected Array
	 * @param aActual Actual Array
	 */
	public static void assertArrayEquals(Object[] aExpected, Object[] aActual) {
		if (!JuCollectionUtils.arrayEquals(aExpected, aActual)) {
			Assert.fail("Arrays are not equal. Expected: " + Arrays.toString(aExpected) + "; Actual: " + Arrays.toString(aActual));
		}
	}
	
	/**
	 * Asserts that two collections are equal. If they are not, Assert.fail is called.
	 * Uses CollectionUtil.collectionEquals to perform the test.
	 * @param cExpected Expected Collection
	 * @param cActual Actual Collection
	 */
	public static <T> void assertCollectionEquals(Collection<? extends T> cExpected, Collection<? extends T> cActual) {
		if (!JuCollectionUtils.collectionEquals(cExpected, cActual)) {
			Assert.fail("Collections are not equal. Expected: " + cExpected + "; Actual: " + cActual);
		}
	}
	
	/**
	 * Convenience method to assert collection using variable parameter list.
	 * @param cActual Actual collection
	 * @param expectedObjects List of expected elements
	 */
	@SafeVarargs
	public static <T> void assertCollectionEquals(Collection<? extends T> cActual, T... expectedObjects) {
		TestUtils.assertCollectionEquals(Arrays.asList(expectedObjects), cActual);
	}
	
	/**
	 * Asserts that a String matches an expected pattern. If it doesn't, Assert.fail is called.
	 * @param expectedPattern Expected regular expression pattern
	 * @param actualString Actual String
	 */
	public static void assertRegexEquals(String expectedPattern, String actualString) {
		if (!new RegexUtil(expectedPattern).matches(actualString)) {
			Assert.fail("String doesn't match pattern. Expected: " + expectedPattern + "; Actual: " + actualString);
		}
	}
	
	/**
	 * Asserts that two XMLs are equal, ignoring whitespace and formatting.
	 * @param expectedDocument Expected document
	 * @param actualDocument Actual document
	 */
	public static void assertEqualsXml(Document expectedDocument, Document actualDocument) {
		String expectedXml = XmlUtils.toString(expectedDocument, false, true);
		String actualXml = XmlUtils.toString(actualDocument, false, true);
		
		Assert.assertEquals(expectedXml, actualXml);
	}
	
	/**
	 * Asserts that all specified values are part of the specified collection.
	 * <p>
	 * The collection may contain more than the specified values
	 * @param cCollection Collection
	 * @param values Values the collection must contain, in arbitrary order
	 */
	@SafeVarargs
	public static <T> void assertCollectionContains(Collection<? extends T> cCollection, T... values) {
		for (T val : values) {
			if (!cCollection.contains(val)) Assert.fail("Value not part of collection: " + val);
		}
	}
	
	/**
	 * Asserts that the specified collection contains all specified values, in arbitray order
	 * and count.
	 * @param collection Collection to test
	 * @param values Values the collection must contain, i.e. the collection must contain all
	 * specified values and no non-specified
	 */
	@SafeVarargs
	public static <T> void assertCollectionConsistsOfAll(Collection<? extends T> collection, T... values) {
		ArrayList<T> c = new ArrayList<>(collection);
		for (T val : values) {
			if (c.remove(val)) {
				while (c.remove(val));
			} else {
				Assert.fail("Element not found in collection: " + val);
			}
		}
		
		if (c.size() > 0) {
			Assert.fail("Collection contains other than the specified values: " + c);
		}
	}
	
	/**
	 * Extracts the TestMethod from a JUnit description as used in TestRules.
	 * @param description JUnit description
	 * @return Test method
	 */
	public static Method getTestMethod(Description description) {
		Class<?> testClass = description.getTestClass();
		AssertUtil.assertNotNull("Description must contain test class", testClass);
		String testMethod = description.getMethodName();
		AssertUtil.assertNotNull("Description must contain test method name", testMethod);
		
		// When using Parameterized runner, the method name in the description will be
		// methodName[index]. Therefore, we'll check if the base statement is an InvokeMethod statement
		if (testMethod.contains("[")) {
			testMethod = testMethod.substring(0, testMethod.indexOf("["));
		}
		
		Method method = ReflectUtils.getMethod(testClass, testMethod, null);
		AssertUtil.assertNotNull(String.format("Couldn't get method using reflection: %s.%s", testClass, testMethod), method);
		
		return method;
	}

	/**
	 * Returns an AssertionBuilder to configure assertions.
	 * 
	 * @return
	 */
	public static AssertionBuilder assertion() {
		return new AssertionBuilder();
	}

	/**
	 * Helper class to configure and execute assertions.
	 * 
	 * @author martin.meyer@inftec.ch
	 *
	 */
	public static final class AssertionBuilder {
		/**
		 * Use TestUtils.assertion()
		 */
		private AssertionBuilder() {
		}

		/**
		 * Compares XMLs.
		 * 
		 * @return
		 */
		public XmlAssertionBuilder xml() {
			return new XmlAssertionBuilder();
		}

		/**
		 * Helper class to compare XMLs.
		 * 
		 * @author martin.meyer@inftec.ch
		 *
		 */
		public static final class XmlAssertionBuilder {
			private URL expectedResourceUrl;
			private String actualXml;

			private final ExportBuilder<XmlAssertionBuilder> exportBuilder = new ExportBuilder<>(this);

			private XmlAssertionBuilder() {
			}

			/**
			 * URL to a resource that contains the expected XML.
			 * 
			 * @param url
			 *            URL to expected XML
			 * @return
			 */
			public XmlAssertionBuilder expectedResource(URL url) {
				this.expectedResourceUrl = url;
				return this;
			}

			/**
			 * Actual XML.
			 * 
			 * @param xml
			 * @return
			 */
			public XmlAssertionBuilder actualXml(String xml) {
				this.actualXml = xml;
				return this;
			}

			/**
			 * Makes sure the actual XML matches the expected XML.
			 */
			public void assertEquals() {
				AssertUtil.assertNotNull("actualXml must be specified", this.actualXml);


				// Either compare to expected resource or export to file, depending on export configuration
				if (this.exportBuilder.enabled) {
					// Export to file

					Path exportFilePath = this.exportBuilder.exportFilePath;
					AssertUtil.assertNotNull("exportFilePath must be specified", exportFilePath);

					logger.debug("Exporting actualXml to file: " + exportFilePath);

					try {
						new IOUtil().writeTextToFile(this.actualXml, exportFilePath, true);
					} catch (JuException ex) {
						throw new JuRuntimeException("Couldn't write actual XML to %s:\n%s", ex, exportFilePath, this.actualXml);
					}
				} else {
					// Compare to expected resource
					AssertUtil.assertNotNull("expectedResource must be specified", this.expectedResourceUrl);
					TestUtils.assertEqualsXmlResource(this.expectedResourceUrl, this.actualXml);
				}
			}

			/**
			 * Allows to configure file exporting of actual contents (instead of comparing to expected resources).
			 * <p>
			 * This can be useful if we want to update the expected resources of our tests and use a VCS to to the comparison.s
			 * 
			 * @return
			 */
			public ExportBuilder<XmlAssertionBuilder> export() {
				return this.exportBuilder;
			}

			/**
			 * Helper class to export actual XMLs instead of comparing to expected resources.
			 * 
			 * @author martin.meyer@inftec.ch
			 *
			 */
			public static final class ExportBuilder<T> {
				private final T returnTo;

				private boolean enabled = false;
				private Path exportFilePath;
				
				private ExportBuilder(T returnTo) {
					this.returnTo = returnTo;
				}

				/**
				 * Whether file export is enabled. Defaults to false.
				 * 
				 * @param enabled
				 * @return
				 */
				public ExportBuilder<T> enable(boolean enabled) {
					this.enabled = enabled;
					return this;
				}

				/**
				 * Set the file path for an export.
				 * 
				 * @param exportFilePath
				 * @return
				 */
				public ExportBuilder<T> exportFilePath(Path exportFilePath) {
					this.exportFilePath = exportFilePath;
					return this;
				}

				/**
				 * Completes export configuration.
				 * 
				 * @return
				 */
				public T done() {
					return this.returnTo;
				}
			}
		}
	}
}
