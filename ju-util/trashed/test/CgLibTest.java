package ch.inftec.ju.util.libs;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.junit.Assert;
import org.junit.Test;

public class CgLibTest {
	@Test
	public void createBean() {
		Enhancer e = new Enhancer();
		e.setSuperclass(MyClass.class);
		e.setCallback(new MethodInterceptor() {
			@Override
			public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
				return method.getName();
			}
		});
		
		MyClass mc = (MyClass) e.create();
		Assert.assertEquals("getValue", mc.getValue());
	}
	
	public static class MyClass {
		public String getValue() {
			return "foo";
		}
	}
}
