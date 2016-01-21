package ch.inftec.ju.util;

import org.junit.Assert;
import org.junit.Test;

public class DataHolderTest {
	@Test
	public void waitForValue_returnsValue_ifAvailable() {
		final DataHolder<String> res = new DataHolder<>();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				res.setValue("res");
			}
		}).start();
		
		Assert.assertEquals("res", res.waitForValue(1000));
	}
	
	@Test
	public void waitForValue_returnsNull_ifNotAvailable() {
		DataHolder<String> res = new DataHolder<>();
		Assert.assertNull(res.waitForValue(100));
	}
}
