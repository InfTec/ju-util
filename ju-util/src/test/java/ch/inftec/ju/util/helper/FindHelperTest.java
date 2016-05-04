package ch.inftec.ju.util.helper;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.inftec.ju.util.JuCollectionUtils;
import ch.inftec.ju.util.TestUtils;

public class FindHelperTest {
	private List<Integer> empty = Collections.emptyList();
	private List<Integer> one = JuCollectionUtils.asArrayList(1);
	private List<Integer> two = JuCollectionUtils.asArrayList(1, 2);
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void one_throwsException_forEmpty() {
		this.exception.expectMessage("No element available");
		
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().iterable(this.empty).createFindHelper();
		findHelper.one();
	}
	
	@Test
	public void one_throwsException_forTwo() {
		this.exception.expectMessage("More than 1 item available");
		
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().iterable(this.two).createFindHelper();
		findHelper.one();
	}
	
	@Test
	public void one_findsOne() {
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().iterable(this.one).createFindHelper();
		Assert.assertEquals(new Integer(1), findHelper.one());
	}
	
	@Test
	public void oneOrNull_findsOne() {
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().iterable(this.one).createFindHelper();
		Assert.assertEquals(new Integer(1), findHelper.oneOrNull());
	}
	
	@Test
	public void oneOrNull_returnsNull_forEmpty() {
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().iterable(this.empty).createFindHelper();
		Assert.assertNull(findHelper.oneOrNull());
	}
	
	@Test
	public void all_findsAll() {
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().iterable(this.two).createFindHelper();
		TestUtils.assertCollectionConsistsOfAll(findHelper.all(), 1, 2);
	}
}
