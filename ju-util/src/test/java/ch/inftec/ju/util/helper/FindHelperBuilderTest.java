package ch.inftec.ju.util.helper;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import ch.inftec.ju.util.JuCollectionUtils;
import ch.inftec.ju.util.function.Function;

public class FindHelperBuilderTest {
	@Test
	public void iterableTransformed_returnsFindHelperWithTransformation() {
		List<Integer> src = JuCollectionUtils.asArrayList(1);

		FindHelper<String> findHelper = new FindHelperBuilder<String>()
				.iterableTransformed(src, new Function<Integer, String>() {
					@Override
					public String apply(Integer integer) {
						return integer.toString();
					}
				}).createFindHelper();

		assertEquals("1", findHelper.one());

		List<String> res = JuCollectionUtils.asArrayList("1");
		assertTrue(JuCollectionUtils.collectionEquals(res, findHelper.all()));
	}
}
