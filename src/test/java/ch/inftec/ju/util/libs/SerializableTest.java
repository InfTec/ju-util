package ch.inftec.ju.util.libs;

import java.io.Serializable;

import org.junit.Assert;
import org.junit.Test;

import ch.inftec.ju.util.IOUtil;

/**
 * Contains test for serializing Java classes
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 */
@SuppressWarnings("unused")
public class SerializableTest {
	@Test
	public void canSerialize_staticInnerClass_withNullReference() {
		StaticInnerClass c = new StaticInnerClass(null);
		
		Assert.assertNull(IOUtil.isSerializableOrException(c));
	}
	
	@Test
	public void canNotSerialize_staticInnerClass_withNonNullReference() {
		StaticInnerClass c = new StaticInnerClass(new InnerClass());
		
		Assert.assertFalse(IOUtil.isSerializable(c));
	}
	
	/**
	 * Transient variables won't get serialized...
	 */
	@Test
	public void canSerialize_staticInnerClass_withTransientReference() {
		StaticInnerClassTransient c = new StaticInnerClassTransient(new InnerClass());
		
		Assert.assertNull(IOUtil.isSerializableOrException(c));
	}
	
	@Test
	public void canNotSerialize_innerClass() {
		InnerClass c = new InnerClass();
		
		Assert.assertFalse(IOUtil.isSerializable(c));
	}
	
	@Test
	public void canSerialize_staticInnerClass_withInnerClassReference() {
		StaticInnerClass2 c = new StaticInnerClass2();
		
		Assert.assertTrue(IOUtil.isSerializable(c));
	}
	
	@Test
	public void canSerialize_staticInnerClass_withStaticInnerClassCrossReference() {
		StaticInnerClass3 c = new StaticInnerClass3();
		
		Assert.assertTrue(IOUtil.isSerializable(c));
	}
	
	private static class StaticInnerClass implements Serializable {
		private InnerClass value;
		
		private StaticInnerClass(InnerClass value) {
			this.value = value;
		}
	}
	
	private static class StaticInnerClassTransient implements Serializable {
		private transient InnerClass value;
		
		private StaticInnerClassTransient(InnerClass value) {
			this.value = value;
		}
	}
	
	private class InnerClass implements Serializable {
	}
	
	private static class StaticInnerClass2 implements Serializable {
		private InnerInnerClass innerClass = new InnerInnerClass();
		
		private class InnerInnerClass implements Serializable{
		}
	}
	
	private static class StaticInnerClass3 implements Serializable {
		private StaticInnerInnerClass innerClass = new StaticInnerInnerClass(this);
		
		private class StaticInnerInnerClass implements Serializable {
			private StaticInnerClass3 staticInnerClass;
			
			public StaticInnerInnerClass(StaticInnerClass3 staticInnerClass) {
				this.staticInnerClass = staticInnerClass;
			}
		}
	}
	
	private static class NonSerializableClass {
	}
}
