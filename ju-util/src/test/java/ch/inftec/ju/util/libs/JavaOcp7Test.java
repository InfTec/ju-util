package ch.inftec.ju.util.libs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import ch.inftec.ju.util.JuCollectionUtils;
import ch.inftec.ju.util.JuStringUtils;

public class JavaOcp7Test {
	@Test(expected=NullPointerException.class)
	public void switchWith_nullString() {
		String s = null;
		switch (s) {
		}
	}
	
	@Test
	public void instanceOf_withImpossibleType() {
		String s;
		// Not compiling: if (s instanceof java.util.Date);
	}
	
	interface I1 {
		void m1() throws IOException;
		int m2();
	}
	
	interface I2 {
		void m1() throws SQLException;
		// Not possible, would collide with I1.m2... long m2();
	}
	
	static class C implements I1, I2 {
		@Override
		public void m1() {
			// One method for both interfaces. Exception must be compatible to both.
		}

		@Override
		public int m2() {
			// Only one m2() is possible...
			return 0;
		}
		
	}
	
	static String mainMessage = null;
	
	static class Printer1 extends Thread {
		private int interrupted;
		private String message;
		
		@Override
		public void run() {
			while (JavaOcp7Test.mainMessage == null) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException ex) {
					this.interrupted++;
				}
			}
			message = JavaOcp7Test.mainMessage;
		}
	}
	
	static class Printer2 extends Thread {
		private int interrupted;
		private String message;
		
		@Override
		public void run() {
			synchronized(JavaOcp7Test.class) {
				while (JavaOcp7Test.mainMessage == null) {
					try {
						JavaOcp7Test.class.wait();
					} catch (InterruptedException ex) {
						this.interrupted++;
					}
				}
				message = JavaOcp7Test.mainMessage;
			}
		}
	}
	
	@Test
	public void waitAndNotify() throws Exception {
		Printer1 printer1 = new Printer1();
		Printer2 printer2 = new Printer2();
		
		printer1.start();
		printer2.start();
		
		Thread.sleep(200);

		// We use the object's class as a lock
		synchronized(JavaOcp7Test.class) {
			mainMessage = "Message";
			JavaOcp7Test.class.notifyAll();
		}
		
		printer1.join();
		printer2.join();
		
		// None of the Threads will have been interrupted in this scenario
		Assert.assertEquals(0, printer1.interrupted);
		Assert.assertEquals(0, printer2.interrupted);
		
		Assert.assertEquals("Message", printer1.message);
		Assert.assertEquals("Message", printer2.message);
	}

	@Test
	public void formatter() throws Exception {
		// Usage: We can instanciate a formatter directly, use System.out.printf or String.format
		Formatter f = new Formatter(); // Uses StringBuilder internally. Another output may be specified
		Assert.assertEquals("Hello World", f.format("Hello %s", "World").toString());
		Assert.assertEquals("Hello World", String.format("Hello %s", "World"));
		System.out.printf("Hello %s", "World");

		// Argument indexing: %[argumentIndex]$
		// Note that indexing is 1 based
		Assert.assertEquals("Hello World", String.format("%2$s %1$s", "World", "Hello"));
		Assert.assertEquals("Hello", String.format("%2$s", "World", "Hello"));

		// Relative indexing
		// < causes the previous argument to be reused...
		// Note that indexing will continue where it was left (%s will be next index based on previous argument)
		Assert.assertEquals("B B A A B B C", String.format("%2$s %<s %1$s %s %s %<s %s", "A", "B", "C"));

		// Conversions
		// For results containing characters, an upper case letter may be used to perform upper case results

		// toString
		Assert.assertEquals("a", String.format("%s", "a"));
		Assert.assertEquals("A", String.format("%S", "a"));
		Assert.assertEquals("null", String.format("%s", (Object) null));

		// Boolean
		Assert.assertEquals("false", String.format("%b", false));

		// Decimal
		Assert.assertEquals("17", String.format("%d", 17));
		
		// Hex (h or x)
		Assert.assertEquals("11", String.format("%h", 17));
		Assert.assertEquals("null", String.format("%h", (Object) null));
		
		// Floating point
		Assert.assertEquals("340282346638528860000000000000000000000.000000", String.format("%f", Float.MAX_VALUE)); // Decimal
		Assert.assertEquals("3.40282e+38", String.format("%g", Float.MAX_VALUE)); // Scientific
		Assert.assertEquals("0x1.fffffep127", String.format("%a", Float.MAX_VALUE)); // hexadeximal floating point

		// NOT allowed!!!
		try {
			Assert.assertEquals("", String.format("%f", 17L));
			Assert.fail("Not expected...");
		} catch (IllegalFormatConversionException ex) {
			Assert.assertEquals("f != java.lang.Long", ex.getMessage());
		}

		// Percent and newline
		// NewLine is platform specific... Assert.assertEquals("% \n", String.format("%% %n"));

		// Date/Time conversion
		Date d = JuStringUtils.DATE_FORMAT_SECONDS.parse("01.02.2003 11:12:13");
		Assert.assertEquals("2003-02-01", String.format("%tF", d)); // ISO 8601
		Assert.assertEquals("11.12", String.format("%1$tH.%1$tM", d));

		// Flags

		// Left justify / Width
		Assert.assertEquals("Hello      World", String.format("%-10s %s", "Hello", "World"));
		Assert.assertEquals("     Hello World", String.format("%10s %s", "Hello", "World")); // Right justify..
		Assert.assertEquals("Hello", String.format("%1s", "Hello")); // Not truncated...
		Assert.assertEquals(" 17", String.format("%3d", 17));

		// Precision
		Assert.assertEquals("17.00", String.format("%3.2f", 17f));
		Assert.assertEquals(" 1.0", String.format("%4.1f", 1f)); // Note that width includes the . and fraction

		// Zero padding
		Assert.assertEquals("017", String.format("%03d", 17));

		// Include sign
		Assert.assertEquals("+17", String.format("%+d", 17));

		// Enclose negative numbers i parantheses
		Assert.assertEquals("(17)", String.format("%(d", -17));
	}

	@Test
	public void binarySearch() {
		char c[] = { 'a', 'c', 'd' };

		// Return index if key was found
		Assert.assertEquals(0, Arrays.binarySearch(c, 'a'));

		// Return (-(insertion point) - 1) if key was not found
		// insertion point is the index the element would be inserted into
		Assert.assertEquals(-4, Arrays.binarySearch(c, 'e'));
		Assert.assertEquals(-2, Arrays.binarySearch(c, 'b'));

		// Note that the array needs to be sorted to use binary search. Otherwise, results may not make sense...
		char cUnsorted[] = { 'c', 'a' };
		Assert.assertEquals(-1, Arrays.binarySearch(cUnsorted, 'b'));
	}

	@Test
	public void regularExpressions() {
		// Patterns and matches
		Pattern p1 = Pattern.compile("[a-c]");
		Matcher m1 = p1.matcher("0abcde");

		// Matches
		Assert.assertFalse(m1.matches());

		// Split
		String[] split = p1.split("0a1");
		Assert.assertArrayEquals(new String[] { "0", "1" }, split);

		// Finds and Regions
		try {
			Assert.assertEquals(0, m1.start());
			Assert.fail("no match available yet");
		} catch (IllegalStateException ex) {
			Assert.assertEquals("No match available", ex.getMessage());
		}
		Assert.assertTrue(m1.find()); // Pattern can be found, match is available
		Assert.assertEquals(0, m1.regionStart()); // regionStart and regionEnd must be set using Matcher.region(start, stop)
		Assert.assertEquals(1, m1.start()); // Start of this match
		Assert.assertEquals("a", m1.group());
		Assert.assertEquals("a", m1.group(0)); // We'll have one group with index 0 which is the whole match
		
		Assert.assertTrue(m1.find()); // Finds the next occurence
		Assert.assertEquals(2, m1.start());

		Assert.assertFalse(m1.find(4)); // Last match is at index 3
		
		// Groups
		Pattern p2 = Pattern.compile("([ab])((?<middle>.*)[cd])");
		Matcher m2 = p2.matcher("aHellod");
		Assert.assertTrue(m2.find());

		// Indexed groups
		Assert.assertEquals("aHellod", m2.group(0));
		Assert.assertEquals("a", m2.group(1));
		Assert.assertEquals("Hellod", m2.group(2));
		Assert.assertEquals("Hello", m2.group(3));

		// Named groups
		Assert.assertEquals("Hello", m2.group("middle"));
	}

	@Test
	public void stringTokenizer() {
		StringTokenizer st = new StringTokenizer("Hello World");
		Assert.assertEquals(2, st.countTokens());
		Assert.assertTrue(st.hasMoreTokens());
		Assert.assertEquals("Hello", st.nextToken());
		Assert.assertEquals("World", st.nextToken());
		Assert.assertFalse(st.hasMoreTokens());
	}

	@Test
	public void locale() {
		// Default Locale
		Assert.assertNotNull(Locale.getDefault());

		// Get Locale by codes
		Assert.assertNotNull(new Locale("ru", "RU")); // Country code is always upper case

		// Get Locale by constant
		Locale l = Locale.US;

		// Methods
		Assert.assertEquals("US", l.getCountry());
		Assert.assertEquals("English", l.getDisplayLanguage());
	}
	
	/**
	 * Following file structure is given:
	 * 
	 *   File1.txt
	 *   File2.sql
	 *   sub
	 *     subFile1.txt
	 */
	@Test
	public void globs() throws Exception {
		Path rootPath = Paths.get("src/test/resources/ch/inftec/ju/util/libs/glob");
		Path pFile1Txt = rootPath.resolve("File1.txt");
		Path pFile1Sql = rootPath.resolve("File1.sql");
		Path pSubFile1Txt = rootPath.resolve("sub/subFile1.txt");
		
		// Search directory
		DirectoryStream<Path> ds = Files.newDirectoryStream(rootPath, "*.txt");
		List<Path> paths = JuCollectionUtils.asList(ds);
		Assert.assertEquals(1, paths.size()); // Note that search is not recursive
		Assert.assertEquals(pFile1Txt, paths.get(0));
		
		// Use PathMatcher
		PathMatcher pm1 = FileSystems.getDefault().getPathMatcher("glob:*"); // Note that we need the glob prefix here
		Assert.assertFalse(pm1.matches(pFile1Txt)); // Note that * doesn't cross directory boundaries
		Assert.assertTrue(pm1.matches(pFile1Txt.getFileName()));

		// ** crosses directory boundaries
		Assert.assertTrue(FileSystems.getDefault().getPathMatcher("glob:**").matches(pFile1Txt));
		
		// Multi Matches
		Assert.assertTrue(FileSystems.getDefault().getPathMatcher("glob:**.{txt,sql}").matches(pFile1Txt));
		Assert.assertFalse(FileSystems.getDefault().getPathMatcher("glob:**.{html,xml}").matches(pFile1Txt));
		
		// Complex Matches
		PathMatcher pm2 = FileSystems.getDefault().getPathMatcher("glob:**/sub/sub[A-F]ile?.{txt,bak}");
		Assert.assertFalse(pm2.matches(pFile1Txt));
		Assert.assertTrue(pm2.matches(pSubFile1Txt));
	}
	
	private static class SuppressedExceptionTest implements AutoCloseable {
		private String closeExceptionMsg;
		
		public SuppressedExceptionTest(boolean throwExceptionOnCreate, String closeExceptionMsg) {
			if (throwExceptionOnCreate) {
				throw new RuntimeException("Create");
			}
			this.closeExceptionMsg = closeExceptionMsg;
		}
		@Override
		public void close() throws Exception {
			if (closeExceptionMsg != null) throw new Exception(this.closeExceptionMsg);
		}
	}
	
	@Test
	public void suppressedExceptions() {
		// No suppressed exception
		try {
			try (SuppressedExceptionTest t = new SuppressedExceptionTest(true, null)) {
			}
		} catch (Exception ex) {
			Assert.assertEquals("Create", ex.getMessage());
			Assert.assertEquals(0, ex.getSuppressed().length);
		}
		
		try {
			try (SuppressedExceptionTest t1 = new SuppressedExceptionTest(false, "T1")
					; SuppressedExceptionTest t2 = new SuppressedExceptionTest(false, "T2")
					; SuppressedExceptionTest t3 = new SuppressedExceptionTest(true, null)) {
			}
		} catch (Exception ex) {
			Assert.assertEquals("Create", ex.getMessage());
			Assert.assertEquals(2, ex.getSuppressed().length);
			Assert.assertEquals("T2", ex.getSuppressed()[0].getMessage());
			Assert.assertEquals("T1", ex.getSuppressed()[1].getMessage());
		}
	}
	
	private static int staticVar;
	private int instVar;
	
	private static class StaticNestedClass {
		public int getStaticVar() {
			// Not possible: return instVar;
			return staticVar;
		}
	}
	
	private class InnerClass {
		public int getInstVar() {
			return instVar;
		}
	}
	
	@Test
	public void nestedClasses() {
		// Static nested class
		
		StaticNestedClass snc = new JavaOcp7Test.StaticNestedClass(); // Could also just use new StaticNestedClass here
		Assert.assertEquals(0, snc.getStaticVar());
		Assert.assertEquals("class ch.inftec.ju.util.libs.JavaOcp7Test$StaticNestedClass", snc.getClass().toString());
		
		// Inner Classes
		
		InnerClass ic1 = new InnerClass(); // InnerClass related to this
		Assert.assertEquals(0, ic1.getInstVar());
		Assert.assertEquals("class ch.inftec.ju.util.libs.JavaOcp7Test$InnerClass", ic1.getClass().toString());
		
		JavaOcp7Test t2 = new JavaOcp7Test();
		t2.instVar = 1;
		InnerClass ic2 = t2.new InnerClass(); // InnerClass related to arbitrary object
		Assert.assertEquals(1, ic2.getInstVar());
		
		// Anonymous Class
		InnerClass ica = new InnerClass() {
			int v1;
			
			// Anonymous classes cannot have constructores, but initializers. They are parameter-less.
			{
				v1 = 3;
			}
			
			@Override
			public int getInstVar() {
				return v1;
			}
		};
		Assert.assertEquals(3, ica.getInstVar());
		Assert.assertEquals("class ch.inftec.ju.util.libs.JavaOcp7Test$1", ica.getClass().toString());
		
		// Local class
		class LocalClass extends InnerClass {
			private int v2;
			
			// Local classes can have constructors
			LocalClass(int v2) {
				this.v2 = v2;
			}
			
			public int getV2() {
				return v2;
			}
		}
		
		// Can be instanciated like normal class
		LocalClass lc1 = new LocalClass(5);
		Assert.assertEquals(5, lc1.getV2());
		Assert.assertEquals("class ch.inftec.ju.util.libs.JavaOcp7Test$1LocalClass", lc1.getClass().toString());
	}
	
	public static final class AssertTest {
		public static void main(String args[]) {
			int i = 1;
			assert true;
			assert i < 10 : "One not smaller than 10";
			assert false : getMsg("Custom Message");
			
			System.out.println("Done");
		}
	}
	
	private static String getMsg(String msg) {
		return "My assertion failed: " + msg;
	}
}

