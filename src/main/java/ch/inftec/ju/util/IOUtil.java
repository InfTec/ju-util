package ch.inftec.ju.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.inftec.ju.util.function.Predicate;
import ch.inftec.ju.util.function.Predicates;
import ch.inftec.ju.util.io.NewLineReader;

/**
 * Utility class containing I/O related helper methods. Methods that depend on a charset
 * or on a text Reader are not static. In this case, a new IOUtil instance must be created using either the
 * default charset or an explicit charset (as specified in the constructor).
 * <p>
 * The IOUtil class converts line endings to a single LF character ('\n'), regardless of the actual
 * line feed policy used in the source file.
 * @author tgdmemae
 *
 */
public final class IOUtil {
	static final Logger log = LoggerFactory.getLogger(IOUtil.class);
	
	/**
	 * The default charset that will be used to initialize an IOUtil instance
	 * if no explizit charset is specified.
	 */
	private static String defaultCharset = null;
	
	private static int tempFileCounter = 0;

	/**
	 * The Unix line separator string.
	 */
	public static final String LINE_SEPARATOR_UNIX = "\n";

	/**
	 * The default buffer size to use for
	 * {@link #copyLarge(Reader, Writer)}
	 */
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	/**
	 * The Charset used by the IOUtil instance. If not submitted with the constructor, the
	 * defaultCharset will be used.
	 */
	private final String charset;
	
	/**
	 * Gets the default charset used by the IOUtil classes if no explicit
	 * charset is submited with the call. If not changed by the used,
	 * this is the system's default charset as returned by
	 * Charset.defaultCharset()
	 * @return Default charset
	 */
	public synchronized static String getDefaultCharset() {
		if (IOUtil.defaultCharset == null) {
			IOUtil.defaultCharset = Charset.defaultCharset().displayName();
		}
		return IOUtil.defaultCharset;
	}
	
	/**
	 * Sets the default charset used by the IOUtil classes if no explicit
	 * charset is submitted with the call.
	 * <p>
	 * Note that this will only change the default charset for the IOUtil class
	 * and not for the whole java runtime.
	 * @param charset Default charset
	 */
	public synchronized static void setDefaultCharset(String charset) {
		IOUtil.defaultCharset = charset;
	}
	
	/**
	 * Gets the charset that this IOUtil instance uses.
	 * @return Charset
	 */
	public String getCharset() {
		return this.charset;
	}
	
	/**
	 * Creates a new IOUtil instance using the default charset.
	 */
	public IOUtil() {
		this(IOUtil.getDefaultCharset());
	}
	
	/**
	 * Creates a new IOUtil instance with the specified charset.
	 */
	public IOUtil(String charset) {
		this.charset = charset;
	}
	
	/**
	 * Closes the specified Reader and consumes any exception that
	 * might be raised.
	 * @param reader Reader instance
	 */
	public static void close(Reader reader) {
		try {
			log.debug("Closing Reader: " + ObjectUtils.identityToString(reader));
			if (reader != null) reader.close();
		} catch (IOException ex) {
			log.warn("Could not close Reader instance: " + ex.getMessage());
		}
	}
	
	/**
	 * Closes the specified InputStream and consumes any exception that
	 * might be raised.
	 * @param stream InputStream instance
	 */
	public static void close(InputStream stream) {
		try {
			log.debug("Closing InputStream: " + ObjectUtils.identityToString(stream));
			if (stream != null) stream.close();
		} catch (IOException ex) {
			log.warn("Could not close InputStream instance: " + ex.getMessage());
		}
	}
	
	/**
	 * Closes the specified OutputStream and consumes any exception that
	 * might be raised.
	 * @param stream OutputStream instance
	 */
	public static void close(OutputStream stream) {
		try {
			log.debug("Closing OutputStream: " + ObjectUtils.identityToString(stream));
			if (stream != null) stream.close();
		} catch (IOException ex) {
			log.warn("Could not close OutputStream instance: " + ex.getMessage());
		}
	}
	
	/**
	 * Generates a String for the specified reader.
	 * @param reader Reader instance
	 * @throws JuRuntimeException If the conversion fails
	 */
	public static String toString(Reader reader) {
		StringWriter stringWriter = new StringWriter();

		try {
			copy(reader, stringWriter);
			return stringWriter.toString();
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't generate String for Reader", ex);
		}
	}

