package ch.inftec.ju.util.function;

/**
 * Will be obsolete with JDK8.
 */
public interface Predicate<T> {

	/**
	 * Evaluates this predicate on the given argument.
	 *
	 * @param t the input argument
	 * @return {@code true} if the input argument matches the predicate,
	 * otherwise {@code false}
	 */
	boolean test(T t);
}