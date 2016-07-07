package org.immutables.value.processor.encode;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.generator.SourceExtraction;
import org.immutables.generator.Templates;
import org.immutables.value.processor.meta.Reporter;

@Generator.Template
public abstract class Encodings extends AbstractTemplate {
	@Generator.Typedef
	Encoding Encoding;

	public abstract Templates.Invokable generate();

	final Reporter reporter = Reporter.from(processing());
	final List<Encoding> encodings = new ArrayList<>();

	Encodings() {
		for (TypeElement a : annotations()) {
			for (TypeElement t : ElementFilter.typesIn(round().getElementsAnnotatedWith(a))) {
				encodings.add(new Encoding(t));
			}
		}
	}

	class Encoding {
		private final CharSequence source;

		private Object impl;

		Encoding(TypeElement type) {
			if (type.getKind() != ElementKind.CLASS || type.getNestingKind() != NestingKind.TOP_LEVEL) {
				reporter.withElement(type).error("Encoding type '%s' should be top-level class", type.getSimpleName());
			}
			source = SourceExtraction.extract(processing(), type);
			if (source.length() == 0) {
				reporter.withElement(type)
						.error("No source code can be extracted @Encoding. Probably, compilation mode is not supported");
			}

			for (Element e : type.getEnclosedElements()) {
				processMember(e);
			}
		}

		private void processMember(Element member) {
			if (member.getKind() == ElementKind.FIELD) {
				if (processField((VariableElement) member))
					return;
			}
			if (member.getKind() == ElementKind.METHOD) {
				if (processMethod((ExecutableElement) member))
					return;
			}
			if (member.getKind() == ElementKind.CLASS) {
				if (processClass((TypeElement) member))
					return;
			}

			reporter.withElement(member)
					.warning("Unrecognized element '%s' will be ignored", member.getSimpleName());
		}

		private boolean processMethod(ExecutableElement method) {
			return false;
		}

		private boolean processField(VariableElement field) {
			if (ImplMirror.isPresent(field)) {
				if (impl != null) {
					reporter.withElement(field).error(
							"@Encoding.Impl duplicate field '%s' cannot have more than one implementation field",
							field.getSimpleName());
					return true;
				}
				if (field.getModifiers().contains(Modifier.STATIC)) {
					reporter.withElement(field).error(
							"@Encoding.Impl field '%s' cannot be static",
							field.getSimpleName());
					return true;
				}
			}
			return false;
		}

		private boolean processClass(TypeElement type) {
			if (type.getSimpleName().contentEquals("Builder")) {

			}
			return false;
		}
	}

	class Impl {
		Impl() {

		}
	}
}
