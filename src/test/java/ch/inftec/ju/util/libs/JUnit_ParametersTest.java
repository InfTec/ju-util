package ch.inftec.ju.util.libs;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class JUnit_ParametersTest {
	private static Logger logger = LoggerFactory.getLogger(JUnit_ParametersTest.class);
	
	@Parameters
	public static Iterable<Integer[]> params() {
		return Arrays.asList(new Integer[][] {{1}, {2}});
	}
	
	private int param;

	/**
	 * Called after @BeforeClass, for every test case
	 * @param param
	 */
	public JUnit_ParametersTest(int param) {
		logger.debug("Creating test: " + param);
		this.param = param;
	}
	
	/**
	 * Is only called once, before first constructor.
	 */
	@BeforeClass
	public static void beforeClass() {
		logger.debug("Before");
	}
	
	/**
	 * Will be run twice and output 1 and 2.
	 */
	@Test
	public void test1() {
		logger.debug("Param = " + this.param);
	}
	
	/**
	 * Will be run twice and output 11 and 12
	 */
	@Test
	public void test2() {
		logger.debug("Param = " + (this.param + 10));
	}
}