package ch.inftec.ju.util.libs;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Formatter;
import java.util.IllegalFormatConversionException;

import org.junit.Assert;
import org.junit.Test;

import ch.inftec.ju.util.JuStringUtils;

/**
 * Test class containing general Java tests.
 * @author Martin
 *
 */
public class JavaTest {
	private boolean wasClosed = false;
	
	@Test
	public void instanceOf() {
		Assert.assertTrue(new Integer(1) instanceof Integer);
		Assert.assertTrue("Test" instanceof String);
		Assert.assertTrue("Test" instanceof Object);
		
		// nulls
		Assert.assertFalse(null instanceof String);
		Assert.assertFalse(null instanceof Object);
	}
	
	@Test
	public void j7try() {
		this.wasClosed = false;
		try (MyClass c = new MyClass()) {
			Assert.assertFalse(this.wasClosed);
		}
		Assert.assertTrue(this.wasClosed);
		
		this.wasClosed = false;
		boolean hadException = false;
		try (MyClass c = new MyClass()) {
			Assert.assertFalse(this.wasClosed);
			throw new Exception("Fail");
		} catch (Exception ex) {
			hadException = true;
		}
		Assert.assertTrue(this.wasClosed);
		Assert.assertTrue(hadException);
		
		this.wasClosed = false;
		hadException = false;
		try (MyClass c = new MyClass(true)) {
			Assert.fail("Shouldn't reach here");
		} catch (Exception ex) {
			hadException = true;
		}
		Assert.assertFalse(this.wasClosed);
		Assert.assertTrue(hadException);		
	}
	
	/**
	 * Tests the String.format method.
	 */
	@Test
	public void stringFormat() {
		// Padding
		Assert.assertEquals("X-00005", String.format("X-%05d", 5L));
		Assert.assertEquals("X-123456", String.format("X-%05d", 123456L));
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
		// Note that indexing will continue where it was left (%s will be next index based on last non-absolute index)
		Assert.assertEquals("B B A A B B C", String.format("%2$s %<s %1$s %s %s %<s %s", "A", "B", "C"));
		// Note that index continues where it was left:
		//  A is normal, index 1
		//  C is explicit
		//  B is continuing, index 2
		//  B is reusing previous
		//  C is continuing, index 3
		Assert.assertEquals("A C B B C", String.format("%s %3$s %s %<s %s", "A", "B", "C"));
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
	public void weakReference() {
		Long[] largeObject = new Long[100000];
		
		ReferenceQueue<Object> queue = new ReferenceQueue<>();
		WeakReference<Long[]> ref = new WeakReference<Long[]>(largeObject, queue);
		
		Assert.assertSame(largeObject, ref.get());
		
		largeObject = null;
		System.gc();
		
		Assert.assertNull(ref.get());
		
		// Doesn't work, obviously not deterministic... Assert.assertSame(ref, queue.poll());
	}
	
	private class MyClass implements AutoCloseable {
		public MyClass() {
			this(false);
		}
		
		public MyClass(boolean fail) {
			if (fail) throw new RuntimeException("Failed");
		}
		
		@Override
		public void close() {
			wasClosed = true;
		}
	}
	
	@Test
	public void instanceInitializer() {
		InstanceInitializerClass c = new InstanceInitializerClass();
		
		Assert.assertEquals(2, c.x);
	}
	
	@Test
	public void instanceInitializer_anonymous() {
		InstanceInitializerClass c = new InstanceInitializerClass() {
			// Instance initializer of anonymous class
			{
				x = 3;
			}
		};
		
		Assert.assertEquals(3, c.x);
	}
	
	private class InstanceInitializerClass {
		int x = 0;
		
		// Instance initializer
		{
			x = 1;
		}
		
		// Constructor
		InstanceInitializerClass() {
			x = 2;
		}
	}
	
	@Test
	public void arrayInizialization() {
		// Using new keyword
		int a1[] = new int[] {1, 2};
		Assert.assertEquals(1, a1[0]);
		
		// Using brackets
		int a2[] = {1, 2};
		Assert.assertEquals(1, a2[0]);
		
		// Defined size, initialized with 0 (or null if Object type)
		int a3[] = new int[2];
		Assert.assertEquals(0, a3[0]);
	}
	
	@Test
	public void arrayInitialization_multiDimension() {
		// Only first size must be specified. Second dimension will be null here...
		int a1[][][] = new int[1][][];
		Assert.assertNull(a1[0]);
		
		// This defines a two dimensional array
		int[] a2[] = {{0, 1}, {2, 3}};
		Assert.assertEquals(3, a2[1][1]);
		
		// Same as a2
		int a2b[][] = {{0, 1}, {2, 3}};
		int[][] a2c = {{0, 1}, {2, 3}};
		Assert.assertEquals(3, a2b[1][1]);
		Assert.assertEquals(3, a2c[1][1]);
	}
	
	@Test
	@SuppressWarnings("unused")
	public void validIdentifiers() {
		int _;
		int $;
		int a1;
		int a1_$;
		 
		/* Wrong identifiers:
		int 0; // Starts with number
		int 2nd; // Starts with number
		*/
	}
	
	@Test
	public void dataTypes() {
		byte b = 3;
		char c1 = 7;
		char c2 = 'a'; // Unicode 97
		int i = b + c1 + c2;
		
		Assert.assertEquals(107, i);
	}
	
	@Test
	@SuppressWarnings("unused")
	public void implicitNarrowing() {
		byte b = 1;
		char c = (char)b; // Valid, explicit cast
		// Invalid: char c = b; // b could be negative...
		final byte bFinal = 1;
		char c2 = bFinal; // Implicit narrowing, compiler knows bFinal is constant = 1
		
		final long lFinal = 1L;
		// Invalid: int i = lFinal; // Implicit narrowing not supported for int
		// Invalid: short s = lFinal; // Cannot narrow long
		
		final int iFinal = 1;
		short s = iFinal; // Narrowing from int to short is ok
	}
}
