package ch.inftec.ju.util.function;

/**
 * Will be obsolete with JDK8.
 */
public interface Function<T, R> {
	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * @return the function result
	 */
	R apply(T t);
}