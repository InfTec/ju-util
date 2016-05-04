package ch.inftec.ju.util.collection;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.inftec.ju.util.function.Function;

public class SimpleCacheTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Cache<String, String> cache = new SimpleCache<>(2, new Function<String, String>() {
		@Override
		public String apply(String s) {
			return s + "_val";
		}
	});

	@Test
	public void unexistingValue_newValue_isAdded() {
		String val = cache.get("k1");

		assertEquals("k1_val", val);
		assertEquals(1, cache.size());
	}

	@Test
	public void valueIsPut_get_returnsValue() {
		cache.put("k1", "v1");

		assertEquals("v1", cache.get("k1"));
		assertEquals(1, cache.size());
	}

	@Test
	public void maxSizeReached_putWithNewValue_replacesValue() {
		cache.put("k1", "v1");
		cache.put("k2", "v2");
		cache.put("k3", "v3");

		assertEquals("v3", cache.get("k3"));
		assertEquals(2, cache.size());
	}

	@Test
	public void maxSizeReached_getWithNewKey_replacesValue() {
		cache.put("k1", "v1");
		cache.put("k2", "v2");
		cache.put("k3", "v3");

		assertEquals("k1_val", cache.get("k1"));
		assertEquals(2, cache.size());
	}

	@Test
	public void maxSizeReachedNoGet_getWithNewKey_replacesValueFirstPut() {
		cache.put("k1", "v1");
		cache.put("k2", "v2");

		cache.put("k3", "v3");
		assertEquals("k1_val", cache.get("k1"));
	}

	@Test
	public void maxSizeReachedWithGet_getWithNewKey_replacesValueLongestNotReturned() {
		cache.put("k1", "v1");
		cache.put("k2", "v2");

		assertEquals("v1", cache.get("k1"));

		cache.put("k3", "v3");
		assertEquals("v1", cache.get("k1"));
	}

	@Test
	public void maxSizeZero_newSimpleCache_throwsException() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("maxSize must be greater than 0, but was 0");

		new SimpleCache<>(0, new Function<String, String>() {
			@Override
			public String apply(String s) {
				return null;
			}
		});
	}
}
