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

import com.google.common.base.*;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import org.immutables.generator.*;
import org.immutables.generator.Naming.Usage;
import org.immutables.value.processor.encode.Code.Binding;
import org.immutables.value.processor.encode.Code.Term;
import org.immutables.value.processor.encode.EncodedElement.Param;
import org.immutables.value.processor.encode.EncodedElement.Tag;
import org.immutables.value.processor.encode.Type.*;
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
      if (a.getQualifiedName().contentEquals(EncodingMirror.qualifiedName())) {
        for (TypeElement t : ElementFilter.typesIn(round().getElementsAnnotatedWith(a))) {
          encodings.add(new Encoding(t));
        }
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

    final List<EncodedElement> fields = new ArrayList<>();
    final List<EncodedElement> expose = new ArrayList<>();
    final List<EncodedElement> copy = new ArrayList<>();
    final List<EncodedElement> helpers = new ArrayList<>();

    final List<EncodedElement> builderFields = new ArrayList<>();
    final List<EncodedElement> builderInits = new ArrayList<>();
    final List<EncodedElement> builderHelpers = new ArrayList<>();

    final Linkage linkage;
    final SourceExtraction.Imports imports;

    final Iterable<EncodedElement> allElements;
    final Set<String> nonlinkedImports;
    private final Type encodingSelfType;
    private String builderInitCopy;

    private final TypeElement typeEncoding;
    private @Nullable TypeElement typeBuilder;

    Encoding(TypeElement type) {
      this.typeEncoding = type;
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

      if (postValidate()) {
        provideSyntheticElements();
      }

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

    // used for cross validation possible only when all elements are discovered
    private boolean postValidate() {
      if (impl == null) {
        reporter.withElement(typeEncoding)
            .error("@Encoding.Impl field is bare minimum to be declared. Please add implementation field declaration");
        return false;
      }

      if (isPrimitiveExpose()
          && (toString == null || hashCode == null || equals == null)) {
        reporter.withElement(typeEncoding)
            .error("Encoding implemented or exposed via primitive type must define explicitly encoded 'toString', 'hashCode' and 'equals'."
                + " For reference types default routines are assumed via Object.toString(), Object.hashCode() and Object.equals()");
        return false;
      }

      for (EncodedElement e : copy) {
        if (!e.type().equals(impl.type())) {
          reporter.withElement(findEnclosedByName(typeEncoding, e.name()))
              .error("@Encoding.Copy method '%s' return type does not match @Encoding.Impl field type."
                  + " Please, declare it to return: %s",
                  e.name(), impl.type());
        }
      }

      if (typeBuilder != null) {
        if (build != null) {
          if (!build.type().equals(impl.type())) {
            // this is actually best-effort check
            reporter.withElement(findEnclosedByName(typeBuilder, build.name()))
                .warning("@Encoding.Build method '%s' return type does not match @Encoding.Impl field type."
                    + " Please, declare it to return: %s",
                    build.name(), impl.type());
          }

        } else {
          reporter.withElement(typeBuilder)
              .error("@Encoding.Builder must have no arg method @Encoding.Build."
                  + " It is used to describe how to get built fully built instance");
        }

        if (builderInitCopy == null) {
          reporter.withElement(typeBuilder)
              .error("One of builder init methods should be a copy method,"
                  + " i.e. it should be annotated @Encoding.Init @Encoding.Copy"
                  + " and be able to accept values of type which exposed accessor returns");

          return false;
        }
      }

      return true;
    }

    private boolean isPrimitiveExpose() {
      if (!expose.isEmpty()) {
        for (EncodedElement e : expose) {
          if (e.type() instanceof Type.Primitive) {
            return true;
          }
        }
      } else if (impl.type() instanceof Type.Primitive) {
        return true;
      }
      return false;
    }

    private Element findEnclosedByName(Element enclosing, String name) {
      for (Element e : enclosing.getEnclosedElements()) {
        if (e.getSimpleName().contentEquals(name)) {
          return e;
        }
      }
      throw new NoSuchElementException("No enclosed element named '" + name + "' found in " + enclosing);
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
            .error("Duplicate member name '%s'. Encoding has limitation so that any duplicate method names are not supported,"
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
      if (member.getKind() == ElementKind.INSTANCE_INIT) {
        return;
      }

      if (member.getSimpleName().contentEquals("<init>")) {
        return;
      }

      reporter.withElement(member)
          .warning("Unrecognized encoding member '%s' will be ignored", member.getSimpleName());
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

      EnumSet<Tag> tags = EnumSet.of(Tag.FIELD);
      AtomicReference<StandardNaming> standardNaming = new AtomicReference<>(StandardNaming.NONE);

      fields.add(new EncodedElement.Builder()
          .name(field.getSimpleName().toString())
          .type(typesReader.get(field.asType()))
          .naming(inferNaming(field, tags, standardNaming))
          .standardNaming(standardNaming.get())
          .typeParameters(typesReader.parameters)
          .addAllTags(inferTags(field, tags))
          .addAllCode(expression)
          .build());

      return true;
    }

    private boolean processImplField(VariableElement field) {
      boolean virtual = ImplMirror.find(field).get().virtual();

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
          .addTags(Tag.IMPL, Tag.FINAL, Tag.PRIVATE, virtual ? Tag.VIRTUAL : Tag.FIELD)
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
      if (OfMirror.isPresent(method)) {
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
            .error("@Encoding.Of duplicate method '%s'. Cannot have more than one init method",
                method.getSimpleName());
        return true;
      }

      if (!typeParams.equals(getTypeParameterNames(method))
          || !method.getModifiers().contains(Modifier.STATIC)) {
        reporter.withElement(method)
            .error("@Encoding.Of method '%s' should be static with"
                + " the same type parameters as encoding type: %s",
                method.getSimpleName(),
                typesReader.parameters);

        return true;
      }

      if (method.getParameters().size() != 1) {
        reporter.withElement(method)
            .error("@Encoding.Of method '%s' should take a single parameter"
                + " return type assignable to implementation field",
                method.getSimpleName());
        return true;
      }

      VariableElement parameter = method.getParameters().get(0);

      if (!method.getThrownTypes().isEmpty()) {
        reporter.withElement(method)
            .error("@Encoding.Of method '%s' cannot have throws declaration",
                method.getSimpleName());
        return true;
      }

      if (NamingMirror.isPresent(method)) {
        reporter.withElement(method)
            .annotationNamed(NamingMirror.simpleName())
            .warning("@Encoding.Of method '%s' have naming annotation which is ignored."
                + " Init is not exposed as a standalone method",
                method.getSimpleName());
      }

      this.from = new EncodedElement.Builder()
          .name(method.getSimpleName().toString())
          .type(typesReader.get(method.getReturnType()))
          .addTags(Tag.PRIVATE, Tag.FROM, Tag.STATIC)
          .naming(helperNaming(method.getSimpleName()))
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
          .naming(helperNaming(method.getSimpleName()))
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
          .naming(helperNaming(method.getSimpleName()))
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
          .type(Type.Reference.STRING)
          .addTags(Tag.PRIVATE, Tag.TO_STRING)
          .naming(helperNaming(method.getSimpleName()))
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
      AtomicReference<StandardNaming> standardNaming = new AtomicReference<>(StandardNaming.NONE);

      EnumSet<Tag> tags = EnumSet.noneOf(Tag.class);
      for (Tag t : additionalTags) {
        tags.add(t);
      }

      collection.add(builder
          .name(method.getSimpleName().toString())
          .type(typesReader.get(method.getReturnType()))
          .naming(inferNaming(method, tags, standardNaming))
          .standardNaming(standardNaming.get())
          .addAllTags(inferTags(method, tags))
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

    private Naming helperNaming(CharSequence encodedName) {
      return Naming.from("*_" + encodedName);
    }

    private Naming inferNaming(Element element, EnumSet<Tag> tags, AtomicReference<StandardNaming> standardNaming) {
      Optional<NamingMirror> namingAnnotation = NamingMirror.find(element);
      if (namingAnnotation.isPresent()) {
        try {
          NamingMirror mirror = namingAnnotation.get();
          Naming naming = Naming.from(mirror.value());
          if (mirror.depluralize()) {
            tags.add(Tag.DEPLURALIZE);
          }
          standardNaming.set(mirror.standard());
          return naming;
        } catch (IllegalArgumentException ex) {
          reporter.withElement(element)
              .annotationNamed(NamingMirror.simpleName())
              .error(ex.getMessage());
        }
      }
      if (element.getKind() == ElementKind.FIELD
          || (element.getKind() == ElementKind.METHOD
          && (element.getModifiers().contains(Modifier.PRIVATE) || tags.contains(Tag.PRIVATE)))) {
        return helperNaming(element.getSimpleName());
      }
      if (tags.contains(Tag.INIT) || tags.contains(Tag.COPY)) {
        return Naming.identity();
      }
      String encodedMethodName = element.getSimpleName().toString();
      return Naming.from("*" + Naming.Usage.CAPITALIZED.apply(encodedMethodName));
    }

    private String memberPath(Element member) {
      LinkedList<String> names = new LinkedList<>();
      for (Element e = member; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
        names.addFirst(e.getSimpleName().toString());
      }
      String path = Joiner.on('.').join(names);
      String suffix = member.getKind() == ElementKind.METHOD ? "()" : "";
      return path + suffix;
    }

    private boolean processClass(TypeElement type) {
      if (BuilderMirror.isPresent(type)) {
        this.typeBuilder = type;

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
                .error(memberPath(member)
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

          if (member.getKind() == ElementKind.INSTANCE_INIT) {
            continue;
          }

          if (member.getSimpleName().contentEquals("<init>")) {
            continue;
          }

          reporter.withElement(member)
              .warning("Unrecognized Builder member '%s' will be ignored", member.getSimpleName());
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

      EnumSet<Tag> tags = EnumSet.of(Tag.FIELD, Tag.BUILDER);

      AtomicReference<StandardNaming> standardNaming = new AtomicReference<>(StandardNaming.NONE);

      builderFields.add(new EncodedElement.Builder()
          .name(field.getSimpleName().toString())
          .type(typesReader.get(field.asType()))
          .naming(inferNaming(field, tags, standardNaming))
          .standardNaming(standardNaming.get())
          .typeParameters(typesReader.parameters)
          .addAllTags(inferTags(field, tags))
          .addAllCode(sourceMapper.getExpression(memberPath(field)))
          .build());

      return true;
    }

    private boolean processBuilderMethod(ExecutableElement method) {
      if (BuildMirror.isPresent(method)) {
        return processBuilderBuildMethod(method);
      }
      if (InitMirror.isPresent(method) || CopyMirror.isPresent(method)) {
        return processBuilderInitMethod(method);
      }
      return processBuilderHelperMethod(method);
    }

    private boolean processBuilderBuildMethod(ExecutableElement method) {
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

      EnumSet<Tag> tags = EnumSet.of(Tag.BUILDER, Tag.BUILD, Tag.PRIVATE);

      AtomicReference<StandardNaming> standardNaming = new AtomicReference<>(StandardNaming.NONE);

      this.build = new EncodedElement.Builder()
          .name(method.getSimpleName().toString())
          .type(typesReader.get(method.getReturnType()))
          .naming(inferNaming(method, tags, standardNaming))
          .standardNaming(standardNaming.get())
          .typeParameters(typesReader.parameters)
          .addAllTags(tags)
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
              .error("@Encoding.Copy method '%s' is duplicating another builder copy method '%s'."
                  + " There should be only one builder initializer defined as copy-initializer",
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

    private Set<Tag> inferTags(Element member, EnumSet<Tag> tags) {
      if (member.getModifiers().contains(Modifier.STATIC)) {
        tags.add(Tag.STATIC);
      }
      if (member.getModifiers().contains(Modifier.PRIVATE)) {
        tags.add(Tag.PRIVATE);
      }
      if (member.getModifiers().contains(Modifier.FINAL)) {
        tags.add(Tag.FINAL);
      }
      if (member.getAnnotation(SafeVarargs.class) != null) {
        tags.add(Tag.SAFE_VARARGS);
      }
      return tags;
    }

    private void provideSyntheticElements() {
      if (from == null) {
        // adding pass-through intializer
        this.from = new EncodedElement.Builder()
            .name(synthName("from"))
            .type(impl.type())
            .addTags(Tag.PRIVATE, Tag.FROM, Tag.STATIC, Tag.SYNTH)
            .naming(helperNaming("from"))
            .addParams(Param.of("value", impl.type()))
            .typeParameters(typesReader.parameters)
            .addAllCode(Code.termsFrom("{\nreturn value;\n}"))
            .build();
      }

      if (expose.isEmpty()) {
        expose.add(new EncodedElement.Builder()
            .name(synthName("get"))
            .type(impl.type())
            .addTags(Tag.EXPOSE, Tag.SYNTH)
            .naming(Naming.identity())
            .typeParameters(typesReader.parameters)
            .addAllCode(Code.termsFrom("{\nreturn " + impl.name() + ";\n}"))
            .build());
      }

      String exposeName = expose.get(0).name();

      if (hashCode == null) {
        this.hashCode = new EncodedElement.Builder()
            .name("hashCode")
            .type(Type.Primitive.INT)
            .naming(helperNaming("hashCode"))
            .typeParameters(typesReader.parameters)
            .addTags(Tag.PRIVATE, Tag.HASH_CODE, Tag.SYNTH)
            .addAllCode(Code.termsFrom("{\nreturn " + exposeName + "().hashCode();\n}"))
            .build();
      }

      if (toString == null) {
        this.toString = new EncodedElement.Builder()
            .name("toString")
            .type(Type.Reference.STRING)
            .naming(helperNaming("toString"))
            .addTags(Tag.PRIVATE, Tag.TO_STRING, Tag.SYNTH)
            .typeParameters(typesReader.parameters)
            .addAllCode(Code.termsFrom("{\nreturn " + exposeName + "().toString();\n}"))
            .build();
      }

      if (equals == null) {
        this.equals = new EncodedElement.Builder()
            .name("equals")
            .type(Type.Reference.STRING)
            .addTags(Tag.PRIVATE, Tag.EQUALS, Tag.SYNTH)
            .naming(helperNaming("equals"))
            .typeParameters(typesReader.parameters)
            .addParams(Param.of("other", encodingSelfType))
            .addAllCode(Code.termsFrom("{\nreturn this." + exposeName + "().equals(other." + exposeName + "())\n;}"))
            .build();
      }

      if (copy.isEmpty()) {
        // adding pass-through copy
        copy.add(new EncodedElement.Builder()
            .name(synthName("copy"))
            .type(impl.type())
            .naming(Naming.identity())
            .typeParameters(typesReader.parameters)
            .addTags(Tag.COPY, Tag.SYNTH)
            .addParams(Param.of("value", from.params().get(0).type()))
            .addAllCode(Code.termsFrom("{\nreturn " + from.name() + "(value);\n}"))
            .build());
      }

      if (typeBuilder == null) {
        // adding simple pass thru - assign builders
        String fieldElementName = synthName("builder");

        builderFields.add(new EncodedElement.Builder()
            .type(Primitive.asNonprimitive(impl.type()))
            .name(fieldElementName)
            .naming(helperNaming("builder"))
            .typeParameters(typesReader.parameters)
            .addTags(Tag.PRIVATE, Tag.FIELD, Tag.BUILDER, Tag.SYNTH)
            .build());

        this.build = new EncodedElement.Builder()
            .type(impl.type())
            .name(synthName("build"))
            .naming(helperNaming("build"))
            .typeParameters(typesReader.parameters)
            .addTags(Tag.PRIVATE, Tag.BUILD, Tag.BUILDER, Tag.SYNTH)
            .addAllCode(
                Code.termsFrom("{\n"
                    + "if (" + fieldElementName + " == null)"
                    + " throw new IllegalStateException(\"'<*>' is not initialized\");\n"
                    + "return " + fieldElementName + ";\n"
                    + "}"))
            .build();

        builderInits.add(new EncodedElement.Builder()
            .name(synthName("set"))
            .type(Type.Primitive.VOID)
            .naming(Naming.identity())
            .typeParameters(typesReader.parameters)
            .addTags(Tag.INIT, Tag.COPY, Tag.BUILDER, Tag.SYNTH)
            .addParams(Param.of("value", from.params().get(0).type()))
            .addAllCode(Code.termsFrom("{\nthis." + fieldElementName + " = " + from.name() + "(value);\n}"))
            .build());
      }
    }

    private String synthName(String name) {
      return Usage.LOWERIZED.apply(this.name) + "_" + name;
    }

    class Linkage implements Function<EncodedElement, String> {
      private final Set<Binding> staticContext = new LinkedHashSet<>();
      private final Set<Binding> instanceContext = new LinkedHashSet<>();
      private final Set<Binding> builderContext = new LinkedHashSet<>();

      Linkage() {
        addStaticMembers(staticContext);
        addStaticMembers(instanceContext);
        addStaticMembers(builderContext);
        addTypeParameters(instanceContext);
        addTypeParameters(builderContext);
        addTypeParameters(staticContext);
        addInstanceMembers(instanceContext);
        addBuilderMembers(builderContext);
      }

      private void addTypeParameters(Set<Binding> context) {
        for (String p : typeParams) {
          context.add(Binding.newTop(p));
        }
      }

      private void addBuilderMembers(Set<Binding> context) {
        for (EncodedElement e : allElements) {
          if (e.inBuilder()) {
            context.add(e.asBinding());
          }
        }
      }

      private void addInstanceMembers(Set<Binding> context) {
        for (EncodedElement e : allElements) {
          if (!e.inBuilder() && !e.isStatic()) {
            context.add(e.asBinding());
          }
        }
      }

      private void addStaticMembers(Set<Binding> context) {
        for (EncodedElement e : allElements) {
          if (!e.inBuilder() && e.isStatic()) {
            context.add(e.asBinding());
          }
        }
      }

      @Override
      public String apply(EncodedElement element) {
        Code.Binder linker = linkerFor(element);
        List<Term> boundTerms = applyBinder(element, linker);
        return Code.join(Code.trimLeadingIndent(boundTerms));
      }

      private Code.Binder linkerFor(EncodedElement element) {
        Set<Binding> bindings = new LinkedHashSet<>();
        if (element.tags().contains(Tag.BUILDER)) {
          bindings.addAll(builderContext);
        } else if (element.tags().contains(Tag.STATIC)) {
          bindings.addAll(staticContext);
        } else {
          bindings.addAll(instanceContext);
        }
        for (Param p : element.params()) {
          bindings.add(Binding.newTop(p.name()));
        }
        return new Code.Binder(imports.classes, bindings);
      }
    }

    private List<String> getTypeParameterNames(Parameterizable element) {
      List<String> names = new ArrayList<>();
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
          result = binder.parameterAsThis(result, p.name());
        }
      }
      return result;
    }
  }
}
