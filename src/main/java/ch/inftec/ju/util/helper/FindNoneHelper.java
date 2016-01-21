package ch.inftec.ju.util.helper;

import java.util.Collection;

/**
 * Extension of FindHelper that provides additional support for empty objects
 * or dummy objects to avoid null pointers.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 * 
 * @param <T> Element type
 *
 */
public class FindNoneHelper<T> extends FindHelper<T> {
	private final T noneObject;
	
	/**
	 * Creates a new FindHelper
	 * @param coll Collection containing elements
	 */
	public FindNoneHelper(Collection<? extends T> coll, T noneObject) {
		super(coll);
		
		this.noneObject = noneObject;
	}
	
	/**
	 * Returns an actual element if exactly one exists or a dummy implementation
	 * that will return default values if none exists.
	 * <p>
	 * If more than one element exists, an exception will be thrown
	 * @return Actual element if one exists or a dummy implementation
	 */
	public T oneOrNone() {
		T one = this.oneOrNull();
		
		return one == null
			? this.noneObject
			: one;
	}
}
