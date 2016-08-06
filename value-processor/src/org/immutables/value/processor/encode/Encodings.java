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

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.Parameterizable;
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
import org.immutables.value.processor.encode.Code.Binding;
import org.immutables.value.processor.encode.Code.Term;
import org.immutables.value.processor.encode.EncodedElement.Param;
import org.immutables.value.processor.encode.EncodedElement.Tag;
import org.immutables.value.processor.encode.Type.Array;
import org.immutables.value.processor.encode.Type.Defined;
import org.immutables.value.processor.encode.Type.Primitive;
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
    EncodedElement toString;

    @Nullable
    EncodedElement hashCode;

    @Nullable
    EncodedElement equals;

    @Nullable
    EncodedElement from;

    @Nullable
    EncodedElement build;

    List<EncodedElement> fields = new ArrayList<>();
    List<EncodedElement> expose = new ArrayList<>();
    List<EncodedElement> copy = new ArrayList<>();
    List<EncodedElement> helpers = new ArrayList<>();

    List<EncodedElement> builderFields = new ArrayList<>();
    List<EncodedElement> builderInits = new ArrayList<>();
    List<EncodedElement> builderHelpers = new ArrayList<>();

    @Nullable
    String typeBuilderName;

    final Linkage linkage;
    final SourceExtraction.Imports imports;

    final Iterable<EncodedElement> allElements;
    final Set<String> nonlinkedImports;
    private final Type encodingSelfType;
    private String builderInitCopy;

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

      this.encodingSelfType = typesReader.get(type.asType());

      addTypeParameters(type);

      for (Element e : type.getEnclosedElements()) {
        processMember(e);
      }

      postValidate();

      this.allElements = Iterables.concat(
          Arrays.asList(
              Iterables.filter(
                  Arrays.asList(
                      impl,
                      from,
                      toString,
                      hashCode,
                      equals,
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

    private void postValidate() {
      for (EncodedElement e : copy) {
        if (!e.type().equals(impl.type())) {
          reporter.error("@Encoding.Copy methods must be declared to return implementation field type");
        }
      }
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
        Type.Variable v = typesReader.parameters.variable(n);
        if (!v.isUnbounded()) {
          reporter.withElement(type)
              .warning("Encoding type '%s' has type parameter <%s extends %s> and it's bounds will be ignored"
                  + " as they are not yet adequately supported or ever will",
                  type.getSimpleName(), v.name, v.upperBounds);
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
                + " if they are not conflicting as per JLS overload rules",
                member.getSimpleName());
        return;
      }

      if (member.getKind() == ElementKind.FIELD) {
        if (processField((VariableElement) member))
          return;
      }
      if (member.getKind() == ElementKind.METHOD) {
        if (!Ascii.isLowerCase(member.getSimpleName().charAt(0))) {
          reporter.withElement(member)
              .warning("Methods not starting with lowercase ascii letter might not work properly",
                  member.getSimpleName());
        }
        if (processMethod((ExecutableElement) member))
          return;
      }
      if (member.getKind() == ElementKind.CLASS) {
        if (processClass((TypeElement) member))
          return;
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
          .naming(inferNaming(field))
          .typeParameters(typesReader.parameters)
          .addAllTags(inferTags(field, Tag.FIELD))
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

      if (!field.getModifiers().contains(Modifier.PRIVATE)) {
        reporter.withElement(field)
            .error("@Encoding.Impl field '%s' must be private. Other auxiliary fields may be of whatever visibility,"
                + " but primary implementation field should be private",
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
          .addTags(Tag.IMPL, Tag.FINAL, Tag.PRIVATE, Tag.FIELD)
          .naming(Naming.identity())
          .typeParameters(typesReader.parameters)
          .addAllCode(sourceMapper.getExpression(memberPath(field)))
          .build();

      return true;
    }

    private boolean processMethod(ExecutableElement method) {
      if (method.getSimpleName().contentEquals("toString")) {
        return processToStringMethod(method);
      }
      if (method.getSimpleName().contentEquals("hashCode")) {
        return processHashCodeMethod(method);
      }
      if (method.getSimpleName().contentEquals("equals")) {
        return processEqualsMethod(method);
      }
      if (ExposeMirror.isPresent(method)) {
        return processExposeMethod(method);
      }
      if (CopyMirror.isPresent(method)) {
        return processCopyMethod(method);
      }
      if (InitMirror.isPresent(method)) {
        return processFromMethod(method);
      }
      return processHelperMethod(method);
    }

    private boolean processHelperMethod(ExecutableElement method) {
      return processGenericEncodedMethod(method, helpers, Tag.HELPER);
    }

    private boolean processCopyMethod(ExecutableElement method) {
      if (method.getModifiers().contains(Modifier.STATIC)) {
        reporter.withElement(method)
            .error("@Encoding.Copy method '%s' cannot be static",
                method.getSimpleName());

        return true;
      }
      if (impl != null && !typesReader.get(method.getReturnType()).equals(impl.type())) {
        reporter.withElement(method)
            .error("@Encoding.Copy method '%s' should be declared to return implementation field's type",
                method.getSimpleName());
      }
      return processGenericEncodedMethod(method, copy, Tag.COPY);
    }

    private boolean processFromMethod(ExecutableElement method) {
      if (from != null) {
        reporter.withElement(method)
            .error("@Encoding.Init duplicate method '%s'. Cannot have more than one init method",
                method.getSimpleName());
        return true;
      }

      if (!typeParams.equals(getTypeParameterNames(method))
          || !method.getModifiers().contains(Modifier.STATIC)) {
        reporter.withElement(method)
            .error("@Encoding.Init method '%s' should be static with"
                + " the same type parameters as encoding type: %s",
                method.getSimpleName(),
                typesReader.parameters);

        return true;
      }

      if (method.getParameters().size() != 1) {
        reporter.withElement(method)
            .error("@Encoding.Init method '%s' should take a single parameter"
                + " return type assignable to implementation field",
                method.getSimpleName());
        return true;
      }

      VariableElement parameter = method.getParameters().get(0);

      if (!method.getThrownTypes().isEmpty()) {
        reporter.withElement(method)
            .error("@Encoding.Init method '%s' cannot have throws declaration",
                method.getSimpleName());
        return true;
      }

      if (NamingMirror.isPresent(method)) {
        reporter.withElement(method)
            .annotationNamed(NamingMirror.simpleName())
            .warning("@Encoding.Init method '%s' have naming annotation which is ignored."
                + " Init is not exposed as a standalone method",
                method.getSimpleName());
      }

      this.from = new EncodedElement.Builder()
          .name(method.getSimpleName().toString())
          .type(typesReader.get(method.getReturnType()))
          .addTags(Tag.PRIVATE, Tag.FROM, Tag.STATIC)
          .naming(Naming.identity())
          .addParams(Param.of(parameter.getSimpleName().toString(), typesReader.get(parameter.asType())))
          .typeParameters(typesReader.parameters)
          .addAllCode(sourceMapper.getBlock(memberPath(method)))
          .build();

      return true;
    }

    private boolean processEqualsMethod(ExecutableElement method) {
      if (method.getParameters().size() != 1) {
        return false;
      }

      VariableElement parameter = method.getParameters().get(0);

      if (typesReader.get(method.getReturnType()) != Primitive.BOOLEAN
          || !typesReader.get(parameter.asType()).equals(encodingSelfType)) {
        reporter.withElement(method)
            .error("method '%s' should take a single parameter of encoding type %s and return boolean",
                method.getSimpleName(), encodingSelfType);
        return true;
      }

      if (method.getModifiers().contains(Modifier.STATIC)) {
        reporter.withElement(method)
            .error("method '%s' cannot be static",
                method.getSimpleName());

        return true;
      }

      if (!method.getTypeParameters().isEmpty()) {
        reporter.withElement(method)
            .error("method '%s' cannot have type parameters",
                method.getSimpleName());
        return true;
      }

      if (!method.getThrownTypes().isEmpty()) {
        reporter.withElement(method)
            .error("method '%s' cannot have throws declaration",
                method.getSimpleName());
        return true;
      }

      if (NamingMirror.isPresent(method)) {
        reporter.withElement(method)
            .annotationNamed(NamingMirror.simpleName())
            .warning("method '%s' have naming annotation which is ignored,"
                + " it is used as part of generated equals()",
                method.getSimpleName());
      }

      this.equals = new EncodedElement.Builder()
          .name(method.getSimpleName().toString())
          .type(Primitive.BOOLEAN)
          .addParams(Param.of(parameter.getSimpleName().toString(), encodingSelfType))
          .addTags(Tag.PRIVATE, Tag.EQUALS)
          .naming(inferNaming(method))
          .typeParameters(typesReader.parameters)
          .addAllCode(sourceMapper.getBlock(memberPath(method)))
          .build();

      return true;
    }

    private boolean processHashCodeMethod(ExecutableElement method) {
      if (!method.getParameters().isEmpty()) {
        // major checks are made by compiler which forces hashCode to properly override
        return false;
      }

      if (NamingMirror.isPresent(method)) {
        reporter.withElement(method)
            .annotationNamed(NamingMirror.simpleName())
            .warning("method '%s' have naming annotation which is ignored,"
                + " it is used as part of generated hashCode()",
                method.getSimpleName());
      }

      this.hashCode = new EncodedElement.Builder()
          .name(method.getSimpleName().toString())
          .type(Primitive.INT)
          .addTags(Tag.PRIVATE, Tag.HASH_CODE)
          .naming(inferNaming(method))
          .typeParameters(typesReader.parameters)
          .addAllCode(sourceMapper.getBlock(memberPath(method)))
          .build();

      return true;
    }

    private boolean processToStringMethod(ExecutableElement method) {
      if (!method.getParameters().isEmpty()) {
        // major checks are made by compiler which forces hashCode to properly override
        return false;
      }

      if (NamingMirror.isPresent(method)) {
        reporter.withElement(method)
            .annotationNamed(NamingMirror.simpleName())
            .warning("method '%s' have naming annotation which is ignored,"
                + " it is used as part of generated toString()",
                method.getSimpleName());
      }

      this.toString = new EncodedElement.Builder()
          .name(method.getSimpleName().toString())
          .type(Type.STRING)
          .addTags(Tag.PRIVATE, Tag.TO_STRING)
          .naming(inferNaming(method))
          .typeParameters(typesReader.parameters)
          .addAllCode(sourceMapper.getBlock(memberPath(method)))
          .build();

      return true;
    }

    private boolean processExposeMethod(ExecutableElement method) {
      if (NamingMirror.isPresent(method)) {
        reporter.withElement(method)
            .annotationNamed(NamingMirror.simpleName())
            .warning("@Encoding.Expose method '%s' have naming annotation which is ignored,"
                + " an accessor name is configured using @Value.Style(get) patterns",
                method.getSimpleName());
      }

      if (method.getModifiers().contains(Modifier.STATIC)) {
        reporter.withElement(method)
            .error("@Encoding.Expose method '%s' cannot be static",
                method.getSimpleName());

        return true;
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
                + " Use @Encoding.Derive to create helper accessors that can have parameters",
                method.getSimpleName());
        return true;
      }

      expose.add(new EncodedElement.Builder()
          .name(method.getSimpleName().toString())
          .type(typesReader.get(method.getReturnType()))
          .addTags(Tag.EXPOSE)
          .naming(Naming.identity())
          .typeParameters(typesReader.parameters)
          .addAllCode(sourceMapper.getBlock(memberPath(method)))
          .build());

      return true;
    }

    private boolean processGenericEncodedMethod(
        ExecutableElement method,
        List<EncodedElement> collection,
        Tag... additionalTags) {
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

      builder.typeParameters(typesReader.parameters);

      return typesReader;
    }

    private List<Param> getParameters(final TypeExtractor typesReader, ExecutableElement method) {
      List<Param> result = new ArrayList<>();
      for (VariableElement v : method.getParameters()) {
        result.add(Param.of(
            v.getSimpleName().toString(),
            typesReader.get(v.asType())));
      }
      if (!result.isEmpty() && method.isVarArgs()) {
        Param last = Iterables.getLast(result);
        Type type = last.type().accept(new Type.Transformer() {
          @Override
          public Type array(Array array) {
            return typesReader.factory.varargs(array.element);
          }
        });
        result.set(result.size() - 1, Param.of(last.name(), type));
      }
      return result;
    }

    private Naming inferNaming(VariableElement field) {
      Optional<NamingMirror> namingAnnotation = NamingMirror.find(field);
      if (namingAnnotation.isPresent()) {
        return Naming.from(namingAnnotation.get().value());
      }
      boolean isConstant = field.getModifiers().contains(Modifier.STATIC)
          && field.getModifiers().contains(Modifier.FINAL);

      String base = field.getSimpleName().toString();

      if (field.getModifiers().contains(Modifier.PRIVATE)) {
        String extraPrefix = isConstant ? CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name) : name + "_";
        base = extraPrefix + base;
      }

      return Naming.from(base).requireNonConstant(Preference.SUFFIX);
    }

    private Naming inferNaming(ExecutableElement method) {
      Optional<NamingMirror> namingAnnotation = NamingMirror.find(method);
      if (namingAnnotation.isPresent()) {
        return Naming.from(namingAnnotation.get().value());
      }
      String simpleName = method.getSimpleName().toString();
      if (method.getModifiers().contains(Modifier.PRIVATE)) {
        return Naming.from(name + "_" + simpleName).requireNonConstant(Preference.SUFFIX);
      }
      return Naming.from(simpleName).requireNonConstant(Preference.SUFFIX);
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

        if (!typeParams.equals(getTypeParameterNames(type))
            || !type.getModifiers().contains(Modifier.STATIC)) {
          reporter.withElement(type)
              .error("@Encoding.Builder class '%s' should be static with"
                  + " the same type parameters as encoding type: %s",
                  type.getSimpleName(),
                  typesReader.parameters);

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
                    + " if they are not conflicting as per JLS overload rules",
                    member.getSimpleName());
            continue;
          }

          if (member.getKind() == ElementKind.FIELD) {
            if (processBuilderField((VariableElement) member))
              continue;
          }

          if (member.getKind() == ElementKind.METHOD) {
            if (processBuilderMethod((ExecutableElement) member))
              continue;
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
                + " an builder field name is derived automatically",
                field.getSimpleName());
      }

      builderFields.add(new EncodedElement.Builder()
          .type(typesReader.get(field.asType()))
          .name(field.getSimpleName().toString())
          .naming(inferNaming(field))
          .typeParameters(typesReader.parameters)
          .addAllTags(inferTags(field, Tag.FIELD, Tag.BUILDER))
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
            .warning("@Encoding.Build method '%s' return type does not match @Encoding.Impl field type",
                method.getSimpleName());
      }

      if (NamingMirror.isPresent(method)) {
        reporter.withElement(method)
            .annotationNamed(NamingMirror.simpleName())
            .warning("@Encoding.Build method '%s' have naming annotation which is ignored."
                + " This method is used internally to build instances",
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
            .error("@Encoding.Build method '%s' have parameters which is illegal for build method",
                method.getSimpleName());
        return true;
      }

      this.build = new EncodedElement.Builder()
          .name(method.getSimpleName().toString())
          .type(typesReader.get(method.getReturnType()))
          .addTags(Tag.BUILDER, Tag.BUILD, Tag.PRIVATE)
          .naming(inferNaming(method))
          .typeParameters(typesReader.parameters)
          .addAllCode(sourceMapper.getBlock(memberPath(method)))
          .build();

      return true;
    }

    private boolean processBuilderInitMethod(ExecutableElement method) {
      if (method.getModifiers().contains(Modifier.PRIVATE)) {
        reporter.withElement(method)
            .error("@Encoding.Init method '%s' cannot be private",
                method.getSimpleName());
        return true;
      }

      if (typesReader.get(method.getReturnType()) != Type.Primitive.VOID) {
        reporter.withElement(method)
            .error("@Encoding.Init method '%s' should be declared void."
                + " During instantiation, void return type will be replaced with builder type"
                + " and 'return this' used for chained invokation",
                method.getSimpleName());
        return true;
      }

      if (!method.getTypeParameters().isEmpty()) {
        reporter.withElement(method)
            .error("@Encoding.Init method '%s' cannot have type parameters",
                method.getSimpleName());
        return true;
      }

      Tag[] additionalTags = {Tag.INIT, Tag.BUILDER};

      if (CopyMirror.isPresent(method)) {
        if (builderInitCopy != null) {
          reporter.withElement(method)
              .error("@Encoding.Init @Encoding.Copy method '%s' is duplicating another copy method '%s'."
                  + " There should be only one initialized defined as copy-initializer",
                  method.getSimpleName(),
                  builderInitCopy);
          return true;
        }
        builderInitCopy = method.getSimpleName().toString();
        additionalTags = ObjectArrays.concat(additionalTags, Tag.COPY);
      }

      return processGenericEncodedMethod(method, builderInits, additionalTags);
    }

    private boolean processBuilderHelperMethod(ExecutableElement method) {
      return processGenericEncodedMethod(method, builderFields, Tag.HELPER, Tag.BUILDER);
    }

    private Set<Tag> inferTags(Element member, Tag... additionalTags) {
      EnumSet<Tag> tags = EnumSet.noneOf(Tag.class);
      if (member.getModifiers().contains(Modifier.STATIC)) {
        tags.add(Tag.STATIC);
      }
      if (member.getModifiers().contains(Modifier.PRIVATE)) {
        tags.add(Tag.PRIVATE);
      }
      if (member.getModifiers().contains(Modifier.PROTECTED)) {
        tags.add(Tag.PROTECTED);
      }
      if (member.getModifiers().contains(Modifier.PUBLIC)) {
        tags.add(Tag.PUBLIC);
      }
      if (member.getModifiers().contains(Modifier.FINAL)) {
        tags.add(Tag.FINAL);
      }
      for (Tag t : additionalTags) {
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
          if (e.tags().contains(Tag.BUILDER)) {
            context.add(e.name());
          }
        }
      }

      private void addInstanceMembers(Set<String> context) {
        for (EncodedElement e : allElements) {
          if (!e.tags().contains(Tag.STATIC) && !e.tags().contains(Tag.BUILDER)) {
            context.add(e.name());
          }
        }
      }

      private void addStaticMembers(Set<String> context) {
        for (EncodedElement e : allElements) {
          if (e.tags().contains(Tag.STATIC)) {
            context.add(e.name());
          }
        }
      }

      @Override
      public String apply(EncodedElement element) {
        Code.Binder linker = linkerFor(element);
        return Code.join(applyBinder(element, linker));
      }

      private Code.Binder linkerFor(EncodedElement element) {
        ImmutableSet.Builder<String> topsBuilder = ImmutableSet.builder();
        for (Param p : element.params()) {
          topsBuilder.add(p.name());
        }
        Set<String> tops = topsBuilder.build();

        if (element.tags().contains(Tag.BUILDER)) {
          return new Code.Binder(imports.classes, builderContext, tops);
        }
        if (element.tags().contains(Tag.STATIC)) {
          return new Code.Binder(imports.classes, staticContext, tops);
        }
        return new Code.Binder(imports.classes, instanceContext, tops);
      }
    }

    private List<String> getTypeParameterNames(Parameterizable element) {
      ArrayList<String> names = new ArrayList<>();
      for (TypeParameterElement p : element.getTypeParameters()) {
        names.add(p.getSimpleName().toString());
      }
      return names;
    }

    private List<Term> applyBinder(EncodedElement element, Code.Binder binder) {
      List<Term> result = binder.apply(element.code());
      for (Param p : element.params()) {
        // trick to interpolate members on the self-type parameter (for equals etc)
        if (p.type().equals(encodingSelfType)) {
          result = interpolateSelfEncodingParameter(binder, result, p.name());
        }
      }
      return result;
    }

    private List<Term> interpolateSelfEncodingParameter(Code.Binder binder, List<Term> result, String param) {
      Term thisSubstitute = new Code.Term("$$$") {};
      Code.WordOrNumber thisToken = new Code.WordOrNumber("this");

      ListIterator<Term> it = result.listIterator();
      while (it.hasNext()) {
        Term t = it.next();
        if (t.is("this")) {
          it.set(thisSubstitute);
        } else if (t.isBinding()) {
          Code.Binding b = (Binding) t;
          if (b.isTop() && b.identifier().equals(param)) {
            it.set(thisToken);
          }
        }
      }

      // rebind using new this
      result = binder.apply(result);

      // put "this" back
      it = result.listIterator();
      while (it.hasNext()) {
        Term t = it.next();
        if (t == thisToken) {
          it.set(Code.Binding.newTop(param));
        } else if (t == thisSubstitute) {
          it.set(thisToken);
        }
      }
      return result;
    }
  }
}
