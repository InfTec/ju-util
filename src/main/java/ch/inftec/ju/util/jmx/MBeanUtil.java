package ch.inftec.ju.util.jmx;

import ch.inftec.ju.util.helper.ValueConverter;

/**
 * Interface to work with Java JMX MBeans.
 * <p>
 * Use the MBeanUtils class to get instances of MBeanUtil.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 */
public interface MBeanUtil {
	/**
	 * Gets the attribute with the specified name from the MBean.
	 * @param attributeName Attribute name
	 * @return ValueConverter used to get the value of the attribute
	 */
	ValueConverter getAttribute(String attributeName);
}
