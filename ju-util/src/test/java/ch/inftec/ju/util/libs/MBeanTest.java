package ch.inftec.ju.util.libs;

import java.lang.management.ManagementFactory;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.Test;

public class MBeanTest {
	@Test
	public void canGet_platformMBeanServer() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		Assert.assertNotNull(mbs);
		Assert.assertTrue(mbs.getMBeanCount() > 0);
	}
	
	@Test
	public void canGet_classLoadingMBean() throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		MBeanInfo mbi = mbs.getMBeanInfo(new ObjectName("java.lang", "type", "ClassLoading"));
		Assert.assertTrue(mbi.getAttributes().length > 0);
	}
	
	@Test
	public void canGet_totalLoadedClassCount() throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		Object obj = mbs.getAttribute(new ObjectName("java.lang:type=ClassLoading"), "TotalLoadedClassCount");
		Long l = (Long) obj;
		Assert.assertTrue(l > 0);
	}
}
