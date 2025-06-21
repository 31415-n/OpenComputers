package li.cil.oc.util;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

public final class OCObfuscationReflectionHelper {
	private OCObfuscationReflectionHelper() {

	}

	@SuppressWarnings("unchecked")
	public static <T, E> T getPrivateValue(Class<? super E> classToAccess, @Nullable E instance, String fieldName) {
		try {
			Field field = classToAccess.getDeclaredField(fieldName);
			field.setAccessible(true);
			return (T) field.get(instance);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get private value", e);
		}
	}

	public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, String fieldName) {
		try {
			Field field = classToAccess.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(instance, value);
		} catch (Exception e) {
			throw new RuntimeException("Failed to set private value", e);
		}
	}
}
