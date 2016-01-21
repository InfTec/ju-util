package ch.inftec.ju.util;


import java.util.Date;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JuStringUtilsTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void canCreate_hexString() {
		byte b[] = new byte[] { 1, 2, 127 };
		Assert.assertEquals("01027f", JuStringUtils.toHexString(b));
	}
	
	@Test
	public void canCreate_md5Checksum() {
		String md5 = JuStringUtils.getMd5Checksum("String");
		Assert.assertEquals("27118326006d3829667a400ad23d5d98", md5);
	}

	@Test
	public void canConvert_iso8601String_toDate_date() throws Exception {
		Date expDate = JuStringUtils.DATE_FORMAT_DAYS.parse("01.01.1970");
		Assert.assertEquals(expDate.getTime(), JuStringUtils.parseIso8601Date("1970-01-01").getTime());
	}

	@Test
	public void canConvert_iso8601String_toDate_timeZone() {
		Assert.assertEquals(0, JuStringUtils.parseIso8601Date("1970-01-01T00:00:00+00:00").getTime());
	}

	@Test
	public void canConvert_iso8601String_toDate_zulu() {
		Assert.assertEquals(0, JuStringUtils.parseIso8601Date("1970-01-01T00:00:00Z").getTime());
	}

	@Test
	public void iso8601String_toDate_throwsException_onInvalidDate() {
		this.thrown.expect(JuRuntimeException.class);
		this.thrown.expectMessage("Not a ISO 8601 compliant date string: abc");

		JuStringUtils.parseIso8601Date("abc");
	}
}
