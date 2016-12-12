package ch.inftec.ju.util;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.inftec.ju.util.RegexUtil.Match;

public class JuUrl {
	private static Logger logger = LoggerFactory.getLogger(JuUrl.class);
	
	/**
	 * Gets the resource with the specified name, using the ClassLoader to
	 * resolve the resource.
	 * @param resourceName Absolute name, not starting with a '/'
	 * @return (First) found resource as URL or null if none was found
	 */
	public static URL resource(String resourceName) {
		return JuUrl.resource().get(resourceName);
	}
	
	/**
	 * Gets a resource URL relative to the specified class.
	 * @param resourceName Resource name
	 * @param relativeClass Class used to resolve the resource
	 * @return (First) found resource as URL or null if none was found
	 */
	public static URL resourceRelativeTo(String resourceName, Class<?> relativeClass) {
		return JuUrl.resource().relativeTo(relativeClass).get(resourceName);
	}
	
	/**
	 * Gets an existing resource relative to a specified class, throwing an exception if no resource
	 * is found.
	 * @param resourceName Resource name
	 * @param relativeClass Class used to resolve the resource
	 * @return (First) found resource as URL
	 */
	public static URL existingResourceRelativeTo(String resourceName, Class<?> relativeClass) {
		return JuUrl.resource().relativeTo(relativeClass).exceptionIfNone().get(resourceName);
	}
	
	/**
	 * Gets an existing resource relative to a specified class, throwing an exception if no resource is found.
	 * <p>
	 * The resourceName will be automatically prefixed with the relative classes simple name. Example:
	 * <ul>
	 * <li>resourceName: test.txt</li>
	 * <li>relativeClass: com.test.TestClass</li>
	 * <li>Actual resource name: <strong>TestClass_test.txt</strong> in directory com/test.</li>
	 * </ul>
	 * 
	 * @param resourceName
	 * @param relativeClass
	 * @return
	 */
	public static URL existingResourceRelativeToAndPrefixed(String resourceName, Class<?> relativeClass) {
		String actualResourceName = String.format("%s_%s", relativeClass.getSimpleName(), resourceName);

		return existingResourceRelativeTo(actualResourceName, relativeClass);
	}

	/**
	 * Gets the resource with the specified name, making sure that there is only one resource with the name
	 * and that it exists.
	 * 
	 * @param resourceName
	 *            Resource name
	 * @return URL to resource
	 * @throws JuRuntimeException
	 *             If resource doesn't exist
	 */
	public static URL singleResource(String resourceName) {
		return JuUrl.resource().single().exceptionIfNone().get(resourceName);
	}
	
	/**
	 * Converts the specified path to an URL, wrapping any exception into a
	 * runtime exception.
	 * @param p Path
	 * @return Path as URL
	 */
	public static URL toUrl(Path p) {
		try {
			return p.toUri().toURL();
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't convert path '%s' to URL", ex, p);
		}
	}
	
	/**
	 * Converts the specified URL to a Path instance.
	 * @param url URL
	 * @return Path instance
	 */
	public static Path toPath(URL url) {
		try {
			return Paths.get(url.toURI());
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't convert URL %s to Path", ex, url);
		}
	}
	
	/**
	 * Gets a Path instance to an existing file.
	 * @param path Path to file
	 * @return Path instance
	 * @throws JuRuntimeException If the file doesn't exist
	 */
	public static Path existingFile(String path) {
		return JuUrl.path().file().exists().get(path);
	}
		
	/**
	 * Gets a Path instance to an existing folder.
	 * @param path Path to folder
	 * @return Path instance
	 * @throws JuRuntimeException If the folder doesn't exist
	 */
	public static Path existingFolder(String path) {
		return JuUrl.path().directory().exists().get(path);
	}
	
	/**
	 * Gets an URL to an existing resource. If none is found, an exception will be
	 * thrown.
	 * <p>
	 * If multiple resources with the specified name/path exist, the first found is returned.
	 * @param resourceName Resource name
	 * @return URL to resource
	 */
	public static URL existingResource(String resourceName) {
		return JuUrl.resource().exceptionIfNone().get(resourceName);
	}
	
