package ch.inftec.ju.util;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.interpolation.EnvarBasedValueSource;

/**
 * Helper class to temporarly set system properties that can will be set
 * to their original values on (auto) close.
 * <p>
 * Useful for tests that rely on system properties when we don't want to
 * change global state.
 * @author Martin
 *
 */
public class SystemPropertyTempSetter implements AutoCloseable, Serializable {
	private final Map<String, String> originalValues = new HashMap<>();
	
	private EnvMaps origEnvs;
	private boolean handleJuPropertyChainClearing;
	private boolean clearedPropertyChain = false;
	
	/**
	 * Creates a new SystemPropertyTempSetter that will clear the JU property chain
	 * automatically when needed.
	 */
	public SystemPropertyTempSetter() {
		this(true);
	}
	
	/**
	 * Creates a new SystemPropertyTempSetter.
	 * @param handleJuPropertyChainClearing If true, JU property chain is automatically cleared to
	 * make sure changes made by the temp setter are picked up by the JU default chain
	 */
	public SystemPropertyTempSetter(boolean handleJuPropertyChainClearing) {
		this.handleJuPropertyChainClearing = handleJuPropertyChainClearing;
	}
	
	/**
	 * Temporarly set the property to the specified value.
	 * @param key Key
	 * @param value Value
	 */
	public void setProperty(String key, String value) {
		if (!this.originalValues.containsKey(key)) {
			this.originalValues.put(key, System.getProperty(key));
		}
		if (value == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, value);
		}
		
		this.clearJuChainIfNecessary();
	}
	
	private void clearJuChainIfNecessary() {
		if (this.handleJuPropertyChainClearing && !this.clearedPropertyChain) {
			JuUtils.clearPropertyChain();
			this.clearedPropertyChain = true;
		}
	}
	
	/**
	 * Temporarly sets the environmental variable with the specified key.
	 * <p>
	 * Note that this will only set the value in the JVM memory and not effect
	 * the actual value of the OS environmental variable.
	 * @param key Key
	 * @param value Value
	 */
	public void setEnv(String key, String value) {
		// Using Hack from http://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
		// that should support both Windows and Unix
		
		EnvMaps maps = this.getEnvMaps();

		// If this is the first run, backup original values
		if (this.origEnvs == null) {
			this.origEnvs = new EnvMaps();
			this.origEnvs.env = new HashMap<>(maps.env);
			if (maps.envCaseInsensitive != null) {
				this.origEnvs.envCaseInsensitive = new HashMap<>(maps.envCaseInsensitive);
			}
		}
		
		maps.put(key, value);
		
		this.resetEnvarBasedValueSourceCache();
		this.clearJuChainIfNecessary();
	}
	
	/**
	 * Hack for UnitTests that use EnvarBasedValueSource. This class caches values by default,
	 * so we need to clear that internal cache when Using SystemPropertyTempSetter...
	 */
	private void resetEnvarBasedValueSourceCache() {
		ReflectUtils.setStaticField(EnvarBasedValueSource.class, "envarsCaseSensitive", null, true);
		ReflectUtils.setStaticField(EnvarBasedValueSource.class, "envarsCaseInsensitive", null, true);
	}
	
	@SuppressWarnings("unchecked")
	private EnvMaps getEnvMaps() {
		EnvMaps envMaps = new EnvMaps();
		
		try {
			Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
			Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
			theEnvironmentField.setAccessible(true);
			
			envMaps.env = (Map<String, String>) theEnvironmentField.get(null);
			Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
			theCaseInsensitiveEnvironmentField.setAccessible(true);
			envMaps.envCaseInsensitive = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
		} catch (NoSuchFieldException e) {
			try {
				Class<?>[] classes = Collections.class.getDeclaredClasses();
				Map<String, String> env = System.getenv();
				for(Class<?> cl : classes) {
					if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
						Field field = cl.getDeclaredField("m");
						field.setAccessible(true);
						Object obj = field.get(env);
						envMaps.env = (Map<String, String>) obj;
					}
				}
			} catch (Exception e2) {
				throw new JuRuntimeException(e2);
			}
		} catch (Exception e1) {
			throw new JuRuntimeException(e1);
		}
		
		return envMaps;
	}
	
	private static final class EnvMaps {
		private Map<String, String> env;
		private Map<String, String> envCaseInsensitive;
		
		public void put(String key, String value) {
			if (value == null) {
				this.env.remove(key);
				if (this.envCaseInsensitive != null) {
					this.envCaseInsensitive.remove(key);
				}
			} else {
				this.env.put(key, value);
				if (this.envCaseInsensitive != null) {
					this.envCaseInsensitive.put(key, value);
				}
			}
		}
		
		public void reset(EnvMaps origMaps) {
			this.env.clear();
			this.env.putAll(origMaps.env);
			if (origMaps.envCaseInsensitive != null) {
				this.envCaseInsensitive.clear();
				this.envCaseInsensitive.putAll(origMaps.envCaseInsensitive);
			}
		}
	}
	
	@Override
	public void close() {
		// Reset System Properties
		for (String key : this.originalValues.keySet()) {
			String value = this.originalValues.get(key);
			if (value == null) {
				System.clearProperty(key);
			} else {
				System.setProperty(key, value);
			}
		}
		
		// Reset Env
		if (this.origEnvs != null) {
			this.getEnvMaps().reset(this.origEnvs);
		}
		
		// Clear JU property chain if we had changes
		if (this.handleJuPropertyChainClearing && this.originalValues.size() > 0) {
			JuUtils.clearPropertyChain();
		}
	}
}
