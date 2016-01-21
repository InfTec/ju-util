package ch.inftec.ju.util.helper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.inftec.ju.util.AssertUtil;
import ch.inftec.ju.util.JuCollectionUtils;

/**
 * Helper class that wraps a collection and provides convenience methods to access
 * their elements.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 * @param <T> Type of the elements
 */
public class FindHelper<T> {
	private final Collection<? extends T> items;
	
	public FindHelper(Collection<? extends T> items) {
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
		List<? extends T> all = this.all();
		AssertUtil.assertFalse("Expected no more than 1 item. Found " + all.size(), all.size() > 1);
		
		return all.size() == 0 ? null : all.get(0);
	}
}
