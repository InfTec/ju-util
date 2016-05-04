package ch.inftec.ju.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jasypt.util.digest.Digester;

/**
 * Class containing String related utility methods.
 * @author tgdmemae
 *
 */
public final class JuStringUtils {
	/**
	 * Don't instantiate.
	 */
	private JuStringUtils() {
		throw new AssertionError("use only statically");
	}
	
	/**
	 * Newline character.
	 */
	public static final String NEW_LINE = "\n";
	
	/**
	 * Line feed character (equals NEW_LINE).
	 */
	public static final String LF = JuStringUtils.NEW_LINE;
	
	/**
	 * Carriage return / line feed combination.
	 */
	public static final String CRLF = "\r\n";
	
	/**
	 * Date of format dd.MM.yyyy.
	 */
	public final static SimpleDateFormat DATE_FORMAT_DAYS = new SimpleDateFormat("dd.MM.yyyy");
	
	/**
	 * Date of format dd.MM.yyyy HH:mm
	 */
	public final static SimpleDateFormat DATE_FORMAT_HOURS = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	
	/**
	 * Date of format dd.MM.yyyy HH:mm:ss
	 */
	public static final SimpleDateFormat DATE_FORMAT_SECONDS = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	
	/**
	 * Date of format yyyyMMdd_HHmmss
	 */
	public static final SimpleDateFormat TIMESTAMP_FORMAT_SECONDS = new SimpleDateFormat("yyyyMMdd_HHmmss");
	
	/**
	 * Zulu date format, as used by XML: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
	 */
	private static final SimpleDateFormat DATE_FORMAT_ZULU = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
	/**
	 * Converts the specified String to a date using the SipleDateFormat provided.<br>
	 * Use the static date formats of the JuStringUtils class.
	 * @param s String
	 * @param dateFormat SimpleDateFormat to use
	 * @return Date
	 * @throws ParseException If the string cannot be parsed
	 */
	public static Date toDate(String s, SimpleDateFormat dateFormat) throws ParseException {
		return dateFormat.parse(s);
	}
	
	/**
	 * Converts the specified Date (in local TimeZone) to a Zulu time string as used
	 * in XML.
	 * @param date Date
	 * @return Zulu date string
	 */
	public static String toZuluDateString(Date date) {
		int offset = TimeZone.getDefault().getOffset(date.getTime());
		
		Date zuluDate = new Date(date.getTime() - offset);
		return JuStringUtils.DATE_FORMAT_ZULU.format(zuluDate);
	}
	
	/**
	 * Perses an ISO 8601 Date String and returns the corresponding Date instance.
	 * <p>
	 * ISO 8601 allows the following formats:
	 * <ul>
	 * <li>Date: 2014-07-28
	 * <li>Date and time in UTC: 2014-07-28T06:07:34+00:00
	 * <li>Date and time in UTC, Zulu: 2014-07-28T06:07:34Z
	 * </ul>
	 * <p>
	 * Note that when we only specify the date, the system time zone will be applied.
	 * 
	 * @param iso8601Date
	 *            Date String in ISO 8601 format
	 * @return Date instance
	 */
	public static Date parseIso8601Date(String iso8601Date) {
		try {
			return javax.xml.bind.DatatypeConverter.parseDateTime(iso8601Date).getTime();
		} catch (Exception ex) {
			throw new JuRuntimeException("Not a ISO 8601 compliant date string: " + iso8601Date);
		}
	}
	
	/**
	 * Crops the String to the length specified. If the String
	 * is shorter, it is just returned.
	 * @param s String
	 * @param maxLength Maximum length of the String
	 * @return Cropped String
	 */
	public static String crop(String s, int maxLength) {
		if (s == null || s.length() <= maxLength) return s;
		return s.substring(0, Math.max(0, maxLength));
	}
	
