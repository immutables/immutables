package org.immutables.value.processor.meta;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import com.google.common.base.Function;
import com.google.common.base.Optional;

class DefaultAnnotations {
	private DefaultAnnotations() {}

	private static final Map<String, AnnotationHandler<?>> handlers = new HashMap<>();
	static {
		handlers.put(DefaultIntMirror.QUALIFIED_NAME, new AnnotationHandler<>(TypeKind.INT, Integer.class, DefaultIntMirror::from, DefaultIntMirror::value));
		handlers.put(DefaultLongMirror.QUALIFIED_NAME, new AnnotationHandler<>(TypeKind.LONG, Long.class, DefaultLongMirror::from, DefaultLongMirror::value));
		handlers.put(DefaultCharMirror.QUALIFIED_NAME, new AnnotationHandler<>(TypeKind.CHAR, Character.class, DefaultCharMirror::from, DefaultCharMirror::value));
		handlers.put(DefaultDoubleMirror.QUALIFIED_NAME, new AnnotationHandler<>(TypeKind.DOUBLE, Double.class, DefaultDoubleMirror::from, DefaultDoubleMirror::value));
		handlers.put(DefaultFloatMirror.QUALIFIED_NAME, new AnnotationHandler<>(TypeKind.FLOAT, Float.class, DefaultFloatMirror::from, DefaultFloatMirror::value));
		handlers.put(DefaultBooleanMirror.QUALIFIED_NAME, new AnnotationHandler<>(TypeKind.BOOLEAN, Boolean.class, DefaultBooleanMirror::from, DefaultBooleanMirror::value));
		handlers.put(DefaultStringMirror.QUALIFIED_NAME, 	new AnnotationHandler<DefaultStringMirror>(TypeKind.NULL, String.class, DefaultStringMirror::from, DefaultStringMirror::value));
		// NULL cannot occur and will never match, so for a String, we will always check declared type object String, a bit illogical,
		// but works for all primitive+wrapper types and for String in such ad hoc interpretation
	}

	static @Nullable Object extractConstantDefault(Reporter reporter, Element element,
			TypeMirror type) {
		@Nullable Object defaultValue = null;
		for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
			String qualifiedName = ((TypeElement) mirror.getAnnotationType().asElement())
					.getQualifiedName().toString();

			@Nullable AnnotationHandler<?> handler = handlers.get(qualifiedName);
			if (handler == null) continue;
			defaultValue = handler.extractOrReport(reporter, mirror, element, type);
		}
		return defaultValue;
	}

	private static class AnnotationHandler<M> {
		private final TypeKind typeKind;
		private final Function<AnnotationMirror, Optional<M>> toMirror;
		private final Function<M, Object> extractValue;
		private final String orDeclaredType;

		AnnotationHandler(TypeKind typeKind, Class<?> orDeclaredType, Function<AnnotationMirror, Optional<M>> toMirror, Function<M, Object> extractValue) {
			this.typeKind = typeKind;
			this.toMirror = toMirror;
			this.extractValue = extractValue;
			this.orDeclaredType = orDeclaredType.getCanonicalName();
		}

		boolean typeApplies(TypeMirror type) {
			return typeKind == type.getKind()
					|| (type.getKind() == TypeKind.DECLARED
						&& ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName()
						.contentEquals(orDeclaredType));
		}

		@Nullable
		Object extractOrReport(Reporter reporter, AnnotationMirror mirror, Element element,
				TypeMirror type) {
			if (typeApplies(type)) {
				return toMirror.apply(mirror).transform(extractValue).orNull();
			}

			reporter.withAnnotation(mirror).error("Annotation %s is not applicable to '%s' of type '%s'",
					mirror.getAnnotationType().asElement().getSimpleName(),
					element.getSimpleName(),
					type);

			return null;
		}
	}
}
