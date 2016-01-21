package ch.inftec.ju.util.libs;

import java.util.Properties;

import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test Plexus interpolation.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 */
public class PlexusInterpolationTest {
	@Test
	public void testInterpolation_withoutSource() throws Exception {
		String s = "Hello ${name}";
		
		Interpolator i = new RegexBasedInterpolator();
		Assert.assertEquals("Hello ${name}", i.interpolate(s));
	}
	
	@Test
	public void testInterpolation_withPropertiesSource() throws Exception {
		String s = "Hello ${name}";
		
		Properties props = new Properties();
		props.put("name", "World");
		ValueSource vs = new PropertiesBasedValueSource(props);
		
		Interpolator i = new RegexBasedInterpolator();
		i.addValueSource(vs);
		Assert.assertEquals("Hello World", i.interpolate(s));
	}
	
	@Test
	public void testInterpolation_withCustomValueSource() throws Exception {
		String s = "Hello ${name}";
		
		ValueSource vs = new AbstractValueSource(false) {
			@Override
			public Object getValue(String expression) {
				return "World";
			}
		};
		
		Interpolator i = new RegexBasedInterpolator();
		i.addValueSource(vs);
		Assert.assertEquals("Hello World", i.interpolate(s));
	}
}
