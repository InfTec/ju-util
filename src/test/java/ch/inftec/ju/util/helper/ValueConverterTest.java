package ch.inftec.ju.util.helper;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import ch.inftec.ju.util.JuStringUtils;

public class ValueConverterTest {
	@Test
	public void returnsObject() {
		String s = "test";
		Assert.assertSame(s, ValueConverterFactory.createNewValueConverter(s).get());
	}
	
	@Test
	public void convertsObject_toString() {
		Long l = 3L;
		Assert.assertEquals("3", ValueConverterFactory.createNewValueConverter(l).get(String.class));
	}
	
	@Test
	public void convertsNull_toNull() {
		Long l = null;
		Assert.assertNull(ValueConverterFactory.createNewValueConverter(l).get(String.class));
	}

	@Test
	public void convertsStrings_inIso8601_toDate_shortForm() throws Exception {
		String date = "2014-07-28";
		Date convDate = ValueConverterFactory.createNewValueConverter(date).get(Date.class);

		Assert.assertEquals(JuStringUtils.DATE_FORMAT_DAYS.parse("28.07.2014"), convDate);
	}

	@Test
	public void convertsNullString_toNull_usingDate() {
		Assert.assertNull(ValueConverterFactory.createNewValueConverter(null).get(Date.class));
	}
}
