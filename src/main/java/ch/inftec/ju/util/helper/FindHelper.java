package ch.inftec.ju.util.helper;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ch.inftec.ju.util.AssertUtil;
import ch.inftec.ju.util.JuCollectionUtils;
import ch.inftec.ju.util.JuRuntimeException;

/**
 * Helper class that wraps a collection and provides convenience methods to access
 * their elements.
 * @author martin.meyer@inftec.ch
 *
 * @param <T> Type of the elements
 */
public class FindHelper<T> {
	private final Iterable<? extends T> items;
	
	public FindHelper(Iterable<? extends T> items) {
		this.items = items != null
				? items
				: Collections.<T>emptyList();
	}
	
	/**
	 * Returns a list of all items of the collection.
	 * @return List of all items
	 */
	public List<? extends T> all() {
		return JuCollectionUtils.asList(this.items);
	}
	
	/**
	 * Returns exactly one item.
	 * <p>
	 * If none or more than one exist, an exception is thrown
	 * @return One item
	 */
	public T one() {
		T one = this.oneOrNull();
		AssertUtil.assertNotNull("No element available", one);
		return one;
	}
	
	/**
	 * Returns one item or null if none exists.
	 * <p>
	 * If more than 1 exist, an exception is thrown
	 * @return One item or null
	 */
	public T oneOrNull() {
		Iterator<? extends T> iterator = this.items.iterator();

		if (iterator.hasNext()) {
			// At least one item in iterable
			T val = iterator.next();

			// Make sure there are no more values...
			if (iterator.hasNext()) {
				throw new JuRuntimeException("More than 1 item available");
			} else {
				return val;
			}
		} else {
			return null;
		}
	}
}
