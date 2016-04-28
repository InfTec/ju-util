package ch.inftec.ju.util.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import ch.inftec.ju.util.JuRuntimeException;
import ch.inftec.ju.util.helper.ValueConverter;
import ch.inftec.ju.util.helper.ValueConverterFactory;

/**
 * Utility class to deal with MBeans.
 * @author martin.meyer@inftec.ch
 *
 */
public class MBeanUtils {
	/**
	 * Queries an MBean using the PlatformMBeanServer.
	 * @param objectName Name of the object, e.g. 'java.lang:type=ClassLoading'
	 * @return MBeanUtil instance to access the MBean
	 */
	public static MBeanUtil queryPlatformMBeanServer(String objectName) {
		return new MBeanUtilImpl(ManagementFactory.getPlatformMBeanServer(), objectName);
	}
	
	private static final class MBeanUtilImpl implements MBeanUtil {
		private final MBeanServer server;
		private final ObjectName objectName;
		
		private MBeanUtilImpl(MBeanServer server, String objectName) {
			this.server = server;
			
			try {
				this.objectName = new ObjectName(objectName);
			} catch (Exception ex) {
				throw new JuRuntimeException("Couldn't create ObjectName for %s", ex, objectName);
			}
		}
		
		@Override
		public ValueConverter getAttribute(String attributeName) {
			try {
				return ValueConverterFactory.createNewValueConverter(this.server.getAttribute(this.objectName, attributeName));
			} catch (Exception ex) {
				throw new JuRuntimeException("Couldn't get attribute %s from MBean object %s", ex, attributeName, this.objectName);
			}
		}
	}
}
