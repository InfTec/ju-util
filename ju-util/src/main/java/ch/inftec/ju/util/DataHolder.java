package ch.inftec.ju.util;

/**
 * Simple data holder class that can hold a value. Can be used
 * to store values from an anonymous inner class for instance (declare an instance
 * as final and use setValue to return the value).
 * @author Martin
 *
 * @param <T>
 */
public class DataHolder<T> {
	private T t;
	private static final int POLLING_INTERVAL = 50;
	
	public T getValue() {
		return this.t; 
	}
	
	public void setValue(T val) {
		this.t = val;
	}
	
	/**
	 * Waits for a value and returns it as soon as it is available. If no value is available within
	 * the timeout, null is returned.
	 * @param timeout Timeout to wait for value
	 * @return Value or null if onne is available within the timeout
	 */
	// TODO: Refactor...! (make more robust, avoid polling)
	public T waitForValue(int timeout) {
		int waitTime = 0;
		
		do {
			if (this.t != null) {
				return t;
			} else {
				ThreadUtils.sleep(POLLING_INTERVAL);
				waitTime += POLLING_INTERVAL;
			}
		} while (waitTime < timeout);
		
		return null;
	}
}
