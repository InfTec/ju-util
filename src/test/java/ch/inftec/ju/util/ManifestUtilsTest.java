package ch.inftec.ju.util;

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.inftec.ju.util.ManifestUtils.ManifestFinder.ManifestWrapper;

public class ManifestUtilsTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void findsManifest_byAttribute() {
		String vendorId = ManifestUtils.find()
				.byAttribute("Implementation-Title", "slf4j-api")
			.find().one()
				.getValue("Bundle-Vendor");
		
		Assert.assertEquals("SLF4J.ORG", vendorId);
	}
	
	@Test
	public void returnsNull_forMissingEntry() {
		String res = ManifestUtils.find()
				.byAttribute("Implementation-Title", "slf4j-api")
			.find().one()
				.getValue("BliBla");
		
		Assert.assertNull(res);
	}
	
	@Test
	public void findOne_throwsException_forMultipleResults() {
		this.exception.expect(JuRuntimeException.class);
		this.exception.expectMessage("Expected no more than 1 item");
		
		ManifestUtils.find()
				.byAttribute("Manifest-Version", "1\\.0")
			.find().one();
	}
	
	@Test
	public void canFind_multipleManifests() {
		List<? extends ManifestWrapper> manifests = ManifestUtils.find()
				.byAttribute("Manifest-Version", "1\\.0")
			.find().all();
		
		Assert.assertTrue(manifests.size() > 1);
	}
	
	@Test
	public void canReadInfo_fromJarManifest() {
		String bundleName = ManifestUtils.find()
				.byJar("slf4j-log4j12.*")
			.find().one()
				.getValue("Bundle-Name");
		
		Assert.assertEquals("slf4j-log4j12", bundleName);
	}
	
	@Test
	public void returnsNull_forMissingManifest() {
		String res = ManifestUtils.find()
				.byJar("someJar")
			.find().oneOrNone()
				.getValue("Bundle-Name");
		
		Assert.assertNull(res);
	}
}
