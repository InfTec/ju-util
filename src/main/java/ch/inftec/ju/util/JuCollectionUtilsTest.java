package ch.inftec.ju.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class JuCollectionUtilsTest {
	@Test
	public void iterator_asList() {
		final List<String> l = JuCollectionUtils.arrayList("A", "B");
		
		Iterable<String> iterable = new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return l.iterator();
			}
		};
		
		List<String> lRes = JuCollectionUtils.asList(iterable);
		TestUtils.assertCollectionEquals(lRes, "A", "B");
	}

	@Test
	public void equalCollection_collectionEquals_returnsTrue() {
		Arrays.asList(1, 2);
	}
}
