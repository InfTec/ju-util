package ch.inftec.ju.util.helper;

import java.util.Collection;

import ch.inftec.ju.util.AssertUtil;
import ch.inftec.ju.util.JuCollectionUtils;
import ch.inftec.ju.util.function.Function;

/**
 * Helper class to build a FindHelper instance.
 * @author martin.meyer@inftec.ch
 *
 * @param <T> Element type
 */
public class FindHelperBuilder<T> {
	private Iterable<? extends T> items;
	private T noneObject;

	/**
	 * Iterable to be wrapped.
	 */
	public FindHelperBuilder<T> iterable(Iterable<? extends T> iterable) {
		this.items = iterable;
		return this;
	}

	/**
	 * Iterable to be wrapped, transforming the elements after returning them.
	 */
	public <S> FindHelperBuilder<T> iterableTransformed(Iterable<S> srcIterable, Function<S, ? extends T> transformer) {
		Iterable<? extends T> iterableTransformed = JuCollectionUtils.iterableTransformed(srcIterable, transformer);
		return this.iterable(iterableTransformed);
	}

	/**
	 * Collection to be wrapped.
	 * @param coll Collection
	 * @deprecated Use iterable() instead as we accept any kind of Iterable, not only collections
	 */
	@Deprecated
	public FindHelperBuilder<T> collection(Collection<? extends T> coll) {
		return iterable(coll);
	}

	/**
	 * Collection to be wrapped, transforming the elements before adding them.
	 * @param srcColl Source collection
	 * @param transformer Transformer to transform elements
	 * @deprecated Use iterableTransformed() instead as we accept any kind of Iterable, not only collections
	 */
	@Deprecated
	public <S> FindHelperBuilder<T> collectionTransformed(Collection<S> srcColl, Function<S, ? extends T> transformer) {
		return iterableTransformed(srcColl, transformer);
	}
	
	/**
	 * Sets the noneObject, i.e. a dummy object that can be returned in place of
	 * null if no object exists to avoid null pointers.
	 * @param noneObject None object
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
		AssertUtil.assertNotNull("Iterable must be specified", this.items);
		
		return new FindHelper<>(this.items);
	}
	
	/**
	 * Creates a new FindNoneHelper instance.
	 * @return FindNoneHelper
	 */
	public FindNoneHelper<T> createFindNoneHelper() {
		AssertUtil.assertNotNull("Iterable must be specified", this.items);
		AssertUtil.assertNotNull("None Object must be specified", this.noneObject);
		
		return new FindNoneHelper<>(this.items, this.noneObject);
	}
}