	/**
	 *Gets a PathUrlBuilder that can be used to configure and perform path lookup.
	 * @return PathUrlBuilder instance
	 */
	public static PathUrlBuilder path() {
		return new PathUrlBuilder();
	}
	
	/**
	 * Helper class to resolve paths.
	 * @author Martin
	 *
	 */
	public static class PathUrlBuilder {
		private boolean file;
		private boolean directory;
		private boolean exists;
		private String relativeToFirst;
		private String[] relativeToMore = new String[0];
		
		/**
		 * Sets the flag that the path must be a file.
		 * <p>
		 * Default is false which means that it needn't be a file (but CAN be).
		 * @return This builder
		 */
		public PathUrlBuilder file() {
			this.file = true;
			return this;
		}
		
		/**
		 * Sets the flag that the path must be a directory.
		 * <p>
		 * Defaults to false which means that it needn't be a directory (but CAN be).
		 * @return This builder
		 */
		public PathUrlBuilder directory() {
			this.directory = true;
			return this;
		}
		
		/**
		 * Sets the modified that the path must exist. If not, an exception will be thrown.
		 * @return This builder
		 */
		public PathUrlBuilder exists() {
			this.exists = true;
			return this;
		}
		
		/**
		 * Sets the relative paths to resolve the final path.
		 * @param first First path part
		 * @param more Optional additional path parts
		 * @return This builder
		 */
		public PathUrlBuilder relativeTo(String first, String... more) {
			this.relativeToFirst = first;
			this.relativeToMore = more;
			return this;
		}
		
		/**
		 * Gets the specified path, using the configuration of the builder.
		 * @param path Path
		 * @return Path instance. If exists is set, an exception will be thrown if the path doesn't exist
		 */
		public Path get(String path) {
			Path p = null;
			if (StringUtils.isEmpty(this.relativeToFirst)) {
				p = Paths.get(path);
			} else {
				String moreString[] = Arrays.copyOf(this.relativeToMore, this.relativeToMore.length + 1);
				moreString[moreString.length - 1] = path;
				p = Paths.get(this.relativeToFirst, moreString);
			}
			
			if (this.exists && !Files.exists(p)) {
				throw new JuRuntimeException("Path doesn't exist: %s (absolute: %s)", p, p.toAbsolutePath());
			}
			if (this.file && !Files.isRegularFile(p)) {
				throw new JuRuntimeException("Path is not a file: %s (absolute: %s)", p, p.toAbsolutePath());
			}
			if (this.directory && !Files.isDirectory(p)) {
				throw new JuRuntimeException("Path is not a directory: %s (absolute: %s)", p, p.toAbsolutePath());
			}
			
			return p;
		}
		
		/**
		 * Checks if the specified path is an existing file.
		 * @param path Path to file
		 * @return True if path is an existing file, false otherwise
		 */
		public boolean isExistingFile(Path path) {
			return Files.exists(path) && Files.isRegularFile(path);
		}
		
		/**
		 * Checks if the specified path is an existing directory.
		 * @param path Path to directory
		 * @return True if path is an existing directory, false otherwise
		 */
		public boolean isExistingDirectory(Path path) {
			return Files.exists(path) && Files.isDirectory(path);
		}
	}
	
	/**
	 * Gets a ResourceUrlBuilder that can be used to configure and perform resource lookup.
	 * @return ResourceUrlBuilder instance
	 */
	public static ResourceUrlBuilder resource() {
		return new ResourceUrlBuilder();
	}
	
	/**
	 * Builder to lookup resources.
	 * @author Martin
	 *
	 */
	public static class ResourceUrlBuilder {
		private Class<?> relativeClass;
		private boolean single = false;
		private boolean exceptionIfNone = false;
		
		static Boolean cacheVrfConversionFlag = null;
		static Boolean disableVfsForResourceLookup = null;
		
