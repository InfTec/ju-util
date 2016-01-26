package ch.inftec.ju.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import ch.inftec.ju.util.function.Function;

public class JuCollectionUtilsTest {
	private List<Integer> list12 = Arrays.asList(1, 2);
	private List<Integer> list12_copy = Arrays.asList(1, 2);
	private List<Integer> list23 = Arrays.asList(2, 3);
	private List<Integer> list123 = Arrays.asList(1, 2, 3);
	private List<Integer> list21 = Arrays.asList(2, 1);
	private List<Integer> list34 = Arrays.asList(3, 4);

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
		assertNotSame(list12, list12_copy);

		assertTrue(JuCollectionUtils.collectionEquals(list12, list12_copy));
	}

	@Test
	public void nonEqualCollectionDifferentLength_collectionEquals_returnsFalse() {
		assertFalse(JuCollectionUtils.collectionEquals(list12, list123));
	}

	@Test
	public void nonEqualCollectionSameLength_collectionEquals_returnsFalse() {
		assertFalse(JuCollectionUtils.collectionEquals(list12, list23));
	}

	@Test
	public void collectionWithSameItemsDifferentOrder_collectionEquals_returnsFalse() {
		assertFalse(JuCollectionUtils.collectionEquals(list12, list21));
	}

	@Test
	public void collectionWithSameItemsDifferentOrder_collectionEqualsIgnoreOrder_returnsTrue() {
		assertTrue(JuCollectionUtils.collectionEqualsIgnoreOrder(list12, list21));
	}

	@Test
	public void nullParameter_intersection_returnsEmptyList() {
		assertTrue(JuCollectionUtils.intersection(null, list12).isEmpty());
	}

	@Test
	public void sameCollection_intersection_returnsNewList() {
		List<Integer> intersection = JuCollectionUtils.intersection(list12, list12);

		assertNotSame(list12, intersection);
		assertTrue(JuCollectionUtils.collectionEquals(list12, intersection));
	}

	@Test
	public void nonOverlappingCollections_intersection_returnsEmptyList() {
		assertTrue(JuCollectionUtils.intersection(list12, list34).isEmpty());
	}

	@Test
	public void overlappingCollections_intersection_returnsOverlappingItems() {
		List<Integer> intersection = JuCollectionUtils.intersection(list12, list123);

		assertNotSame(list12, intersection);
		assertTrue(JuCollectionUtils.collectionEquals(list12, intersection));
	}

	@Test
	public void iteratorTransformed_transformsElements() {
		List<Integer> list = JuCollectionUtils.asArrayList(1, 2);

		Iterator<Integer> transformedIterator = JuCollectionUtils.iteratorTransformed(list.iterator(), getMultiplyByTwoFunction());

		assertEquals(Integer.valueOf(2), transformedIterator.next());
		assertEquals(Integer.valueOf(4), transformedIterator.next());
		assertFalse(transformedIterator.hasNext());
	}

	private Function<Integer, Integer> getMultiplyByTwoFunction() {
		return new Function<Integer, Integer>() {
			@Override
			public Integer apply(Integer integer) {
				return integer * 2;
			}
		};
	}

	@Test
	public void iterableTransformed_transformsElements() {
		List<Integer> list = JuCollectionUtils.asArrayList(1);

		Iterable<Integer> transformedIterable = JuCollectionUtils.iterableTransformed(list, getMultiplyByTwoFunction());

		Iterator<Integer> transformedIterator = transformedIterable.iterator();

		assertEquals(Integer.valueOf(2), transformedIterator.next());
		assertFalse(transformedIterator.hasNext());
	}
}

