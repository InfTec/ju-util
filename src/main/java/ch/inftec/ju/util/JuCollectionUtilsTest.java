package ch.inftec.ju.util;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

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
}