		/**
		 * Resolves the resource relative to the specified class, using the method
		 * Class.getResource.
		 * <p>
		 * If the resource starts with an '/', the path is absolute.
		 * @param clazz Relative class
		 * @return This builder
		 */
		public ResourceUrlBuilder relativeTo(Class<?> clazz) {
			this.relativeClass = clazz;
			return this;
		}
		
		/**
		 * Modifier for the get method to make sure that a single resource is found.
		 * <p>
		 * If multiple resources are found, an exception is thrown.
		 * <p>
		 * Default is false. 
		 * @return This builder
		 */
		public ResourceUrlBuilder single() {
			this.single = true;
			return this;
		}
		
		/**
		 * Modified to throw an exception if no resource is found.
		 * <p>
		 * Default is false, i.e. null is returned if no resource is found
		 * @return This builder
		 */
		public ResourceUrlBuilder exceptionIfNone() {
			this.exceptionIfNone = true;
			return this;
		}
		
		/**
		 * Sets the exceptionIfNone flag.
		 * @param exceptionIfNone If true, an exception is thrown if the resource doesn't exist
		 * @return This builder
		 */
		public ResourceUrlBuilder exceptionIfNone(boolean exceptionIfNone) {
			this.exceptionIfNone = exceptionIfNone;
			return this;
		}
		
		/**
		 * Gets a list of all resources on the classpath using the JuUrl classloader.
		 * <p>
		 * Paths are always absolute and must not start with a '/'.
		 * <p>
		 * Examples:<ul>
		 * 	<li>log4j.xml</li>
		 * 	<li>META-INF/persistence.xml</li>
		 * </ul>
		 * @param resourceName Resource name
		 * @return List of all resources with the specified name, as returned by the ClassLoader.getResources method. If none
		 * are found, an empty list is returned
		 */
		public List<URL> getAll(String resourceName) {
			return this.getAll(resourceName, true);
		}
		
		/**
		 * Helper method with package privacy to avoid stack overflows as JuPropertyChain is also evaluated using JuUrl.
		 * @param resouceName Resource Name
		 * @param considerVfsReplacement If false, VFS replacement isn't consided, i.e. the property defining it isn't evaluated
		 * @return List of URLs
		 */
		List<URL> getAll(String resourceName, boolean considerVfsReplacement) {
			try {
				String resourcePrefix = "";
				// If we have a relative class set, we'll need to specify the path explicitly...
				if (this.relativeClass != null) {
					String packageName = this.relativeClass.getPackage().getName();
					resourcePrefix = packageName.replaceAll("\\.", "/") + "/";
					
					// Note: This probably would fail on inner classes, but that shouldn't be a use case...
				}
						
				Enumeration<URL> resourcesEnum = Thread.currentThread().getContextClassLoader().getResources(resourcePrefix + resourceName);
				List<URL> resources = new ArrayList<>();
				while (resourcesEnum.hasMoreElements()) {
					URL url = resourcesEnum.nextElement();
					resources.add(considerVfsReplacement
							? this.convertVfsResourceIfNecessary(url)
							: url);
				}
				
				return resources;
			} catch (Exception ex) {
				throw new JuRuntimeException("Couldn't lookup resource " + resourceName, ex);
			}
		}
		
		/**
		 * Gets the resource with the specified name, taking the configuration of the
		 * builder into account.
		 * @param resourceName Resource name
		 * @return URL or null if none was found and exceptionIfNone is not set. May also throw an exception
		 * if multiples are found and single is set.
		 */
		public URL get(String resourceName) {
			return this.get(resourceName, true);
		}
		
