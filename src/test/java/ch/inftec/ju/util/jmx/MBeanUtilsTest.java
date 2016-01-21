package ch.inftec.ju.util.jmx;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.inftec.ju.util.JuRuntimeException;

public class MBeanUtilsTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void canQueryMBeanAttribute_fromPlatformMBeanServer() {
		MBeanUtil mbu = MBeanUtils.queryPlatformMBeanServer("java.lang:type=ClassLoading");
		Long val = mbu.getAttribute("TotalLoadedClassCount").get(Long.class);
		Assert.assertTrue(val > 0);
	}
	
	@Test
	public void invalidObjectName_throwException() {
		thrown.expect(JuRuntimeException.class);
		thrown.expectMessage("ObjectName");
		
		MBeanUtils.queryPlatformMBeanServer("bla");
	}
}
