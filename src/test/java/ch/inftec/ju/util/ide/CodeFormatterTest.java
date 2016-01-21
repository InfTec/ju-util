package ch.inftec.ju.util.ide;

/**
 * Test class for the (Eclipse) code formatter. When we find a behavior of the formatter we don't like,
 * we can add a test code snippet here to make sure it won't happen again.
 * 
 * @author Martin Meyer <martin.meyer@inftec.ch>
 * 
 */
public class CodeFormatterTest {
	/**
	 * Space before params.
	 * 
	 * @param param1
	 *            Test
	 * @param param2
	 *            Test2.
	 *            With line breaks.
	 * @return False
	 */
	public boolean javaDoc(String param1, String param2) {
		return false;
	}

	/**
	 * New lines for blocks, keep one-lines on same line.
	 * 
	 * @param test
	 */
	public void ifThenElse(boolean test) {
		if (test) {
			System.out.println("block");
		}

		if (test) {
			System.out.println("block");
		} else if (test) {
			System.out.println("block");
		} else {
			System.out.println("block");
		}
		if (test) System.out.println("oneLine");

		// Not good practice, but verify formatter...
		if (test) {
			System.out.println("block");
		} else if (test) System.out.println("oneLine");
		else System.out.println("oneLine");

		if (test) System.out.println("oneLine");
		else System.out.println("oneLine");
	}

	/**
	 * New line and indent for statements.
	 * Case on same line as switch (seems to be usual convention).
	 * 
	 * @param num
	 */
	public void switchTest(int num) {
		switch (num) {
		case 1:
			System.out.println("switch1");
			break;
		case 2:
			System.out.println("switch1");
		case 3:
			System.out.println("switch1");
			System.out.println("switch2");
			break;
		case 4: // empty
		case 5:
		default:
			System.out.println("switch1");
		}
	}

	/**
	 * Make sure we can use formatter flags.
	 */
	public void formatterFlags() {
		// @formatter:off
		int a = 7; int b = 8;
		  System.out
		    .println("Some nasty formatting: " + a
		  + b);
		// @formatter:on
		System.out
				.println("Some nasty formatting: " + a
						+ b);
	}
}
