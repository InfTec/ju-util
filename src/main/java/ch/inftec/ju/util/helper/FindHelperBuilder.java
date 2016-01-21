package ch.inftec.ju.util.helper;

import java.util.Collection;

import ch.inftec.ju.util.AssertUtil;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

/**
 * Helper class to build a FindHelper instance.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 * @param <T> Element type
 */
public class FindHelperBuilder<T> {
	private Collection<? extends T> collection;
	private T noneObject;
	
	/**
	 * Collection to be wrapped.
	 * @param coll Collection
	 * @return
	 */
	public FindHelperBuilder<T> collection(Collection<? extends T> coll) {
		this.collection = coll;
		return this;
	}
	
	/**
	 * Collection to be wrapped, transforming the elements before adding them.
	 * @param srcColl Source collection
	 * @param transformer Transformer to transform elements
	 * @return
	 */
	public <S> FindHelperBuilder<T> collectionTransformed(Collection<S> srcColl, Function<? super S, ? extends T> transformer) {
		return this.collection(Collections2.transform(srcColl, transformer));
	}
	
	/**
	 * Sets the noneObject, i.e. a dummy object that can be returned in place of
	 * null if no object exists to avoid null pointers.
	 * @param noneObject None object
	 * @return 
	 */
	public FindHelperBuilder<T> noneObject(T noneObject) {
		this.noneObject = noneObject;
		return this;
	}
	
	/**
	 * Creates a new FindHelper instance.
	 * @return FindHelper
	 */
	public FindHelper<T> createFindHelper() {
		AssertUtil.assertNotNull("Collection must be specified", this.collection);
		
		return new FindHelper<>(this.collection);
	}
	
	/**
	 * Creates a new FindNoneHelper instance.
	 * @return FindNoneHelper
	 */
	public FindNoneHelper<T> createFindNoneHelper() {
		AssertUtil.assertNotNull("Collection must be specified", this.collection);
		AssertUtil.assertNotNull("None Object must be specified", this.noneObject);
		
		return new FindNoneHelper<>(this.collection, this.noneObject);
	}
}
