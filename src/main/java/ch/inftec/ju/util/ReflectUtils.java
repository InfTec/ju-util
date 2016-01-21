package ch.inftec.ju.util;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * Class containing reflection related utility methods.
 * 
 * @author tgdmemae
 * 
 */
public final class ReflectUtils {
	/**
	 * Don't instantiate.
	 */
	private ReflectUtils() {
		throw new AssertionError("use only statically");
	}
	
	/**
	 * Get the underlying class for a type, or null if the type is a variable
	 * type.
	 * 
	 * @param type
	 *            the type
	 * @return the underlying class
	 */
	public static Class<?> getClass(Type type) {
		if (type instanceof Class) {
			return (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			return getClass(((ParameterizedType) type).getRawType());
		} else if (type instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) type)
					.getGenericComponentType();
			Class<?> componentClass = getClass(componentType);
			if (componentClass != null) {
				return Array.newInstance(componentClass, 0).getClass();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the inner class of the specified class with the specified name.
	 * <p>
	 * If the class does not exist, null is returned.
	 * <p>
	 * Note that the class (or interface) has to be public.
	 * @param clazz Enclosing class
	 * @param innerClassName Public inner class or interface
	 * @return Inner class or null if no such class exists
	 */
	public static Class<?> getInnerClass(Class<?> clazz, String innerClassName) {
		for (Class<?> innerClass : clazz.getClasses()) {
			if (innerClass.getSimpleName().equals(innerClassName)) {
				return innerClass;
			}
		}
		
		return null;
	}

	/**
	 * Get the actual type arguments a child class has used to extend a generic
	 * base class.
	 * 
	 * @param baseClass
	 *            the base class
	 * @param childClass
	 *            the child class
	 * @return a list of the raw classes for the actual type arguments.
	 */
	public static <T> List<Class<?>> getTypeArguments(Class<T> baseClass,
			Class<? extends T> childClass) {
		Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
		Type type = childClass;
		// start walking up the inheritance hierarchy until we hit baseClass
		while (!getClass(type).equals(baseClass)) {
			if (type instanceof Class) {
				// there is no useful information for us in raw types, so just
				// keep going.
				type = ((Class<?>) type).getGenericSuperclass();
			} else {
				ParameterizedType parameterizedType = (ParameterizedType) type;
				Class<?> rawType = (Class<?>) parameterizedType.getRawType();

				Type[] actualTypeArguments = parameterizedType
						.getActualTypeArguments();
				TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
				for (int i = 0; i < actualTypeArguments.length; i++) {
					resolvedTypes
							.put(typeParameters[i], actualTypeArguments[i]);
				}

				if (!rawType.equals(baseClass)) {
					type = rawType.getGenericSuperclass();
				}
			}
		}

		// finally, for each actual type argument provided to baseClass,
		// determine (if possible)
		// the raw class for that type argument.
		Type[] actualTypeArguments;
		if (type instanceof Class) {
			actualTypeArguments = ((Class<?>) type).getTypeParameters();
		} else {
			actualTypeArguments = ((ParameterizedType) type)
					.getActualTypeArguments();
		}
		List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
		// resolve types by chasing down type variables.
		for (Type baseType : actualTypeArguments) {
			while (resolvedTypes.containsKey(baseType)) {
				baseType = resolvedTypes.get(baseType);
			}
			typeArgumentsAsClasses.add(getClass(baseType));
		}
		return typeArgumentsAsClasses;
	}
	
	/**
	 * Gets the class whose method called the current method.
	 * @return Class that called the current method
	 */
	public static Class<?> getCallingClass() {
		try {
			StackTraceElement stackTrace[] = Thread.currentThread().getStackTrace();
		
			// The first element in the stack trace will be the getStackTrace method,
			// the second this method, the third the called method - i.e. the method
			// of the calling class is the 4th element.
			return Class.forName(stackTrace[3].getClassName());
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException("Couldn't get calling class", ex);
		}
	}
	
	/**
	 * Gets an array of types for the specified objects. If an object is null,
	 * it is assumed to be of type Object.
	 * @param objs Array ob objects
	 * @return Array of corresponding class types for the objects
	 */
	public static Class<?>[] getTypes(Object objs[]) {
		ArrayList<Class<?>> types = new ArrayList<Class<?>>();
		
		for (Object obj : objs) {
			types.add(obj == null ? Object.class : obj.getClass());
		}
		
		return (Class<?>[])types.toArray(new Class<?>[0]);
	}
	
	/**
	 * Gets the method of the specified class by its name.
	 * <p>
	 * If no such method can be found, null is returned.
	 * <p>
	 * This uses the Java Class.getMethod function internally.
	 * @param clazz Clazz containing the method
	 * @param name Name of the method
	 * @param paramTypes Parameter types of the method
	 * @return Method instance of null if no such method exists
	 */
	public static Method getMethod(Class<?> clazz, String name, Class<?> paramTypes[]) {
		try {
			Method method = clazz.getMethod(name, paramTypes);
			return method;
		} catch (NoSuchMethodException ex) {
			return null;
		}
	}
	
	/**
	 * Gets the first declared method that matches the specified name and parameter types. In contrast
	 * to the Java reflection method, this method will try to find a method that only matches super types
	 * of the specified parameters instead of the exact type.
	 * <br>
	 * If multiple methods match, the first returned by the Class.getMethods method will be returned
	 * @param clazz Class instance
	 * @param paramTypes Parameter types
	 * @return Method of the class with the specified parameter types or null if no such
	 * method exists
	 */
	public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?> paramTypes[]) {
		// Try to get the exact method using the Java built in method
		try {
			return clazz.getDeclaredMethod(name, paramTypes);
		} catch (NoSuchMethodException ex) {
			// Ignore this exception and try to find a method that matches
			// super types of the parameter types
		}
		
		for (Method m : clazz.getDeclaredMethods()) {
			if (m.getName().equals(name) && m.getParameterTypes().length == paramTypes.length) {
				boolean mismatch = false;
				for (int i = 0; i < paramTypes.length; i++) {
					if (!m.getParameterTypes()[i].isAssignableFrom(paramTypes[i])) {
						mismatch = true;
						break;
					}
				}
				if (!mismatch) return m;
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the first declared method that matches the specified name and parameter types. In contrast
	 * to the Java reflection method, this method will try to find a method that only matches super types
	 * of the specified parameters instead of the exact type. <br>
	 * If multiple methods match, the first returned by the Class.getMethods method will be returned
	 * <p>
	 * This method will also include declared methods of parent classes.
	 * 
	 * @param clazz
	 *            Class instance
	 * @param paramTypes
	 *            Parameter types
	 * @return Method of the class with the specified parameter types or null if no such
	 *         method exists
	 */
	public static Method getDeclaredMethodInherited(Class<?> clazz, String name, Class<?> paramTypes[]) {
		Class<?> c = clazz;
		while (c != null) {
			Method m = ReflectUtils.getDeclaredMethod(c, name, paramTypes);
			if (m != null) {
				return m;
			} else {
				c = c.getSuperclass();
			}
		}

		return null;
	}

	/**
	 * Gets the value of a static field.
	 * 
	 * @param clazz
	 *            Clazz to get static field from
	 * @param fieldName
	 *            Name of the field
	 * @param defaultValue
	 *            Default value that is returned if the field doesn't exist or has the value null.
	 * @return Value of the field or the specified return value if the field either doesn't exist or is null
	 * @throws JuRuntimeException
	 *             If the field cannot be read
	 */
	public static Object getStaticFieldValue(Class<?> clazz, String fieldName, Object defaultValue) {
		try {
			Object fieldValue = FieldUtils.readStaticField(clazz, fieldName, true);
			return fieldValue != null ? fieldValue : defaultValue;
		} catch (IllegalArgumentException ex) {
			return defaultValue;
		} catch (Exception ex) {
			throw new JuRuntimeException(String.format("Couldn't read static field %s from class %s", fieldName, clazz), ex);
		}
	}
	
	/**
	 * Gets the value of the specified field in the specified object.
	 * <p>
	 * If the field is not accessible, the method tries to overwrite the
	 * accessibility.
	 * @param obj Object to get field value of
	 * @param field Field to get
	 * @return
	 */
	public static Object getFieldValue(Object obj, Field field) {
		try {
			if (!field.isAccessible()) field.setAccessible(true);
			return field.get(obj);
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't access field", ex);
		}
	}
	
	/**
	 * Creates an instance of the specified class, forcing access to the default constructor if necessary.
	 * @param clazz Clazz
	 * @param forceAccess If true, access to a private constructor is forces
	 * @param parameters Optional list of parameters to pass to the constructor
	 * @return New instance of the specified class
	 * @throws JuRuntimeException if the instance cannot be created using the default constructor
	 */
	public static <T> T newInstance(Class<T> clazz, boolean forceAccess, Object... parameters) {
		try {
			if (!forceAccess && parameters.length == 0) {
				return clazz.newInstance();
			} else {
				Class<?> parameterTypes[] = new Class<?>[parameters.length];
				for (int i = 0; i < parameterTypes.length; i++) {
					AssertUtil.assertNotNull("Null parameters not supported yet", parameters[i]);
					parameterTypes[i] = parameters[i].getClass();
				}
				
				Constructor<T> constructor = clazz.getDeclaredConstructor(parameterTypes);
				if (forceAccess) {
					constructor.setAccessible(true);
				}
				
				return constructor.newInstance(parameters);
			}
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't create instance using default constructor for class " + clazz, ex);
		}
	}
	
	/**
	 * Gets all declared Methods of the specified class that have the specified annotation.
	 * <p>
	 * This will not return any inherited methods of base classes.
	 * <p>
	 * Annotations need to have retention=RUNTIME to be found at runtime
	 * @param clazz Class of the object to find methods of
	 * @param annotationClass Class of the annotations the methods need to have
	 * @return List of declared methods of the class (regardless of accessibility) that have the annotation assigned. Will be ordered by name.
	 */
	public static List<Method> getDeclaredMethodsByAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
		Map<String, Method> methods = new TreeMap<>();
		
		for (Method method : clazz.getDeclaredMethods()) {
			Annotation a = method.getAnnotation(annotationClass);
			if (a != null) methods.put(method.getName(), method);			
		}
		
		return new ArrayList<>(methods.values());
	}
	
	/**
	 * Gets all declared fields of the specified class that have the specified annotation.
	 * <p>
	 * This will not returned any inherited fields of base classes.
	 * <p>
	 * Annotations need to have retention=RUNTIME to be found at runtime
	 * @param clazz Class of the object to find fields of
	 * @param annotationClass Class of the annotation the field needs to have
	 * @return All declared fields of the class (regardless of accessibility) that have the annotation assigned.
	 * Fields will be sorted by name
	 */
	public static List<Field> getDeclaredFieldsByAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
		Map<String, Field> fields = new TreeMap<>();
		
		for (Field field : clazz.getDeclaredFields()) {
			Annotation a = field.getAnnotation(annotationClass);
			if (a != null) fields.put(field.getName(), field);			
		}
		
		return new ArrayList<>(fields.values());
	}
	
	/**
	 * Sets the static field of a class to a new value.
	 * @param clazz Class name
	 * @param fieldName Static field name
	 * @param val New value
	 * @param force If true, accessibility is forced (used to set private fields
	 */
	public static void setStaticField(Class<?> clazz, String fieldName, Object val, boolean force) {
		try {
			Field field = clazz.getDeclaredField(fieldName);
			if (force && !field.isAccessible()) {
				field.setAccessible(true);
			}
			field.set(null, val);
		} catch (Exception ex) {
			throw new JuRuntimeException("Couldn't set static field %s.%s to value %s", ex, clazz, fieldName, val);
		}
	}
	
	/**
	 * Gets the value if the (alphabetically) first declared field of the specified object that has the
	 * specified annotation assigned.
	 * <p>
	 * If no such field can be found, null is returned.
	 * @param obj Object to find field of
	 * @param annotationClass Class of the annotation the field needs to have
	 * @param forceAccess If true, access is forced even if the field is private
	 * @return Value of the (alphabetically) first declared field with the specified annotation or null if none exists
	 */
	public static Object getDeclaredFieldValueByAnnotation(Object obj, Class<? extends Annotation> annotationClass, boolean forceAccess) {
		List<Field> fields = ReflectUtils.getDeclaredFieldsByAnnotation(obj.getClass(), annotationClass);
		if (fields.isEmpty()) return null;
		
		Field field = fields.get(0);
		try {
			if (forceAccess) field.setAccessible(true);
			return field.get(obj);
		} catch (IllegalAccessException ex) {
			throw new JuRuntimeException("Couldn't access field " + field.getName(), ex);
		}
	}
	
	private static <A extends Annotation> List<A> toAnnotationList(List<AnnotationInfo<A>> list) {
		List<A> annos = new ArrayList<>();
		
		for (AnnotationInfo<A> annoInfo : list) {
			annos.add(annoInfo.getAnnotation());
		}
		
		return annos;
	}
	
	/**
	 * Gets all annotations of the specified type for the specified class, not including annotation declaration info.
	 * <p>
	 * If no annotation is found, an empty list is returned.
	 * <p>
	 * If includeSuperClassesAnnotations is true, all super classes of the class are searched for the
	 * specified annotation. The annotations are returned in order class (first) to super classes (Object last).
	 * @param clazz Class to search for annotations
	 * @param annotationClass Type of annotation
	 * @param includeSuperClassesAnnotations If true, super classes are searched as well. If false, we can only get 0 or 1 results
	 * @return List of annotations, in order of clazz (first) to super class (Object is last)
	 */
	public static <A extends Annotation> List<A> getAnnotations(
			Class<?> clazz, 
			Class<A> annotationClass, 
			boolean includeSuperClassesAnnotations) {
		
		return toAnnotationList(getAnnotationsWithInfo(clazz, annotationClass, includeSuperClassesAnnotations));
	}
	
	/**
	 * Gets all annotations of the specified type for the specified class.
	 * <p>
	 * If no annotation is found, an empty list is returned.
	 * <p>
	 * If includeSuperClassesAnnotations is true, all super classes of the class are searched for the
	 * specified annotation. The annotations are returned in order class (first) to super classes (Object last).
	 * @param clazz Class to search for annotations
	 * @param annotationClass Type of annotation
	 * @param includeSuperClassesAnnotations If true, super classes are searched as well. If false, we can only get 0 or 1 results
	 * @return List of annotations, in order of clazz (first) to super class (Object is last). Includes infos on where the annotation
	 * was defined (on which class / method).
	 */
	public static <A extends Annotation> List<AnnotationInfo<A>> getAnnotationsWithInfo(
			Class<?> clazz, 
			Class<A> annotationClass, 
			boolean includeSuperClassesAnnotations) {
		
		List<AnnotationInfo<A>> annos = new ArrayList<>();
		
		Class<?> c = clazz;
		do {
			A anno = c.getAnnotation(annotationClass);
			if (anno != null) annos.add(new AnnotationInfo<A>(anno, annotationClass, c, null));
			
			c = c.getSuperclass();
		} while (includeSuperClassesAnnotations && c != null);
		
		return annos;
	}
	
	/**
	 * Gets annotations of the specified type for the specified method. Doesn't include annotation declaration info.
	 * <p>
	 * If no annotation is found, an empty list is returned.
	 * <p>
	 * If includeOverriddenMethods and includeClassAnnotations are both false, we can get either 1
	 * or 2 results.
	 * <p>
	 * Annotations are returned in the following order:
	 * <ol>
	 *   <li>method</li>
	 *   <li>overridden method(s)</li>
	 *   <li>declaring class</li>
	 *   <li>super classes of declaring class</li>
	 * </ol>
	 * @param method Method to get annotations for
	 * @param annotationClass Class of the annotation to get
	 * @param includeOverriddenMethods If true, overridden methods of super classes are also searched
	 * @param includeClassAnnotations If true, the declaring class of the method is also searched
	 * @param includeSuperClassesAnnotations If true, super classes of the declaring class are also searched. This
	 * parameter is ignored if includeClassAnnotations is false.
	 * @return List of annotations. If no annotations were found, an empty list is returned.
	 */
	public static <A extends Annotation> List<A> getAnnotations(
			Method method,
			Class<A> annotationClass,
			boolean includeOverriddenMethods,
			boolean includeClassAnnotations,
			boolean includeSuperClassesAnnotations) {
		
		return toAnnotationList(getAnnotationsWithInfo(method, annotationClass
				, includeOverriddenMethods, includeClassAnnotations, includeSuperClassesAnnotations));
	}
	
	/**
	 * Gets annotations of the specified type for the specified method.
	 * <p>
	 * If no annotation is found, an empty list is returned.
	 * <p>
	 * If includeOverriddenMethods and includeClassAnnotations are both false, we can get either 1
	 * or 2 results.
	 * <p>
	 * Annotations are returned in the following order:
	 * <ol>
	 *   <li>method</li>
	 *   <li>overridden method(s)</li>
	 *   <li>declaring class</li>
	 *   <li>super classes of declaring class</li>
	 * </ol>
	 * @param method Method to get annotations for
	 * @param annotationClass Class of the annotation to get
	 * @param includeOverriddenMethods If true, overridden methods of super classes are also searched
	 * @param includeClassAnnotations If true, the declaring class of the method is also searched
	 * @param includeSuperClassesAnnotations If true, super classes of the declaring class are also searched. This
	 * parameter is ignored if includeClassAnnotations is false.
	 * @return List of annotations. If no annotations were found, an empty list is returned. Includes information on where the annotation
	 * was declared (on which class / method).
	 */
	public static <A extends Annotation> List<AnnotationInfo<A>> getAnnotationsWithInfo(
			Method method,
			Class<A> annotationClass,
			boolean includeOverriddenMethods,
			boolean includeClassAnnotations,
			boolean includeSuperClassesAnnotations) {
		
		List<AnnotationInfo<A>> annos = new ArrayList<>();
		
		Method m = method;
		do {
			A anno = m.getAnnotation(annotationClass);
			if (anno != null) annos.add(new AnnotationInfo<A>(anno, annotationClass, m.getDeclaringClass(), m));
			
			// Check if the method is overriding a method in the super class
			Class<?> superClass = m.getDeclaringClass().getSuperclass();
			if (superClass != null) {
				try {
					m = superClass.getMethod(m.getName(), m.getParameterTypes());
				} catch (NoSuchMethodException ex) {
					m = null;
				}
			} else {
				m = null;
			}
		} while (includeOverriddenMethods && m != null);
		
		if (includeClassAnnotations) {
			List<AnnotationInfo<A>> classAnnos = ReflectUtils.getAnnotationsWithInfo(method.getDeclaringClass(), annotationClass, includeSuperClassesAnnotations);
			annos.addAll(classAnnos);
		}
		
		return annos;
	}
	
	/**
	 * Helper class that encapsulates an Annotation and information on where it was declared (on which class / method).
	 * <p>
	 * Overrides toString that will return the Annotation type along with the declaring class and (if any) method
	 * @author Martin Meyer <martin.meyer@inftec.ch>
	 *
	 * @param <A> Annotation Type
	 */
	public static class AnnotationInfo<A extends Annotation> implements Serializable {
		private A annotation;
		private String annotationClassName;
		private String className;
		private String methodName;
		
		private AnnotationInfo(A annotation, Class<A> annotationClass, Class<?> clazz, Method method) {
			this.annotation = annotation;
			this.annotationClassName = annotationClass.getName();
			this.className = clazz.getName();
			this.methodName = method == null ? null : method.getName();
		}
		
		/**
		 * Gets the actual annotation.
		 * @return Annotation
		 */
		public A getAnnotation() {
			return this.annotation;
		}
		
		/**
		 * Gets the name of the class the annotation is declared on. If it's a method annotation,
		 * the class of the method is returned.
		 * @return Name of the declaring class
		 */
		public String getDeclaringClassName() {
			return this.className;
		}
		
		/**
		 * Gets the name of the method the annotation is declared on. If it's a class annotation, null
		 * is returned.
		 * @return Name of the declaring method or null for class annotations
		 */
		public String getDeclaringMethodName() {
			return this.methodName;
		}
		
		@Override
		public String toString() {
			String s = String.format("%s (%s", this.annotationClassName, this.className);
			if (this.methodName == null) {
				s += ")";
			} else {
				s += String.format(".%s())", this.methodName);
			}
			return s;
		}
	}
}