		/**
		 * Helper method with package privacy to avoid stack overflows as JuPropertyChain is also evaluated using JuUrl.
		 * @param resouceName Resource Name
		 * @param considerVfsReplacement If false, VFS replacement isn't consided, i.e. the property defining it isn't evaluated
		 * @return URL
		 */
		URL get(String resourceName, boolean considerVfsReplacement) {
			URL url = null;
			if (this.relativeClass != null) {
				url = this.relativeClass.getResource(resourceName);
			} else {
				List<URL> urls = this.getAll(resourceName, false);
				if (this.single && urls.size() > 1) {
					XString xs = new XString("Found more than 1 resource with name %s:", resourceName);
					xs.increaseIndent();
					for (URL u : urls) xs.addLine(u.toString());
					throw new JuRuntimeException(xs.toString());
				} else if (urls.size() > 0) {
					url = urls.get(0);
				}
			}
			
			if (url == null && this.exceptionIfNone) {
				throw new JuRuntimeException("Resource not found: %s", resourceName);
			} else {
				return considerVfsReplacement
						? this.convertVfsResourceIfNecessary(url)
						: url;
			}
		}
		
		/**
		 * Converts a VFS resource (as used in JBoss) to a regular jar:file resource if necessary.
		 * <p>
		 * For details, see http://stackoverflow.com/questions/20100390/how-to-turn-off-or-disable-vfs-file-loading-in-jboss-as7
		 * <p>
		 * Note that VFS resource conversion makes only sense when developping locally and should be disabled for all server environments
		 * (as we cannot access the resource the way we do here in this context anyway as the EAR is not deployed exploadedly).
		 * 
		 * @param url
		 *            URL
		 * @return Converted URL or same URL if no conversion was necessary
		 */
		private URL convertVfsResourceIfNecessary(URL url) {
			// No need to convert null URLs...
			if (url == null) return url;

			boolean disableVfs;

			synchronized(JuUrl.class) {
				if (cacheVrfConversionFlag == null) {
					cacheVrfConversionFlag = JuUtils.getJuPropertyChain().get("ju.ee.url.disableVfsForResourceLookup", Boolean.class);
				}
				
				if (!cacheVrfConversionFlag) {
					disableVfs = JuUtils.getJuPropertyChain().get("ju.ee.url.disableVfsForResourceLookup", Boolean.class);
				} else {
					if (disableVfsForResourceLookup == null) {
						disableVfsForResourceLookup = JuUtils.getJuPropertyChain().get("ju.ee.url.disableVfsForResourceLookup", Boolean.class);
					}
					disableVfs = disableVfsForResourceLookup;
				}
			}
			
			if (disableVfs) {
				String externalForm = url.toExternalForm();
				if (externalForm.startsWith("vfs:")) {
					// VFS resource.
					// A VFS resource looks like    vfs:/pathToJar/jarFile.jar/pathToResource
					// and needs to be converted to jar:file:/pathToJar/jarFile.jar!/pathToResource
					
					// As some resources are deployed in exploded form, we might be able to access the file directly:
					String newPath = externalForm.replaceFirst("vfs:", "file:");
					// Try if file is accessible
					try {
						URL fileUrl = new URL(newPath);
						if (Files.exists(JuUrl.toPath(fileUrl))) {
							return fileUrl;
						}
					} catch (Exception ex) {
						logger.warn("Couldn't construct file URL for " + newPath);
					}
					
					// Now, we'll try to access the resource within a JAR. We'll still make sure that the JAR actually exists as an
					// accessible file.
					RegexUtil jarFile = new RegexUtil("(file:.+/([^/]+\\.jar))/");
					Match[] matches = jarFile.getMatches(newPath);
					if (matches.length > 0) {
						Match lastMatch = matches[matches.length - 1];
						String jarFilePath = lastMatch.getGroups()[0];
						if (IOUtil.exists().file(jarFilePath)) {
							String jarFileName = lastMatch.getGroups()[1];

							newPath = "jar:" + newPath.replaceAll(jarFileName + "/", jarFileName + "!/");

							try {
								return new URL(newPath);
							} catch (Exception ex) {
								logger.warn("Couldn't convert vfs resource to jar:file resource for {}. Using old URL. Exception: {}",
										externalForm, ex.getMessage());
								return url;
							}
						} else {
							return url;
						}
					} else {
						return url;
					}
				} else {
					return url;
				}
			} else {
				return url;
			}
		}
	}
}