	/**
	 * Removes all non-alphabetical leading characters from the String.
	 * <p>
	 * E.g. _01_test -> test
	 * @return
	 */
	public static String removeNonAlphabeticalLeadingCharacters(String s) {
		if (StringUtils.isEmpty(s)) {
			return s;
		} else {
			int i = new RegexUtil("[a-zA-Z]").getFirstMatchIndex(s);
			
			if (i < 0) {
				return "";
			} else {
				return s.substring(i);
			}
		}
	}
	
	/**
	 * Repeats the String s n times and returns that newly built string.
	 * @param s String
	 * @param n Repetitions
	 * @return n times s
	 */
	public static String times(String s, int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) sb.append(s);
		
		return sb.toString();
	}
	
	/**
	 * Replaces all occurrences of %key% as specified in the replacements
	 * parameters (as key, value pairs) with the specified value.
	 * @param s String to be replaced
	 * @param replacements key, value replacement pairs
	 * @return String with replaced keys. If keys in the source string exist that
	 * were not specified in the replacements, they are left untouched
	 */
	public static String replaceAll(String s, String... replacements) {
		if (replacements.length % 2 != 0) {
			throw new IllegalArgumentException("replacements parameter must consist of 0-n key-value pairs");
		}
		
		if (replacements.length == 0 || s == null) return s;
		
		for (int i = 0; i < replacements.length; i += 2) {
			String key = replacements[i];
			String value = replacements[i + 1];
			
			s = s.replaceAll("%" + key + "%", Matcher.quoteReplacement(value)); // We don't want special handling of regex signs in replacement string
		}
		
		return s;
	}
	
	/**
	 * Counts how many times the substring occurrs in the specified String (without overlapping).
	 * If either string is null, 0 is returned.
	 * @param s String
	 * @param substring Substring
	 * @return Number of occurrences of the substring in the string
	 */
	public static int occurrancies(String s, String substring) {
		if (s == null || substring == null) return 0;
		
		int cnt = 0;
		int index = -1;
		while ((index = s.indexOf(substring, index + 1)) >= 0) {
			cnt++;
		}
		
		return cnt;
	}
	
	/**
	 * Checks if the specified String contains at least one whitespace character. Empty strings
	 * and null are not considered to contain whitespace.
	 * @param s String
	 * @return True if the String contains whitespace
	 */
	public static boolean containsWhitespace(String s) {
		return s != null && s.length() > 0 && new RegexUtil(RegexUtil.WHITESPACE).containsMatch(s);
	}
	
	/**
	 * Creates a toString String for the specified object and the list
	 * of key (String) value (Object) pairs. Uses the ToStringBuilder of the
	 * Apache commons library.
	 * @param obj Object to create toString for
	 * @return String representation of the object, containing key value infos
	 */
	public static String toString(Object obj, Object... keyValuePairs) {
		ToStringBuilder b = new ToStringBuilder(obj, ToStringStyle.SHORT_PREFIX_STYLE);
		
		for (int i = 1; i < keyValuePairs.length; i+= 2) {
			b.append(ObjectUtils.toString(keyValuePairs[i-1]), keyValuePairs[i]);
		}
		
		return b.toString();
	}
	
	/**
	 * Splits the specified String by the dividingString. Example: Split 'a,b,c' by 'c'
	 * returns ['a', 'b', 'c'].
	 * @param s String to split
	 * @param dividingString Dividing string
	 * @param trim If true, 
	 * @return
	 */
	public static String[] split(String s, String dividingString, boolean trim) {
		if (StringUtils.isEmpty(s)) {
			return new String[0];
		} else {
			String whitespaceRegex = trim ? String.format("[%s]*", RegexUtil.WHITESPACE) : "";
			String regex = whitespaceRegex + Pattern.quote(dividingString) + whitespaceRegex;
			
			String res[] = s.split(regex);

			if (trim) {
				// We still need to manually trim the first and the last element
				if (res.length > 0) res[0] = res[0].trim();
				if (res.length > 1) res[res.length - 1] = res[res.length - 1].trim();
			}
			
			return res;
		}
	}
	
	/**
	 * Gets the stack trace of the specified Throwable as a String
	 * @param t Throwable
	 * @return Stacktrace
	 */
	public static String getStackTrace(Throwable t) {
		try (StringWriter w = new StringWriter()) {
			PrintWriter pw = new PrintWriter(w);
			t.printStackTrace(pw);
			return w.toString();
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't get stacktrace", ex);
		}
	}
	
	/**
	 * Gets the 128 bit MD5 checksum for the specified String as a Hex String (consisting of 32 characters).
	 * @param s String
	 * @Return MD5 checksum of the specified String as a Hex String
	 */
	public static String getMd5Checksum(String s) {
		Digester digester = new Digester();
		return JuStringUtils.toHexString(digester.digest(s.getBytes()));
	}
	
	public static String toHexString(byte[] b) {
		StringBuffer sb = new StringBuffer();
        for (int j = 0; j < b.length; ++j) {
        	sb.append(Integer.toHexString((b[j] & 0xFF) | 0x100).substring(1,3));
        }
		return sb.toString();
	}
	
	/**
	 * Gets a creator to create sample Lorem Ipsum strings that can be used
	 * as test data.
	 * <p>
	 * Copied from de.svenjacobs.loremipsum
	 * @return LoremIpsumCreator
	 */
	public static LoremIpsumCreator createLoremIpsum() {
		return new LoremIpsumCreator();
	}
	
	/**
	 * Helper class to create Lorem Ipsum strings.
	 * @author Martin
	 *
	 */
	public static class LoremIpsumCreator {
		public static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";
		private String[] loremIpsumWords;

		public LoremIpsumCreator() {
			this.loremIpsumWords = LOREM_IPSUM.split("\\s");
		}

		/**
		 * Returns one sentence (50 words) of the lorem ipsum text.
		 * 
		 * @return 50 words of lorem ipsum text
		 */
		public String getWords() {
			return getWords(50);
		}

		/**
		 * Returns words from the lorem ipsum text.
		 * 
		 * @param amount
		 *            Amount of words
		 * @return Lorem ipsum text
		 */
		public String getWords(int amount) {
			return getWords(amount, 0);
		}

		/**
		 * Returns words from the lorem ipsum text.
		 * 
		 * @param amount
		 *            Amount of words
		 * @param startIndex
		 *            Start index of word to begin with (must be >= 0 and < 50)
		 * @return Lorem ipsum text
		 * @throws IndexOutOfBoundsException
		 *             If startIndex is < 0 or > 49
		 */
		public String getWords(int amount, int startIndex) {
			if (startIndex < 0 || startIndex > 49) {
				throw new IndexOutOfBoundsException(
						"startIndex must be >= 0 and < 50");
			}

			int word = startIndex;
			StringBuilder lorem = new StringBuilder();

			for (int i = 0; i < amount; i++) {
				if (word == 50) {
					word = 0;
				}

				lorem.append(loremIpsumWords[word]);

				if (i < amount - 1) {
					lorem.append(' ');
				}

				word++;
			}

			return lorem.toString();
		}

		/**
		 * Returns two paragraphs of lorem ipsum.
		 * 
		 * @return Lorem ipsum paragraphs
		 */
		public String getParagraphs() {
			return getParagraphs(2);
		}

		/**
		 * Returns paragraphs of lorem ipsum.
		 * 
		 * @param amount
		 *            Amount of paragraphs
		 * @return Lorem ipsum paragraphs
		 */
		public String getParagraphs(int amount) {
			StringBuilder lorem = new StringBuilder();

			for (int i = 0; i < amount; i++) {
				lorem.append(LOREM_IPSUM);

				if (i < amount - 1) {
					lorem.append("\n\n");
				}
			}

			return lorem.toString();
		}
	}
}
