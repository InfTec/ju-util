package ch.inftec.ju.util;

/**
 * Interface that adds interpolation support to PropertyChain.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 */
public interface InterpolatingPropertyChain extends PropertyChain {
	/**
	 * Interpolates the specified expression using the values of this PropertyChain.
	 * <p>
	 * If interpolation fails, the same expression is returned and a warning message may
	 * be output in the log.
	 * @param expression Expression to be interpolated
	 * @return Interpolated value
	 */
	String interpolate(String expression);
}
