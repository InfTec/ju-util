package ch.inftec.ju.util;

import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import ch.inftec.ju.util.helper.FindHelperBuilder;
import ch.inftec.ju.util.helper.FindNoneHelper;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Helper class to find and read manifest files (<code>META-INF/MANIFEST.MF</code>).
 * <p>
 * This class will not perform any caching of manifests, but search the whole classpath everytime
 * it is used.
 * @author Martin Meyer <martin.meyer@inftec.ch>
 *
 */
public class ManifestUtils {
	private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";
	
	/**
	 * Returns a finder to locate manifest files.
	 * @return ManifestFinder
	 */
	public static ManifestFinder find() {
		return new ManifestFinder();
	}
	
	/**
	 * Helper class to find manifest files.
	 * @author Martin Meyer <martin.meyer@inftec.ch>
	 *
	 */
	public static final class ManifestFinder {
		private Map<URL, Manifest> manifests = new LinkedHashMap<>();

		private ManifestFinder() {
			// Find manifests...
			List<URL> manifests = JuUrl.resource().getAll(ManifestUtils.MANIFEST_PATH);
			
			// Load manifests...
			try {
				for (URL manifestUrl : manifests) {
					Manifest manifest = IOUtil.loadManifestFromUrl(manifestUrl);
					this.manifests.put(manifestUrl, manifest);
				}
			} catch (Exception ex) {
				throw new JuRuntimeException("Couldn't load manifests", ex);
			}
		}
		
		/**
		 * Looks for manifests that contain the specified attribute, matching the
		 * regular expression.
		 * @param attributeName Attribute name
		 * @param valueRegex Regular expression matching the value
		 * @return Finder to chain
		 */
		public ManifestFinder byAttribute(final String attributeName, String valueRegex) {
			final RegexUtil ru = new RegexUtil(valueRegex);
			
			this.filterManifests(new Predicate<Entry<URL, Manifest>>() {
				@Override
				public boolean apply(Entry<URL, Manifest> e) {
					Attributes attrs = e.getValue().getMainAttributes();
					String val = attrs.getValue(attributeName);
					return ru.matches(val);
				}
			});
			
			return this;
		}
		
		/**
		 * Finds manifests that are defined in the specified JAR.
		 * @param jarFileRegex Regex matching JAR
		 * @return Finder to chain
		 */
		public ManifestFinder byJar(String jarFileRegex) {
			final RegexUtil ru = new RegexUtil(jarFileRegex);
			final RegexUtil jarNamePattern = new RegexUtil(".*/([^/]*.\\.jar)!.*");
			
			this.filterManifests(new Predicate<Entry<URL, Manifest>>() {
				@Override
				public boolean apply(Entry<URL, Manifest> e) {
					URL url = e.getKey();
					
					if ("jar".equals(url.getProtocol())) {
						String path = url.getPath();
						if (jarNamePattern.matches(path)) {
							String jarName = jarNamePattern.getMatches(path)[0].getGroups()[0];
							if (ru.matches(jarName)) {
								return true;
							}
						}
					}
					return false;
				}
			});
			
			
			return this;
		}
		
		private void filterManifests(Predicate<Entry<URL, Manifest>> keep) {
			for (Iterator<Entry<URL, Manifest>> iterator = this.manifests.entrySet().iterator(); iterator.hasNext(); ) {
				if (!keep.apply(iterator.next())) {
					iterator.remove();
				}
			}
		}
		
		/**
		 * Returns a FindNoneHelper to retrieve the manifests found.
		 * @return FindNoneHelper instance
		 */
		public FindNoneHelper<ManifestWrapper> find() {
			return new FindHelperBuilder<ManifestWrapper>()
				.collectionTransformed(this.manifests.values(), new Function<Manifest, ManifestWrapper>() {
						@Override
						public ManifestWrapper apply(Manifest m) {
							return new ManifestWrapper(m);
						}
					})
				.noneObject(new ManifestWrapper(null) {
						@Override
						public String getValue(String attributeName) {
							return null;
						}
					})
				.createFindNoneHelper();
		}
		
		/**
		 * Wrapper class to access Manifest information
		 * @author Martin Meyer <martin.meyer@inftec.ch>
		 *
		 */
		public static class ManifestWrapper {
			private final Manifest manifest;
			
			public ManifestWrapper(Manifest manifest) {
				this.manifest = manifest;
			}
			
			/**
			 * Gets the value of the specified attribute.
			 * @param attributeName Attribute name
			 * @return Value of the attribute or null if it isn't defined
			 */
			public String getValue(String attributeName) {
				Attributes attrs = this.manifest.getMainAttributes();
				return attrs.getValue(attributeName);
			}
		}
	}
}
