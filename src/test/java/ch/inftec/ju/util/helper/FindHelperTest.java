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
	private List<Integer> empty = Collections.<Integer>emptyList();
	private List<Integer> one = JuCollectionUtils.asArrayList(1);
	private List<Integer> two = JuCollectionUtils.asArrayList(1, 2);
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void one_throwsException_forEmpty() {
		this.exception.expectMessage("No element available");
		
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().collection(this.empty).createFindHelper();
		findHelper.one();
	}
	
	@Test
	public void one_throwsException_forTwo() {
		this.exception.expectMessage("Expected no more than 1 item");
		
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().collection(this.two).createFindHelper();
		findHelper.one();
	}
	
	@Test
	public void one_findsOne() {
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().collection(this.one).createFindHelper();
		Assert.assertEquals(new Integer(1), findHelper.one());
	}
	
	@Test
	public void oneOrNull_findsOne() {
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().collection(this.one).createFindHelper();
		Assert.assertEquals(new Integer(1), findHelper.oneOrNull());
	}
	
	@Test
	public void oneOrNull_returnsNull_forEmpty() {
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().collection(this.empty).createFindHelper();
		Assert.assertNull(findHelper.oneOrNull());
	}
	
	@Test
	public void all_findsAll() {
		FindHelper<Integer> findHelper = new FindHelperBuilder<Integer>().collection(this.two).createFindHelper();
		TestUtils.assertCollectionConsistsOfAll(findHelper.all(), new Integer(1), new Integer(2));
	}
}
