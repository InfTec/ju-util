package ch.inftec.ju.util.libs;

import java.util.Scanner;

import org.junit.Test;

/**
 * Test cases using console input.
 * @author martin.meyer@inftec.ch
 *
 */
public class JavaUtilTestConsole {
	@Test
	public void scannerTest_simpleInput() {
		try (Scanner scanner = new Scanner(System.in)) {
			String input = null;
			while (!"exit".equalsIgnoreCase(input)) {
				System.out.println("Enter value (exit to end): ");
				input = scanner.next();
				System.out.println("Entered: " + input);
			}
		}
	}
	
	@Test
	public void scannerTest_line() {
		try (Scanner scanner = new Scanner(System.in)) {
			String input = null;
			while (!"exit".equalsIgnoreCase(input)) {
				System.out.println("Enter value (exit to end): ");
				input = scanner.nextLine();
				System.out.println("Entered: " + input);
			}
		}
	}
	
//	@Test
//	public void scannerTest_pattern() {
//		try (Scanner scanner = new Scanner(System.in)) {
//			String input = null;
//			while (!"exit".equalsIgnoreCase(input)) {
//				System.out.println("Enter value (exit to end): ");
//				input = scanner.findInLine("Hello ([\\w*])");
//				System.out.println("Entered: " + input);
//			}
//		}
//	}
}
