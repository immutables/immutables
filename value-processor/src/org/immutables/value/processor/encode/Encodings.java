/*
   Copyright 2016 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value.processor.encode;

import com.google.common.base.Predicates;
import java.util.Arrays;
import com.google.common.collect.Iterables;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.generator.Naming;
import org.immutables.generator.Naming.Preference;
import org.immutables.generator.SourceExtraction;
import org.immutables.generator.Templates;
import org.immutables.value.processor.encode.Code.Term;
import org.immutables.value.processor.encode.EncodedElement.Param;
import org.immutables.value.processor.encode.Type.Defined;
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
		private final Type.Factory types = new Type.Producer();
		private final TypeExtractor typesReader;
		private final SourceMapper sourceMapper;
		private final Set<String> memberNames = new HashSet<>();

		final String name;
		final String $$package;

		List<String> typeParams = new ArrayList<>();

		@Nullable
		EncodedElement impl;
		@Nullable
		EncodedElement stringify;

		List<EncodedElement> fields = new ArrayList<>();
		List<EncodedElement> expose = new ArrayList<>();
		List<EncodedElement> copy = new ArrayList<>();
		List<EncodedElement> helpers = new ArrayList<>();

		@Nullable
		EncodedElement init;

		List<EncodedElement> builderFields = new ArrayList<>();
		List<EncodedElement> builderInits = new ArrayList<>();
		List<EncodedElement> builderHelpers = new ArrayList<>();

		@Nullable
		EncodedElement build;

		@Nullable
		String typeBuilderName;

		final Linkage linkage;
		final SourceExtraction.Imports imports;

		final Iterable<EncodedElement> allElements;
		final Set<String> nonlinkedImports;

		Encoding(TypeElement type) {
			if (type.getKind() != ElementKind.CLASS || type.getNestingKind() != NestingKind.TOP_LEVEL) {
				reporter.withElement(type).error("Encoding type '%s' should be top-level class", type.getSimpleName());
			}

			this.$$package = processing().getElementUtils().getPackageOf(type).getQualifiedName().toString();
			this.name = type.getSimpleName().toString();

			CharSequence source = SourceExtraction.extract(processing(), type);
			if (source.length() == 0) {
				reporter.withElement(type)
						.error("No source code can be extracted for @Encoding class. Unsupported compilation mode");
			}

			this.imports = SourceExtraction.importsFrom(source);
			this.sourceMapper = new SourceMapper(source);
			this.typesReader = new TypeExtractor(types, type);

			addTypeParameters(type);

			for (Element e : type.getEnclosedElements()) {
				processMember(e);
			}

			this.allElements = Iterables.concat(
					Arrays.asList(
							Iterables.filter(
									Arrays.asList(
											impl,
											stringify,
											init,
											build), Predicates.notNull()),
							fields,
							expose,
							copy,
							helpers,
							builderFields,
							builderHelpers,
							builderInits));

			this.linkage = new Linkage();
			this.nonlinkedImports = nonlinkedImports();
		}

		private Set<String> nonlinkedImports() {
			Set<String> lines = new LinkedHashSet<>();
			for (String a : imports.all) {
				if (a.startsWith("static ") || a.endsWith(".*")) {
					lines.add(a);
				}
			}
			return lines;
		}

		private void addTypeParameters(TypeElement type) {
			for (String n : typesReader.parameters.names()) {
				List<Defined> upperBounds = typesReader.parameters.variable(n).upperBounds;
				if (!upperBounds.isEmpty()) {
					reporter.withElement(type)
							.warning("Encoding type '%s' has type parameter <%s extends %s> and it's bounds will be ignored"
									+ " as they are not yet adequately supported or ever will.",
									type.getSimpleName(),
									n,
									Joiner.on(" & ").join(upperBounds));
				}
				this.typeParams.add(n);
			}
		}

		private void processMember(Element member) {
			if ((member.getKind() == ElementKind.FIELD
					|| member.getKind() == ElementKind.METHOD)
					&& !memberNames.add(memberPath(member))) {
				reporter.withElement(member)
						.warning("Duplicate member name '%s'. Encoding has limitation so that any duplicate method names are not supported,"
								+ " even when allowed by JLS: methods cannot have overloads here."
								+ " @Encoding.Naming annotation could be used so that actually generated methods might have the same name"
								+ " if they are not conflicting as per JLS overload rules.",
								member.getSimpleName());
				return;
			}

			if (member.getKind() == ElementKind.FIELD) {
				if (processField((VariableElement) member)) return;
			}
			if (member.getKind() == ElementKind.METHOD) {
				if (processMethod((ExecutableElement) member)) return;
			}
			if (member.getKind() == ElementKind.CLASS) {
				if (processClass((TypeElement) member)) return;
			}
			if (member.getKind() != ElementKind.INSTANCE_INIT) {
				reporter.withElement(member)
						.warning("Unrecognized encoding member '%s' will be ignored", member.getSimpleName());
			}
		}

		private boolean processField(VariableElement field) {
			if (ImplMirror.isPresent(field)) {
				return processImplField(field);
			}
			return processAuxField(field);
		}

		private boolean processAuxField(VariableElement field) {
			List<Term> expression = sourceMapper.getExpression(memberPath(field));

			if (expression.isEmpty() || !field.getModifiers().contains(Modifier.FINAL)) {
				reporter.withElement(field)
						.error("Auxiliary field '%s' have to be final and initialized to some value,"
								+ " possibly derived from @Encoding.Impl field (if it's not static)",
								field.getSimpleName());

				return true;
			}

			if (NamingMirror.isPresent(field)) {
				reporter.withElement(field)
						.annotationNamed(NamingMirror.simpleName())
						.warning("Auxiliary field '%s' have naming annotation which is ignored,"
								+ " a field name is always directly derived from the attribute name",
								field.getSimpleName());
			}

			fields.add(new EncodedElement.Builder()
					.name(field.getSimpleName().toString())
					.type(typesReader.get(field.asType()))
					.naming(Naming.identity())
					.addAllTags(inferTags(field, "field"))
					.addAllCode(expression)
					.build());

			return true;
		}

		private boolean processImplField(VariableElement field) {
			if (impl != null) {
				reporter.withElement(field)
						.error("@Encoding.Impl duplicate field '%s'. Cannot have more than one implementation field",
								field.getSimpleName());
				return true;
			}

			if (field.getModifiers().contains(Modifier.STATIC)) {
				reporter.withElement(field)
						.error("@Encoding.Impl field '%s' cannot be static",
								field.getSimpleName());
				return true;
			}

			if (NamingMirror.isPresent(field)) {
				reporter.withElement(field)
						.annotationNamed(NamingMirror.simpleName())
						.warning("@Encoding.Impl field '%s' have naming annotation which is ignored,"
								+ " a field name is always directly derived from the attribute name",
								field.getSimpleName());
			}

			this.impl = new EncodedElement.Builder()
					.name(field.getSimpleName().toString())
					.type(typesReader.get(field.asType()))
					.addTags("impl", "final", "private")
					.naming(Naming.identity())
					.addAllCode(sourceMapper.getExpression(memberPath(field)))
					.build();

			return true;
		}

		private boolean processMethod(ExecutableElement method) {
			if (ExposeMirror.isPresent(method)) {
				return processExposeMethod(method);
			}
			if (StringifyMirror.isPresent(method)) {
				return processStringifyMethod(method);
			}
			if (CopyMirror.isPresent(method)) {
				return processCopyMethod(method);
			}
			if (InitMirror.isPresent(method)) {
				return processInitMethod(method);
			}
			return processHelperMethod(method);
		}

		private boolean processHelperMethod(ExecutableElement method) {
			return processGenericEncodedMethod(method, helpers, "helper");
		}

		private boolean processCopyMethod(ExecutableElement method) {
			if (method.getModifiers().contains(Modifier.STATIC)) {
				reporter.withElement(method)
						.error("@Encoding.Expose method '%s' cannot be static",
								method.getSimpleName());

				return true;
			}
			return processGenericEncodedMethod(method, copy, "copy");
		}

		private boolean processInitMethod(ExecutableElement method) {
			if (init != null) {
				reporter.withElement(method)
						.error("@Encoding.Init duplicate method '%s'. Cannot have more than one init method",
								method.getSimpleName());
				return true;
			}

			if (method.getModifiers().contains(Modifier.STATIC)) {
				reporter.withElement(method)
						.error("@Encoding.Init method '%s' cannot have static modifier, but in fact should act like static one."
								+ " This is for consistent access to type parameters declared in Encoding type.",
								method.getSimpleName());

				return true;
			}

			if (!method.getThrownTypes().isEmpty()) {
				reporter.withElement(method)
						.error("@Encoding.Init method '%s' cannot have throws declaration",
								method.getSimpleName());
				return true;
			}

			if (!method.getTypeParameters().isEmpty()) {
				reporter.withElement(method)
						.error("@Encoding.Init method '%s' cannot have type parameters",
								method.getSimpleName());
				return true;
			}

			if (NamingMirror.isPresent(method)) {
				reporter.withElement(method)
						.annotationNamed(NamingMirror.simpleName())
						.warning("@Encoding.Init method '%s' have naming annotation which is ignored."
								+ " Init is not exposed as a standalone method.",
								method.getSimpleName());
			}

			this.init = new EncodedElement.Builder()
					.name(method.getSimpleName().toString())
					.type(typesReader.get(method.getReturnType()))
					.naming(Naming.identity())
					.addAllCode(sourceMapper.getBlock(memberPath(method)))
					.build();

			return false;
		}

		private boolean processStringifyMethod(ExecutableElement method) {
			if (method.getModifiers().contains(Modifier.STATIC)) {
				reporter.withElement(method)
						.error("@Encoding.Expose method '%s' cannot be static",
								method.getSimpleName());

				return true;
			}

			if (!method.getTypeParameters().isEmpty()) {
				reporter.withElement(method)
						.error("@Encoding.Stringify method '%s' cannot have type parameters",
								method.getSimpleName());
				return true;
			}

			if (!method.getThrownTypes().isEmpty()) {
				reporter.withElement(method)
						.error("@Encoding.Stringify method '%s' cannot have throws declaration",
								method.getSimpleName());
				return true;
			}

			if (stringify != null) {
				reporter.withElement(method)
						.error("@Encoding.Stringify duplicate method '%s'. Cannot have more than one stringify method",
								method.getSimpleName());
				return true;
			}

			if (NamingMirror.isPresent(method)) {
				reporter.withElement(method)
						.annotationNamed(NamingMirror.simpleName())
						.warning("@Encoding.Stringify method '%s' have naming annotation which is ignored,"
								+ " Stringify is used as part of toString() invokation.",
								method.getSimpleName());
			}

			if (!method.getParameters().isEmpty()) {
				reporter.withElement(method)
						.error("@Encoding.Stringify method '%s' have parameters which is illegal for stringify."
								+ " Stringify is used as part of toString() invokation.",
								method.getSimpleName());

				return true;
			}

			Type type = typesReader.get(method.getReturnType());
			if (type != Type.STRING) {
				reporter.withElement(method)
						.error("@Encoding.Stringify method '%s' have return type other than java.lang.String, which is not allowed",
								method.getSimpleName());

				return false;
			}

			this.stringify = new EncodedElement.Builder()
					.name(method.getSimpleName().toString())
					.type(type)
					.naming(Naming.identity())
					.addAllCode(sourceMapper.getBlock(memberPath(method)))
					.build();

			return true;
		}

		private boolean processExposeMethod(ExecutableElement method) {
			if (NamingMirror.isPresent(method)) {
				reporter.withElement(method)
						.annotationNamed(NamingMirror.simpleName())
						.warning("@Encoding.Expose method '%s' have naming annotation which is ignored,"
								+ " an accessor name is configured using @Value.Style(get) patterns.",
								method.getSimpleName());
			}

			if (!method.getTypeParameters().isEmpty()) {
				reporter.withElement(method)
						.error("@Encoding.Expose method '%s' cannot have type parameters",
								method.getSimpleName());
				return true;
			}

			if (!method.getThrownTypes().isEmpty()) {
				reporter.withElement(method)
						.error("@Encoding.Expose method '%s' cannot have throws declaration",
								method.getSimpleName());
				return true;
			}

			if (!method.getParameters().isEmpty()) {
				reporter.withElement(method)
						.error("@Encoding.Expose method '%s' have parameters which is illegal for accessor."
								+ " Use @Encoding.Derive to create helper accessors that can have parameters.",
								method.getSimpleName());
				return true;
			}

			if (method.getModifiers().contains(Modifier.STATIC)) {
				reporter.withElement(method)
						.error("@Encoding.Expose method '%s' cannot be static",
								method.getSimpleName());

				return true;
			}

			expose.add(new EncodedElement.Builder()
					.name(method.getSimpleName().toString())
					.type(typesReader.get(method.getReturnType()))
					.addTags("expose")
					.naming(Naming.identity())
					.addAllCode(sourceMapper.getBlock(memberPath(method)))
					.build());

			return true;
		}

		private boolean processGenericEncodedMethod(
				ExecutableElement method,
				List<EncodedElement> collection,
				String... additionalTags) {
			EncodedElement.Builder builder = new EncodedElement.Builder();
			TypeExtractor typesReader = processTypeParameters(method, builder);

			collection.add(builder
					.name(method.getSimpleName().toString())
					.type(typesReader.get(method.getReturnType()))
					.naming(inferNaming(method))
					.addAllTags(inferTags(method, additionalTags))
					.addAllParams(getParameters(typesReader, method))
					.addAllCode(sourceMapper.getBlock(memberPath(method)))
					.addAllThrown(typesReader.getDefined(method.getThrownTypes()))
					.build());

			return true;
		}

		private TypeExtractor processTypeParameters(ExecutableElement method, EncodedElement.Builder builder) {
			boolean isStatic = method.getModifiers().contains(Modifier.STATIC);

			TypeExtractor typesReader = isStatic
					? new TypeExtractor(types, method)
					: this.typesReader;

			for (TypeParameterElement p : method.getTypeParameters()) {
				String name = p.getSimpleName().toString();
				ImmutableList<Defined> bounds = typesReader.getDefined(p.getBounds());
				if (!isStatic) {
					typesReader = typesReader.withParameter(name, bounds);
				}
				builder.addTypeParams(new EncodedElement.TypeParam.Builder()
						.name(name)
						.addAllBounds(bounds)
						.build());
			}

			return typesReader;
		}

		private List<Param> getParameters(TypeExtractor typesReader, ExecutableElement method) {
			List<Param> result = new ArrayList<>();
			for (VariableElement v : method.getParameters()) {
				result.add(Param.of(
						v.getSimpleName().toString(),
						typesReader.get(v.asType())));
			}
			return result;
		}

		private Naming inferNaming(ExecutableElement method) {
			if (method.getModifiers().contains(Modifier.STATIC)) {
				return Naming.identity();
			}
			Optional<NamingMirror> namingAnnotation = NamingMirror.find(method);
			return Naming.from(
					namingAnnotation.isPresent()
							? namingAnnotation.get().value()
							: method.getSimpleName().toString())
					.requireNonConstant(Preference.SUFFIX);
		}

		private String memberPath(Element member) {
			LinkedList<String> names = new LinkedList<>();
			for (Element e = member; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
				names.addFirst(e.getSimpleName().toString());
			}
			return Joiner.on('.').join(names);
		}

		private boolean processClass(TypeElement type) {
			if (BuilderMirror.isPresent(type)) {
				this.typeBuilderName = type.getSimpleName().toString();

				if (!type.getTypeParameters().isEmpty()
						|| type.getModifiers().contains(Modifier.STATIC)) {
					reporter.withElement(type)
							.error("@Encoding.Builder class '%s' should not be static or have type parameters."
									+ "It is only for that to enforce visibility of enclosing type parameters"
									+ " and simpler signature of Builder itself, otherwise it should not access outer"
									+ " encoding instance as it will be unavailable when encoding will be compiled in."
									+ " Static helper methods and constants can be accessed from enclosing Encoding type.",
									type.getSimpleName());

					return true;
				}

				for (Element member : type.getEnclosedElements()) {
					if ((member.getKind() == ElementKind.FIELD
							|| member.getKind() == ElementKind.METHOD)
							&& !memberNames.add(memberPath(member))) {
						reporter.withElement(member)
								.warning(memberPath(member)
										+ ": Duplicate builder member name '%s'."
										+ " Encoding has limitation so that any duplicate method names are not supported,"
										+ " even when allowed by JLS: methods cannot have overloads here."
										+ " @Encoding.Naming annotation could be used so that actually generated methods might have the same name"
										+ " if they are not conflicting as per JLS overload rules.",
										member.getSimpleName());
						continue;
					}

					if (member.getKind() == ElementKind.FIELD) {
						if (processBuilderField((VariableElement) member)) continue;
					}

					if (member.getKind() == ElementKind.METHOD) {
						if (processBuilderMethod((ExecutableElement) member)) continue;
					}

					if (member.getKind() != ElementKind.INSTANCE_INIT) {
						reporter.withElement(member)
								.warning("Unrecognized Builder member '%s' will be ignored", member.getSimpleName());
					}
				}
				return true;
			}
			return false;
		}

		private boolean processBuilderField(VariableElement field) {
			if (NamingMirror.isPresent(field)) {
				reporter.withElement(field)
						.annotationNamed(NamingMirror.simpleName())
						.warning("@Enclosing.Builder field '%s' have naming annotation which is ignored,"
								+ " an builder field name is derived automatically.",
								field.getSimpleName());
			}

			builderFields.add(new EncodedElement.Builder()
					.type(typesReader.get(field.asType()))
					.name(field.getSimpleName().toString())
					.naming(Naming.identity())
					.addAllTags(inferTags(field, "field", "builder"))
					.addAllCode(sourceMapper.getExpression(memberPath(field)))
					.build());

			return true;
		}

		private boolean processBuilderMethod(ExecutableElement method) {
			if (BuildMirror.isPresent(method)) {
				return processBuilderBuildMethod(method);
			}
			if (InitMirror.isPresent(method)) {
				return processBuilderInitMethod(method);
			}
			return processBuilderHelperMethod(method);
		}

		private boolean processBuilderBuildMethod(ExecutableElement method) {
			Type type = typesReader.get(method.getReturnType());

			if (impl != null && impl.type().equals(type)) {
				// this is actually best-effort check
				reporter.withElement(method)
						.warning("@Encoding.Build method '%s' return type does not match @Encoding.Impl field type.",
								method.getSimpleName());
			}

			if (NamingMirror.isPresent(method)) {
				reporter.withElement(method)
						.annotationNamed(NamingMirror.simpleName())
						.warning("@Encoding.Build method '%s' have naming annotation which is ignored."
								+ " This method is used internally to build instances.",
								method.getSimpleName());
			}

			if (!method.getTypeParameters().isEmpty()) {
				reporter.withElement(method)
						.error("@Encoding.Build method '%s' cannot have type parameters",
								method.getSimpleName());
				return true;
			}

			if (!method.getThrownTypes().isEmpty()) {
				reporter.withElement(method)
						.error("@Encoding.Build method '%s' cannot have throws declaration",
								method.getSimpleName());
				return true;
			}

			if (!method.getParameters().isEmpty()) {
				reporter.withElement(method)
						.error("@Encoding.Build method '%s' have parameters which is illegal for build method.",
								method.getSimpleName());
				return true;
			}

			this.build = new EncodedElement.Builder()
					.name(method.getSimpleName().toString())
					.type(typesReader.get(method.getReturnType()))
					.addTags("build", "builder")
					.naming(Naming.identity())
					.addAllCode(sourceMapper.getBlock(memberPath(method)))
					.build();

			return true;
		}

		private boolean processBuilderInitMethod(ExecutableElement method) {
			return processGenericEncodedMethod(method, builderInits, "init", "builder");
		}

		private boolean processBuilderHelperMethod(ExecutableElement method) {
			return processGenericEncodedMethod(method, builderFields, "helper", "builder");
		}

		private List<String> inferTags(Element member, String... additionalTags) {
			List<String> tags = new ArrayList<>();
			if (member.getModifiers().contains(Modifier.STATIC)) {
				tags.add("static");
			}
			if (member.getModifiers().contains(Modifier.PRIVATE)) {
				tags.add("private");
			}
			if (member.getModifiers().contains(Modifier.FINAL)) {
				tags.add("final");
			}
			for (String t : additionalTags) {
				tags.add(t);
			}
			return tags;
		}

		class Linkage implements Function<EncodedElement, String> {
			private final Set<String> staticContext = new LinkedHashSet<>();
			private final Set<String> instanceContext = new LinkedHashSet<>();
			private final Set<String> builderContext = new LinkedHashSet<>();

			Linkage() {
				addStaticMembers(staticContext);
				addStaticMembers(instanceContext);
				addStaticMembers(builderContext);
				addTypeParameters(instanceContext);
				addTypeParameters(builderContext);
				addInstanceMembers(instanceContext);
				addBuilderMembers(builderContext);
			}

			private void addTypeParameters(Set<String> context) {
				context.addAll(typeParams);
			}

			private void addBuilderMembers(Set<String> context) {
				for (EncodedElement e : allElements) {
					if (e.tags().contains("builder")) {
						context.add(e.name());
					}
				}
			}

			private void addInstanceMembers(Set<String> context) {
				for (EncodedElement e : allElements) {
					if (!e.tags().contains("static") && !e.tags().contains("builder")) {
						context.add(e.name());
					}
				}
			}

			private void addStaticMembers(Set<String> context) {
				for (EncodedElement e : allElements) {
					if (e.tags().contains("static")) {
						context.add(e.name());
					}
				}
			}

			@Override
			public String apply(EncodedElement element) {
				Code.Linker linker = linkerFor(element);
				return Code.join(linker.resolve(element.code()));
			}

			private Code.Linker linkerFor(EncodedElement element) {
				if (element.tags().contains("builder")) {
					return new Code.Linker(imports.classes, builderContext);
				}
				if (element.tags().contains("static")) {
					return new Code.Linker(imports.classes, staticContext);
				}
				return new Code.Linker(imports.classes, instanceContext);
			}
		}
	}
}
