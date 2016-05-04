package ch.inftec.ju.util.concurrent;

import org.joda.time.Duration;

import ch.inftec.ju.util.ThreadUtils;

import com.google.common.base.Predicate;


/**
 * Class containing concurrency related functionality.
 * <p>
 * Note that implementations may not be high performant and should not be used for productive use, but rather
 * for tests.
 * @author martin.meyer@inftec.ch
 *
 */
public class ConcurrencyUtils {
	private static final Duration DEFAULT_INTERVAL = Duration.millis(50);
	private static final Duration DEFAULT_TIMEOUT = Duration.standardSeconds(10);
	
	/**
	 * Waits for the specified predicate to return true, using default interval (50 ms) and timeout (10 seconds)
	 * @param predicate Predicate
	 * @return True if the predicate succeeds, false otherwise
	 */
	public static boolean waitFor(Predicate<Void> predicate) {
		return ConcurrencyUtils.waitFor(predicate, DEFAULT_INTERVAL, DEFAULT_TIMEOUT);
	}
	
	/**
	 * Waits for the specified predicate to return true.
	 * @param predicate Predicate
	 * @param interval Polling interval
	 * @param timeout Timeout, i.e. time after which false will be returned if the predicate never returned true
	 * @return True if the predicate succeeds, false otherwise
	 */
	public static boolean waitFor(Predicate<Void> predicate, Duration interval, Duration timeout) {
		Duration d = Duration.ZERO;
		
		do {
			boolean res = predicate.apply(null);
			if (res) {
				return true;
			} else {
				d = d.plus(interval);
				
				// TODO: Take execution time into account...
				ThreadUtils.sleep(interval.getMillis());
			}
		} while (d.isShorterThan(timeout));
		
		return false;
	}
}
