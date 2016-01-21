package ch.inftec.ju.util.libs;

import org.joda.time.DateMidnight;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the Yoda Time framework.
 * @author Martin
 *
 */
public class JodaTimeTest {
	@Test
	public void core() {
		LocalDate lt1 = new LocalDate(2000, 1, 1);
		String s1 = lt1.toString();
		Assert.assertEquals("2000-01-01", s1);
		
		DateTimeFormatter fmt = new DateTimeFormatterBuilder()
			.appendLiteral("Year: ")
			.appendYear(4, 4)
			.toFormatter();// minDigits, maxDigits
		Assert.assertEquals("Year: 2000", lt1.toString(fmt));
	}
	
	@Test
	public void timeZones() {
		LocalDate lt = new LocalDate(2000, 1, 1);
		DateMidnight dm = lt.toDateMidnight(DateTimeZone.UTC);
		
		Assert.assertEquals("2000-01-01T00:00:00.000Z", dm.toString());
	}
}
