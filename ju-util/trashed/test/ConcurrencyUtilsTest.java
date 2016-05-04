package ch.inftec.ju.util.concurrent;

import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Predicate;

public class ConcurrencyUtilsTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void waitFor_returnsTrue_forSuccessfulPolling() {
		TestingPredicate tp = new TestingPredicate(5);
		boolean res = ConcurrencyUtils.waitFor(tp);
		
		Assert.assertTrue(res);
		Assert.assertEquals(5, tp.callCnt);
	}
	
	@Test
	public void waitFor_returnsFalse_forUnsuccessfulPolling() {
		TestingPredicate tp = new TestingPredicate(0);
		boolean res = ConcurrencyUtils.waitFor(tp, Duration.millis(10), Duration.millis(100));
		
		Assert.assertFalse(res);
		Assert.assertTrue(tp.callCnt > 2);
	}
	
	@Test
	public void waitFor_throwsException_forPredicateThrowingException() {
		this.thrown.expect(RuntimeException.class);
		this.thrown.expectMessage("Failure");
		
		ConcurrencyUtils.waitFor(new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				throw new RuntimeException("Failure");
			}
		}, Duration.millis(10), Duration.standardSeconds(1));
		
	}
	
	private static class TestingPredicate implements Predicate<Void> {
		private final int trueAfterCnt;
		private int callCnt;
		
		private TestingPredicate(int trueAfterCnt) {
			this.trueAfterCnt = trueAfterCnt;
		}
		
		@Override
		public boolean apply(Void input) {
			this.callCnt++;
			
			return (this.trueAfterCnt > 0 && this.callCnt >= this.trueAfterCnt);
		}
	}
}