	/**
	 * Copy bytes from an <code>InputStream</code> to an
	 * <code>OutputStream</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * <p>
	 * Large streams (over 2GB) will return a bytes copied value of
	 * <code>-1</code> after the copy has completed since the correct
	 * number of bytes cannot be returned as an int. For large streams
	 * use the <code>copyLarge(InputStream, OutputStream)</code> method.
	 *
	 * @param input  the <code>InputStream</code> to read from
	 * @param output  the <code>OutputStream</code> to write to
	 * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
	 * @throws NullPointerException if the input or output is null
	 * @throws IOException if an I/O error occurs
	 * @since Commons IO 1.1
	 */
	public static int copy(InputStream input, OutputStream output) throws IOException {
		long count = copyLarge(input, output);
		if (count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) count;
	}

	/**
	 * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
	 * <code>OutputStream</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 *
	 * @param input  the <code>InputStream</code> to read from
	 * @param output  the <code>OutputStream</code> to write to
	 * @return the number of bytes copied
	 * @throws NullPointerException if the input or output is null
	 * @throws IOException if an I/O error occurs
	 * @since Commons IO 1.3
	 */
	public static long copyLarge(InputStream input, OutputStream output)
			throws IOException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		long count = 0;
		int n;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	/**
	 * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * <p>
	 * Large streams (over 2GB) will return a chars copied value of
	 * <code>-1</code> after the copy has completed since the correct
	 * number of chars cannot be returned as an int. For large streams
	 * use the <code>copyLarge(Reader, Writer)</code> method.
	 *
	 * @param input  the <code>Reader</code> to read from
	 * @param output  the <code>Writer</code> to write to
	 * @return the number of characters copied, or -1 if &gt; Integer.MAX_VALUE
	 * @throws NullPointerException if the input or output is null
	 * @throws IOException if an I/O error occurs
	 * @since Commons IO 1.1
	 */
	public static int copy(Reader input, Writer output) throws IOException {
		long count = copyLarge(input, output);
		if (count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) count;
	}

