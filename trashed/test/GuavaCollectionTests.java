package ch.inftec.ju.util.libs;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class GuavaCollectionTests {
	private LoadingCache<String, String> createCache() {
		LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(new CacheLoader<String, String>() {
			@Override
			public String load(String key) throws Exception {
				return key + "val";
			}
		});

		return cache;
	}

	@Test
	public void loadingCache_willLoadValues() throws Exception {
		LoadingCache<String, String> c = createCache();

		String val = c.get("key");
		Assert.assertEquals("keyval", val);
	}

	@Test
	public void cacheBuilder_willNotLoadValues_asMap() {
		Cache<String, String> c = createCache();

		String val = c.asMap().get("key");
		Assert.assertNull(val);
	}
}
