package ch.inftec.ju.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class JuCollectionUtilsTest {
	private List<Integer> list12 = Arrays.asList(1, 2);
	private List<Integer> list12_copy = Arrays.asList(1, 2);
	private List<Integer> list23 = Arrays.asList(2, 3);
	private List<Integer> list123 = Arrays.asList(1, 2, 3);

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
	public void nullAndCollection_collectionEquals_returnsFalse() {
		assertFalse(JuCollectionUtils.collectionEquals(list12, null));
	}

	@Test
	public void nullAndNull_collectionEquals_returnsTrue() {
		assertTrue(JuCollectionUtils.collectionEquals(null, null));
	}

	@Test
	public void nullAndEmpty_collectionEquals_returnsFalse() {
		assertFalse(JuCollectionUtils.collectionEquals(null, Collections.emptyList()));
	}

	@Test
	public void sameCollection_collectionEquals_returnsTrue() {
		assertTrue(JuCollectionUtils.collectionEquals(list12, list12));
	}

	@Test
	public void equalCollection_collectionEquals_returnsTrue() {
		assertTrue(JuCollectionUtils.collectionEquals(list12, list12_copy));
	}
}