	/**
	 * Copy chars from a large (over 2GB) <code>Reader</code> to a <code>Writer</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 *
	 * @param input  the <code>Reader</code> to read from
	 * @param output  the <code>Writer</code> to write to
	 * @return the number of characters copied
	 * @throws NullPointerException if the input or output is null
	 * @throws IOException if an I/O error occurs
	 * @since Commons IO 1.3
	 */
	public static long copyLarge(Reader input, Writer output) throws IOException {
		char[] buffer = new char[DEFAULT_BUFFER_SIZE];
		long count = 0;
		int n;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	/**
	 * Converts the specified String to a String containing Unix style new lines, i.e. \n
	 * @param s String to be converted
	 * @return String containing \n for line breaks
	 */
	public static String toNewLineUnix(String s) {
		if (s == null) {
			return null;
		} else {
			NewLineReader reader = new NewLineReader(new StringReader(s), null, LINE_SEPARATOR_UNIX);
			return IOUtil.toString(reader);
		}
	}
	
	/**
	 * Creates a new temporary file in the default temporary directory.
	 * <p>
	 * The file will be new/empty and deleted automatically when the JVM is exited.
	 * @return New temporary file
	 */
	public static synchronized Path getTemporaryFile() throws JuException {
		try {
			Path tempPath = Files.createTempFile(String.format("%s_%s_%s", 
					IOUtil.class.getName(),
					IOUtil.tempFileCounter++,
					System.currentTimeMillis())
					, "tmp");
						
			tempPath.toFile().deleteOnExit();
			
			return tempPath;
		} catch (Exception ex) {
			throw new JuException("Couldn't create temporary file", ex);
		}
	}
	
	/**
	 * Deletes the specified file (if it exists) and throws a runtime
	 * exception if deletion fails.
	 * @param path Path to file to delete
	 * @return True if file existed, false otherwise
	 */
	public static boolean deleteFile(Path path) {
		if (Files.exists(path)) {
			if (!Files.isRegularFile(path)) {
				throw new JuRuntimeException("Not a regular file: " + path);
			}
			try {
				Files.delete(path);
				return true;
			} catch (Exception ex) {
				throw new JuRuntimeException("Couldn't delete file: " + path, ex);
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Copies the specified source file to the specified destionation file.
	 * <p>
	 * All parent directories of the destination file will be created if necessary. Any
	 * IOException will be wrapped into a JuRuntimmeException and be contained in this exception
	 * as cause.
	 * @param srcFile Source file
	 * @param dstFile Destination file
	 * @param overwrite If true, any existing file will be overwritten
	 */
	public static void copyFile(Path srcFile, Path dstFile, boolean overwrite) {
		if (Files.exists(dstFile) && !overwrite) {
			throw new JuRuntimeException("Destination file %s already exists", dstFile);
		}
		
		try {
			Files.createDirectories(dstFile.getParent());
			Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't copy file %s to %s", ex, srcFile, dstFile);
		}
	}
	
	/**
	 * Delets all specified files (if they exist).
	 * @param paths Path to files
	 * @return True if at least one file was deleted, false otherwise
	 */
	public static boolean deleteFiles(Path... paths) {
		boolean oneDeleted = false;
		for (Path path : paths) {
			if (IOUtil.deleteFile(path)) {
				oneDeleted = true;
			}
		}
		return oneDeleted;
	}
	
	/**
	 * Creates a file using the specified path.
	 * @param file Path to file
	 * @param overwrite If true and the file exists, it will be truncated. Otherwise, an exception will be thrown.
	 * @return Path to the file
	 */
	public static Path createFile(Path file, boolean overwrite) throws JuException {
		try {
			if (Files.exists(file)) {
				if (Files.isDirectory(file)) {
					throw new JuException("Directory with file name exists: " + file);
				} else {
					if (!overwrite) {
						throw new JuException("File exists: " + file);
					} else {
						// overwrite, i.e. recreate
						Files.delete(file);
						Files.createFile(file);
					}
				}
			} else {
				Files.createDirectories(file.getParent()); // Makes sure directories exist
				Files.createFile(file);
			}
		} catch (JuException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new JuException("Couldn't create file " + file, ex);
		}
		
		return file;
	}
	
	/**
	 * Checks if the specified object is Serializable.
	 * <p>
	 * This will actually perform a Serialization and not just check for tag interfaces.
	 * @param obj Object
	 * @return True if the objects is serializable, false otherwise
	 */
	public static boolean isSerializable(Object obj) {
		return IOUtil.isSerializableOrException(obj) == null;
	}
	
	/**
	 * Checks if the specified object is Serializable, returning null if it is or
	 * the Exception that occurred if it is not.
	 * <p>
	 * This will actually perform a Serialization and not just check for tag interfaces.
	 * @param obj Object
	 * @return True if the objects is serializable, false otherwise
	 */
	public static Exception isSerializableOrException(Object obj) {
		try (ObjectOutputStream s = new ObjectOutputStream(new ByteArrayOutputStream())) {
			s.writeObject(obj);
			return null;
		} catch (Exception ex) {
			return ex;
		}
	}
	
	/**
	 * Lists all files in the specified directory.
	 * @param parentDir Parent directory
	 * @return List of all files in the directory (recursively)
	 */
	public static List<Path> listFiles(Path parentDir) {
		return IOUtil.listFiles(parentDir, Predicates.ALWAYS_TRUE);
	}

	/**
	 * Lists all files in the specified parent directory with the specified ending (e.g. .xml).
	 * @param parentDir Parent directory
	 * @param ending Ending, e.g. .xml
	 * @return List of Path instances to files
	 */
	public static List<Path> listFiles(Path parentDir, final String ending) {
		return IOUtil.listFiles(parentDir, new Predicate<Path>() {
			@Override
			public boolean test(Path input) {
				return input.getFileName().toString().endsWith(ending);
			}
		});
	}
	
	/**
	 * Lists all files of the specified parent directory that meet the predicate.
	 * @param parentDir ParentDirectory
	 * @param predicate Predicate whether the file should be included
	 * @return List of file paths
	 */
	public static List<Path> listFiles(Path parentDir, final Predicate<Path> predicate) {
		final List<Path> files = new ArrayList<>();
		
		try {
			Files.walkFileTree(parentDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (predicate.test(file)) files.add(file);
					
					return FileVisitResult.CONTINUE;
				}
			});
			
			return files;
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't list file of directory %s", ex, parentDir);
		}
	}
	
	/**
	 * Helper to check for the existence of IO (file, directories, ...) objects.
	 * <p>
	 * By default, no exceptions will be rised (e.g. on invalid paths or the like).
	 * 
	 * @return ExistsBuilder instance
	 */
	public static ExistsBuilder exists() {
		return new ExistsBuilder();
	}

	/**
	 * Helper class to check for the existence of IO related objects, handling exceptions and the like...
	 * @author Martin
	 *
	 */
	public static final class ExistsBuilder {
		private boolean noExceptions = true;

		/**
		 * Use IOUtil.exists() to get an instance
		 */
		private ExistsBuilder() {
		}

		/**
		 * Sets if the builder should throw exceptions, e.g. on invalid paths.
		 * <p>
		 * Default setting is false.
		 * 
		 * @param noExceptions
		 *            False if builder should throw exceptions
		 */
		public ExistsBuilder noExceptions(boolean noExceptions) {
			this.noExceptions = noExceptions;
			return this;
		}

		/**
		 * Checks if the specified file exits.
		 * <p>
		 * We'll support URLs starting with 'file:' as well.
		 * 
		 * @param path
		 *            Path to file
		 * @return True if it exists (and is a file), false otherwise
		 */
		public boolean file(String path) {
			try {
				Path filePath;
				if (path.toLowerCase().startsWith("file:")) {
					// Use URL class to get Path
					filePath = JuUrl.toPath(new URL(path));
				} else {
					filePath = Paths.get(path);
				}

				return Files.isRegularFile(filePath);
			} catch (Exception ex) {
				return this.handleException(ex, false);
			}
		}

		/**
		 * Checks if the specified file exists. This will use Files.isRegularFile, but handle
		 * exceptions accordingly to the noExceptions setting.
		 * 
		 * @param path
		 *            Path to file to check
		 * @return True if it exists (and is a file), false otherwise
		 */
		public boolean file(Path path) {
			try {
				return Files.isRegularFile(path);
			} catch (Exception ex) {
				return this.handleException(ex, false);
			}
		}

		private <T> T handleException(Exception ex, T res) {
			if (this.noExceptions) {
				return res;
			} else {
				throw new JuRuntimeException("Couldn't check for existence", ex);
			}
		}
	}

	/**
	 * Loads the specified URL resource into a string. This method uses the charset of the
	 * IOUtil instance.
	 * <p>
	 * Line breaks from the source will be converted to LF if necessary.
	 * 
	 * @param url
	 *            URL to resource
	 * @param replacements
	 *            Optional 'key, value' strings to replace %key% tags in the resource with the specified value
	 * @return Loaded resource as string
	 * @throws JuRuntimeException
	 *             If the resource cannot be loaded
	 */
	public String loadTextFromUrl(URL url, String... replacements) {
		Validate.notNull(url, "Cannot load text from null URL");

		try {
			try (Reader reader = this.createReader(url)) {
			
				StringBuilder sb = new StringBuilder();
				char[] buff = new char[1024];
				int read;
				while ((read = reader.read(buff)) > 0) {
					sb.append(buff, 0, read);
				}
				
				return JuStringUtils.replaceAll(sb.toString(), replacements);
			}
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't load text from URL " + url, ex);
		}
	}

	/**
	 * Creates a Reader for the resource at the specified URL using the IOUtils
	 * charset.
	 * <p>
	 * Line breaks will be automatically converted to LF if necessary.
	 * <p>
	 * The reader will be buffered.
	 * <p>
	 * The reader needs to be closed by the client.
	 * @param url URL to text resource
	 * @return Reader instance
	 */
	public BufferedReader createReader(URL url) {
		try {
			return new BufferedReader(
					new NewLineReader(
						new InputStreamReader(url.openStream(), this.charset)
							, null, NewLineReader.LF));
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't create reader for URL " + url, ex);
		}
	}

	/**
	 * Loads properties from the specified URL.
	 * @return Properties
	 * @throws JuException If loading fails
	 */
	public Properties loadPropertiesFromUrl(URL url) throws JuException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), this.charset))) {
			Properties props = new Properties();
			props.load(reader);
			
			return props;
		} catch (Exception ex) {
			throw new JuException("Couldn't load properties from URL: " + url, ex);
		}		
	}
	
	/**
	 * Loads a manifest from the specified URL.
	 * <p>
	 * Uses an InputStream to read from the manifest, thus won't be able to handle
	 * special characters correctly.
	 * @param url URL to manifest file
	 * @return Manifest instance
	 * @throws JuException If the manifest cannot be loaded from the URL
	 */
	public static Manifest loadManifestFromUrl(URL url) throws JuException {
		try (BufferedInputStream in = new BufferedInputStream(url.openStream())) {
			return new Manifest(in);
		} catch (Exception ex) {
			throw new JuException("Couldn't load manifest from URL: " + url, ex);
		}
	}
	
	/**
	 * Writes the specified text to a file.
	 * @param text Text
	 * @param file File to write to
	 * @param overwrite If true, an existing file will be overwritten. If false, it will be preserved.
	 * @throws JuException If the file cannot be written
	 */
	public void writeTextToFile(String text, Path file, boolean overwrite) throws JuException {
		try {
			if (Files.exists(file)) {
				if (Files.isDirectory(file)) throw new JuException("Directory with file name exists: " + file);
				else if (!overwrite) throw new JuException("File exists: " + file);
			}		
			
			try (BufferedWriter w = Files.newBufferedWriter(file, Charset.forName(this.charset), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING)) {
				w.write(text);
			}
		} catch (JuException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new JuException("Couldn't write text to file: " + file, ex);
		}
	}
	
	/**
	 * Opens a Writer to the specified file with this IOUtils character encoding.
	 * @param file File to write to
	 * @param append If true and the file exists, text will be appended. If the file doesn't exist, we create a new one.
	 * @param overwrite If true and the file exists, it will be overwritten. If false and the file exists, a
	 * JuException will be thrown. If append is true, overwrite must be false.
	 * @return Writer to write to the file. The caller is responsible of closing the writer
	 * @throws JuException If the file cannot be opened for writing using the specified options
	 */
	public Writer openWriter(Path file, boolean append, boolean overwrite) throws JuException {
		if (append) AssertUtil.assertFalse("When appending, overwrite must be set to false", overwrite);
		
		try {
			OpenOption openOption = StandardOpenOption.WRITE;
			// Handle append case
			if (Files.exists(file) && append) {
				openOption = StandardOpenOption.APPEND;
			} else {
				IOUtil.createFile(file, overwrite);
			}
						
			return Files.newBufferedWriter(file, Charset.forName(this.charset), openOption);
		} catch (JuException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new JuException("Couldn't open Writer to: " + file, ex);
		}
	}
	
	@Override
	public String toString() {
		return JuStringUtils.toString(this, "charset", this.charset);
	}

	/**
	 * Unconditionally close a <code>Closeable</code>.
	 * <p>
	 * Equivalent to {@link Closeable#close()}, except any exceptions will be ignored.
	 * This is typically used in finally blocks.
	 * <p>
	 * Example code:
	 * <pre>
	 *   Closeable closeable = null;
	 *   try {
	 *       closeable = new FileReader("foo.txt");
	 *       // process closeable
	 *       closeable.close();
	 *   } catch (Exception e) {
	 *       // error handling
	 *   } finally {
	 *       IOUtils.closeQuietly(closeable);
	 *   }
	 * </pre>
	 *
	 * @param closeable the object to close, may be null or already closed
	 * @since Commons IO 2.0
	 */
	public static void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}

	/**
	 * Unconditionally close an <code>Reader</code>.
	 * <p>
	 * Equivalent to {@link Reader#close()}, except any exceptions will be ignored.
	 * This is typically used in finally blocks.
	 * <p>
	 * Example code:
	 * <pre>
	 *   char[] data = new char[1024];
	 *   Reader in = null;
	 *   try {
	 *       in = new FileReader("foo.txt");
	 *       in.read(data);
	 *       in.close(); //close errors are handled
	 *   } catch (Exception e) {
	 *       // error handling
	 *   } finally {
	 *       IOUtils.closeQuietly(in);
	 *   }
	 * </pre>
	 *
	 * @param input  the Reader to close, may be null or already closed
	 */
	public static void closeQuietly(Reader input) {
		closeQuietly((Closeable)input);
	}
}
