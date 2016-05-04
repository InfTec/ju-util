package ch.inftec.ju.util.helper;

import java.util.Date;

import ch.inftec.ju.util.ConversionUtils;
import ch.inftec.ju.util.JuRuntimeException;
import ch.inftec.ju.util.JuStringUtils;

/**
 * Factory to create ValueConverter instances.
 * @author martin.meyer@inftec.ch
 *
 */
public class ValueConverterFactory {
	/**
	 * Creates a new ValueConverter for the specified object.
	 * <p>
	 * The ValueConverter supports the following conversions:
	 * <ul>
	 * <li>Integer</li>
	 * <li>Long</li>
	 * <li>Boolean</li>
	 * <li>String</li>
	 * <li>Date in ISO 8601 format</li>
	 * </ul>
	 * 
	 * @param obj
	 *            Object to be converted by the converter
	 * @return ValueConverter instance
	 */
	public static ValueConverter createNewValueConverter(Object obj) {
		return new ValueConverterImpl(obj);
	}
	
	private static class ValueConverterImpl implements ValueConverter {
		private final Object obj;
		
		private ValueConverterImpl(Object obj) {
			this.obj = obj;
		}

		@Override
		public Object get() {
			return this.obj;
		}

		@Override
		public <T> T get(Class<T> clazz) {
			return this.convert(clazz);
		}
		
		@SuppressWarnings("unchecked")
		private <T> T convert(Class<T> clazz) {
			Object obj = this.get();
			
			if (obj == null) {
				return null;
			} else {
				// First, try if we can cast the value
				if (obj.getClass() == clazz) {
					return (T) obj;
				} else {
					// Do conversions
					
					if (clazz == Integer.class) {
						Long l = ConversionUtils.toLong(obj);
						return (T) new Integer(l.intValue());
					} else if (clazz == Long.class) {
						Long l = ConversionUtils.toLong(obj);
						return (T) l;
					} else if (clazz == Boolean.class) {
						return (T) new Boolean(obj.toString());
					} else if (clazz == String.class) {
						return (T) obj.toString();
					} else if (clazz == Date.class) {
						return (T) JuStringUtils.parseIso8601Date(obj.toString());
					} else {
						throw new JuRuntimeException("Conversion not supported: " + clazz);
					}
				}
			}
		}
	}
}
