/*
   Copyright 2014-2018 Immutables Authors and Contributors

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
package org.immutables.value.processor.meta;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.ObjectArrays;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import org.immutables.generator.ClasspathFence;
import org.immutables.generator.EnvironmentState;
import org.immutables.generator.SourceExtraction;
import org.immutables.value.Value;
import org.immutables.value.processor.encode.EncMetadataMirror;
import org.immutables.value.processor.encode.EncodingInfo;
import org.immutables.value.processor.encode.EncodingMirror;
import org.immutables.value.processor.encode.Inflater;
import org.immutables.value.processor.encode.Instantiator;
import org.immutables.value.processor.encode.Type;
import org.immutables.value.processor.meta.AnnotationInjections.AnnotationInjection;
import org.immutables.value.processor.meta.AnnotationInjections.InjectionInfo;
import org.immutables.value.processor.meta.Reporter.About;
import org.immutables.value.processor.meta.Styles.UsingName.TypeNames;
import static com.google.common.base.Verify.verify;

@Value.Enclosing
public class Proto {
  private Proto() {}

  public static Type.Factory typeFactory() {
    return typeFactoryAndInflater().factory;
  }

  private static TypeFactoryAndInflater typeFactoryAndInflater() {
    return EnvironmentState.getPerProcessing(TypeFactoryAndInflater.class, TypeFactoryAndInflater::new);
  }

  private static class TypeFactoryAndInflater implements Runnable {
    final Type.Factory factory = new Type.Producer();
    final Inflater inflater = new Inflater(factory);

    @Override public void run() {}
  }

  final static class Interning {
    private final Interner<DeclaringPackage> packageInterner = Interners.newStrongInterner();
    private final Interner<DeclaringType> typeInterner = Interners.newStrongInterner();
    private final Interner<Protoclass> protoclassInterner = Interners.newStrongInterner();

    DeclaringPackage forPackage(DeclaringPackage declaringPackage) {
      return packageInterner.intern(declaringPackage);
    }

    DeclaringType forType(DeclaringType declaringType) {
      return typeInterner.intern(declaringType);
    }

    Protoclass forProto(Protoclass protoclass) {
      return protoclassInterner.intern(protoclass);
    }
  }

  @Value.Immutable(builder = false)
  public static abstract class MetaAnnotated {
    @Value.Parameter
    @Value.Auxiliary
    public abstract Element element();

    @Value.Parameter
    @Value.Auxiliary
    public abstract String qualifiedName();

    @Value.Parameter
    @Value.Auxiliary
    public abstract Environment environment();

    @Value.Derived
    @Value.Auxiliary
    public Set<EncodingInfo> encodings() {
      if (qualifiedName().endsWith("Enabled")
          || CustomImmutableAnnotations.annotations().contains(qualifiedName())
          || style().isPresent()) {

        // See if it is encoding enabled itself
        Optional<EncodingInfo> encoding = EncMetadataMirror.find(element())
            .transform(typeFactoryAndInflater().inflater);

        if (encoding.isPresent()) {
          return encoding.asSet();
        }

        // trying to find it as meta-meta annotation
        List<EncodingInfo> result = new ArrayList<>();
        for (AnnotationMirror m : element().getAnnotationMirrors()) {
          MetaAnnotated metaAnnotated = MetaAnnotated.from(m, environment());
          result.addAll(metaAnnotated.encodings());
        }

        if (!result.isEmpty()) {
          return ImmutableSet.copyOf(result);
        }
      }
      return ImmutableSet.of();
    }

    @Value.Derived
    @Value.Auxiliary
    public Optional<DataMirror> datatypeEnabled() {
      return DataMirror.find(element());
    }

    @Value.Derived
    @Value.Auxiliary
    public Optional<StyleInfo> style() {
      return StyleMirror.find(element()).transform(ToStyleInfo.FUNCTION);
    }

    @Value.Derived
    @Value.Auxiliary
    public Optional<String[]> depluralize() {
      Optional<DepluralizeMirror> d = DepluralizeMirror.find(element());
      if (d.isPresent()) {
        return Optional.of(d.get().dictionary());
      }
      return Optional.absent();
    }

    @Value.Derived
    @Value.Auxiliary
    public Optional<Long> serialVersion() {
      if (!environment().hasSerialModule()) {
        return Optional.absent();
      }
      Optional<VersionMirror> version = VersionMirror.find(element());
      return version.isPresent()
          ? Optional.of(version.get().value())
          : Optional.absent();
    }

    @Value.Derived
    @Value.Auxiliary
    public boolean isSerialStructural() {
      return environment().hasSerialModule()
          && (StructuralMirror.isPresent(element()) || AllStructuralMirror.isPresent(element()));
    }

    @Value.Derived
    @Value.Auxiliary
    public boolean isJacksonSerialized() {
      return environment().hasJacksonLib()
          && isJacksonSerializedAnnotated(element());
    }

    @Value.Derived
    @Value.Auxiliary
    public boolean isJacksonDeserialized() {
      return environment().hasJacksonLib()
          && isJacksonDeserializedAnnotated(element());
    }

    @Value.Derived
    @Value.Auxiliary
    public boolean isJacksonJsonTypeInfo() {
      return environment().hasJacksonLib()
          && isJacksonJsonTypeInfoAnnotated(element());
    }

    @Value.Derived
    @Value.Auxiliary
    public boolean isJsonQualifier() {
      return environment().hasOkJsonLib()
          && OkQualifierMirror.isPresent(element());
    }

    @Value.Derived
    @Value.Auxiliary
    public boolean isEnclosing() {
      return EnclosingMirror.isPresent(element());
    }

    @Value.Derived
    @Value.Auxiliary
    public ImmutableList<InjectionInfo> injectAnnotation() {
      if (environment().hasAnnotateModule()) {
        ImmutableList.Builder<InjectionInfo> builder = ImmutableList.builder();
        Optional<InjectAnnotationMirror> injectAnnotation = InjectAnnotationMirror.find(element());
        if (injectAnnotation.isPresent()) {
          builder.add(ToInjectionInfo.FUNCTION.apply(injectAnnotation.get()));
        }
        Optional<InjectManyAnnotationsMirror> injectMany = InjectManyAnnotationsMirror.find(element());
        if (injectMany.isPresent()) {
          for (InjectAnnotationMirror m : injectMany.get().value()) {
            builder.add(ToInjectionInfo.FUNCTION.apply(m));
          }
        }
        return builder.build();
      }
      return ImmutableList.of();
    }

    public static MetaAnnotated from(AnnotationMirror mirror, Environment environment) {
      TypeElement element = (TypeElement) mirror.getAnnotationType().asElement();
      String name = element.getQualifiedName().toString();

      ConcurrentMap<String, MetaAnnotated> cache = metaAnnotatedCache();
      @Nullable MetaAnnotated metaAnnotated = cache.get(name);
      if (metaAnnotated == null) {
        metaAnnotated = ImmutableProto.MetaAnnotated.of(element, name, environment);
        @Nullable MetaAnnotated existing = cache.putIfAbsent(name, metaAnnotated);
        if (existing != null) {
          metaAnnotated = existing;
        }
      }

      return metaAnnotated;
    }

    private static ConcurrentMap<String, MetaAnnotated> metaAnnotatedCache() {
      return EnvironmentState.getPerProcessing(MetaAnnotatedCache.class, MetaAnnotatedCache::new).cache;
    }
  }

  private static class MetaAnnotatedCache implements Runnable {
    final ConcurrentMap<String, MetaAnnotated> cache = new ConcurrentHashMap<>(16, 0.7f, 1);

    @Override public void run() {
      // these clear() calls are not strictly necessary, just subject to GC after processing or round
      // but we try to be "paranoidaly consistent"
      cache.clear();
    }
  }

  abstract static class Diagnosable {
    /**
     * Element suitable for reporting as a source of declaration which might causing problems.
     */
    @Value.Auxiliary
    abstract Element element();

    @Value.Auxiliary
    abstract Environment environment();

    ProcessingEnvironment processing() {
      return environment().processing();
    }

    @Value.Auxiliary
    @Value.Derived
    public String simpleName() {
      return element().getSimpleName().toString();
    }

    public Reporter report() {
      return Reporter.from(processing()).withElement(element());
    }
  }

  @Value.Immutable
  public abstract static class Environment {
    @Value.Parameter
    abstract ProcessingEnvironment processing();

    @Value.Parameter
    abstract Round round();

    @Value.Derived
    StyleInfo defaultStyles() {
      @Nullable TypeElement element = findElement(StyleMirror.qualifiedName());
      if (element == null) {
        processing().getMessager()
            .printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                "Could not found annotations on the compile classpath. It looks like annotation processor is running"
                    + " in a separate annotation-processing classpath and unable to get to annotation definitions."
                    + " To fix this, please add annotation-only artifact 'org.immutables:value:(version):annotations'"
                    + " to 'compile' 'compileOnly' or 'provided' dependency scope.");

        element = findElement(StyleMirror.mirrorQualifiedName());
        verify(element != null, "Classpath should contain at least mirror annotation, otherwise library is corrupted");
      }
      try {
        return ToStyleInfo.FUNCTION.apply(StyleMirror.from(element));
      } catch (Exception ex) {
        processing().getMessager()
            .printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                "The version of the Immutables annotation on the classpath has incompatible differences"
                    + " from the Immutables annotation processor used. Various problems might occur,"
                    + " like this one: "
                    + ex);

        element = findElement(StyleMirror.mirrorQualifiedName());
        verify(element != null,
            "classpath should contain at least the mirror annotation, otherwise library is corrupted");
        return ToStyleInfo.FUNCTION.apply(StyleMirror.from(element));
      }
    }

    /**
     * Try to find Guava's object util classes if they're available. First lookup for
     * {@code base.MoreObjects} then {@code base.Objects}.
     * Return {@code null} if not found.
     * @return full class name for Guava's {@code MoreObjects} / {@code Objects} or {@code null} if
     *     such class doesn't exist in classpath
     */
    @Nullable
    @Value.Lazy
    String typeMoreObjects() {
      for (String shortName : Arrays.asList("base.MoreObjects", "base.Objects")) {
        final String name = UnshadeGuava.qualify(shortName);
        if (hasElement(name)) {
          return name;
        }
      }
      // not found
      return null;
    }

    public boolean hasGuavaLib() {
      return typeMoreObjects() != null;
    }

    @Value.Lazy
    public boolean hasOkJsonLib() {
      return hasElement("com.squareup.moshi.Moshi");
    }

    @Value.Lazy
    public boolean hasGsonLib() {
      return hasElement("com.google.gson.Gson");
    }

    @Value.Lazy
    public boolean hasDatatypesModule() {
      return hasElement(DataMirror.qualifiedName());
    }

    @Value.Lazy
    public boolean hasDatatypes2Module() {
      return hasElement(DDataMirror.qualifiedName());
    }

    @Value.Lazy
    public boolean hasJacksonLib() {
      return hasElement(Proto.JACKSON_DESERIALIZE);
    }

    @Value.Lazy
    public boolean hasCriteriaModule() {
      return hasElement(CriteriaMirror.qualifiedName());
    }

    @Value.Lazy
    public boolean hasMongoModule() {
      return hasElement(RepositoryMirror.qualifiedName());
    }

    @Value.Lazy
    public boolean hasSerialModule() {
      return hasElement(VersionMirror.qualifiedName());
    }

    @Value.Lazy
    public boolean hasTreesModule() {
      return hasElement(TransformMirror.qualifiedName());
    }

    @Value.Lazy
    public boolean hasAstModule() {
      return hasElement(AstMirror.qualifiedName());
    }

    @Value.Lazy
    public boolean hasOrdinalModule() {
      return hasElement(ORDINAL_VALUE_INTERFACE_TYPE);
    }

    @Value.Lazy
    public boolean hasBuilderModule() {
      return hasElement((FactoryMirror.qualifiedName()));
    }

    @Value.Lazy
    public boolean hasFuncModule() {
      return hasElement(FunctionalMirror.qualifiedName());
    }

    @Value.Lazy
    public boolean hasEncodeModule() {
      return hasElement(EncodingMirror.qualifiedName());
    }

    @Value.Lazy
    public boolean hasAnnotateModule() {
      return hasElement(InjectAnnotationMirror.qualifiedName());
    }

    @Value.Lazy
    public boolean hasJava9Collections() {
      for (SourceVersion version : SourceVersion.values()) {
        if (version.name().equals("RELEASE_9")) {
          if (processing().getSourceVersion().compareTo(version) >= 0) {
            TypeElement element = findElement(List.class.getCanonicalName());
            assert element != null : "always present in modern JREs";
            for (ExecutableElement e : ElementFilter.methodsIn(element.getEnclosedElements())) {
              if (e.getModifiers().contains(Modifier.STATIC)
                  && e.getSimpleName().contentEquals("of")) {
                return true;
              }
            }
          }
          break;
        }
      }
      return false;
    }

    /**
     * Default type adapters should only be called if {@code Gson.TypeAdapters} annotation is
     * definitely in classpath. Currently, it is called by for mongo repository module,
     * which have {@code gson} module as a transitive dependency.
     * @return default type adapters
     */
    @Value.Lazy
    TypeAdaptersMirror defaultTypeAdapters() {
      @Nullable TypeElement typeElement =
          findElement(TypeAdaptersMirror.qualifiedName());

      Preconditions.checkState(typeElement != null,
          "Processor internal error, @%s is not know to be on the classpath",
          TypeAdaptersMirror.qualifiedName());

      return TypeAdaptersMirror.from(typeElement);
    }

    ValueType composeValue(Protoclass protoclass) {
      return round().composeValue(protoclass);
    }

    ImmutableList<Protoclass> protoclassesFrom(Iterable<? extends Element> elements) {
      return round().protoclassesFrom(elements);
    }

    /**
     * Check if {@code qualifiedName} is known to current environment (annotation processor).
     * Retrieve (by canonical name) {@link TypeElement} from APT environment.
     */
    private boolean hasElement(String qualifiedName) {
      return findElement(qualifiedName) != null;
    }

    private @Nullable TypeElement findElement(String qualifiedName) {
      if (ClasspathFence.isInhibited(qualifiedName)) {
        return null;
      }
      try {
        TypeElement typeElement = processing()
            .getElementUtils()
            .getTypeElement(qualifiedName);
        return typeElement;
      } catch (Exception ex) {
        // to be visible during build
        ex.printStackTrace();
        // any type loading problems, which are unlikely (leftover?)
        return null;
      }
    }

    private final Map<Set<EncodingInfo>, Instantiator> instantiators = new HashMap<>();

    Instantiator instantiatorFor(Set<EncodingInfo> encodings) {
      Inflater inflater = typeFactoryAndInflater().inflater;
      synchronized (instantiators) {
        return instantiators.computeIfAbsent(encodings, inflater::instantiatorFor);
      }
    }

    public boolean isCheckedException(TypeMirror throwable) {
      return checkedExceptionProbe().isCheckedException(throwable);
    }

    @Value.Lazy
    CheckedExceptionProbe checkedExceptionProbe() {
      return new CheckedExceptionProbe(
          processing().getTypeUtils(),
          processing().getElementUtils());
    }

    private final Map<String, Boolean> whetherTypeuseOnly = new HashMap<>(1);

    public boolean isTypeuseOnly(String annotation) {
      synchronized (whetherTypeuseOnly) {
        // don't want to run computation inside computeIfAbsent lambda
        @Nullable Boolean is = whetherTypeuseOnly.get(annotation);
        if (is == null) {
          is = determineIfUnambiguousTypeuse(annotation);
          whetherTypeuseOnly.put(annotation, is);
        }
        return is;
      }
    }

    private boolean determineIfUnambiguousTypeuse(String annotationClassName) {
      TypeElement annotationTypeElement = processing().getElementUtils().getTypeElement(annotationClassName);
      @Nullable Target targetAnnotation = annotationTypeElement.getAnnotation(Target.class);
      if (targetAnnotation == null) {
        // annotation can be used anywhere
        return false;
      }
      boolean isTypeUse = false;
      for (ElementType elementType : targetAnnotation.value()) {
        switch (elementType) {
          case LOCAL_VARIABLE:
          case PARAMETER:
          case FIELD:
          case METHOD:
            // these might conflict with TYPE_USE
            return false;
          case TYPE_USE:
            isTypeUse = true;
            continue;
          default: // skip
        }
      }
      return isTypeUse;
    }
  }

  /**
   * Introspection supertype for the {@link DeclaringType} and {@link DeclaringPackage}
   */
  public static abstract class AbstractDeclaring extends Diagnosable {
    public abstract String name();

    @Override
    @Value.Auxiliary
    public abstract Element element();

    public abstract DeclaringPackage packageOf();

    @Value.Lazy
    protected Optional<IncludeMirror> include() {
      return IncludeMirror.find(element());
    }

    @Value.Lazy
    protected Optional<FIncludeMirror> builderInclude() {
      return FIncludeMirror.find(element());
    }

    public String asPrefix() {
      return name().isEmpty() ? "" : (name() + ".");
    }

    public Optional<DeclaringType> asType() {
      return this instanceof DeclaringType
          ? Optional.of((DeclaringType) this)
          : Optional.<DeclaringType>absent();
    }

    /**
     * used to intern packaged created internally
     */
    @Value.Auxiliary
    abstract Proto.Interning interner();

    @Value.Lazy
    public Optional<TypeAdaptersMirror> typeAdapters() {
      return TypeAdaptersMirror.find(element());
    }

    @Value.Lazy
    public Optional<OkTypeAdaptersMirror> okTypeAdapters() {
      return OkTypeAdaptersMirror.find(element());
    }

    @Value.Lazy
    List<TypeElement> includedTypes() {
      Optional<IncludeMirror> includes = include();

      ImmutableList<TypeMirror> typeMirrors = includes.isPresent()
          ? ImmutableList.copyOf(includes.get().valueMirror())
          : ImmutableList.<TypeMirror>of();

      FluentIterable<TypeElement> typeElements = FluentIterable.from(typeMirrors)
          .filter(DeclaredType.class)
          .transform(DeclatedTypeToElement.FUNCTION);

      ImmutableSet<String> uniqueTypeNames = typeElements
          .filter(IsPublic.PREDICATE)
          .transform(ElementToName.FUNCTION)
          .toSet();

      if (uniqueTypeNames.size() != typeMirrors.size()) {
        report().annotationNamed(IncludeMirror.simpleName())
            .warning(About.INCOMPAT,
                "Some types were ignored, non-supported for inclusion: duplicates,"
                    + " non declared reference types, non-public");
      }

      return typeElements.toList();
    }

    @Value.Lazy
    List<TypeElement> builderIncludedTypes() {
      Optional<FIncludeMirror> includes = builderInclude();

      ImmutableList<TypeMirror> typeMirrors = includes.isPresent()
          ? ImmutableList.copyOf(includes.get().valueMirror())
          : ImmutableList.<TypeMirror>of();

      FluentIterable<TypeElement> typeElements = FluentIterable.from(typeMirrors)
          .filter(DeclaredType.class)
          .transform(DeclatedTypeToElement.FUNCTION);

      ImmutableSet<String> uniqueTypeNames = typeElements
          .filter(IsPublic.PREDICATE)
          .transform(ElementToName.FUNCTION)
          .toSet();

      if (uniqueTypeNames.size() != typeMirrors.size()) {
        report().annotationNamed(IncludeMirror.simpleName())
            .warning(About.INCOMPAT,
                "Some types were ignored, non-supported for inclusion: duplicates,"
                    + " non declared reference types, non-public");
      }

      return typeElements.toList();
    }

    private final List<AnnotationInjection> annotationInjections = new ArrayList<>();

    public List<AnnotationInjection> getAnnotationInjections() {
      if (!environment().hasAnnotateModule()) {
        return ImmutableList.of();
      }
      // force initialization of annotationInjections list
      metaAnnotated();

      synchronized (annotationInjections) {
        return ImmutableList.copyOf(annotationInjections);
      }
    }

    @Value.Lazy
    List<MetaAnnotated> metaAnnotated() {
      ImmutableList.Builder<MetaAnnotated> builder = ImmutableList.builder();
      for (AnnotationMirror mirror : element().getAnnotationMirrors()) {
        MetaAnnotated meta = MetaAnnotated.from(mirror, environment());
        registerAnnotationInjection(mirror, meta);
        builder.add(meta);
      }
      return builder.build();
    }

    private void registerAnnotationInjection(AnnotationMirror mirror, MetaAnnotated meta) {
      for (InjectionInfo info : meta.injectAnnotation()) {
        AnnotationInjection injection = info.injectionFor(mirror, environment());
        synchronized (annotationInjections) {
          annotationInjections.add(injection);
        }
      }
    }

    @Value.Lazy
    public Optional<StyleInfo> style() {
      Optional<StyleInfo> style = StyleMirror.find(element()).transform(ToStyleInfo.FUNCTION);

      if (style.isPresent()) {
        return style;
      }

      for (MetaAnnotated m : metaAnnotated()) {
        Optional<StyleInfo> metaStyle = m.style();
        if (metaStyle.isPresent()) {
          return metaStyle;
        }
      }

      return Optional.absent();
    }

    @Value.Lazy
    public Optional<DataMirror> datatypeEnabled() {
      Optional<DataMirror> datatypeOwn = DataMirror.find(element());
      if (datatypeOwn.isPresent()) {
        return datatypeOwn;
      }

      for (MetaAnnotated m : metaAnnotated()) {
        Optional<DataMirror> d = m.datatypeEnabled();
        if (d.isPresent()) {
          return d;
        }
      }

      return Optional.absent();
    }

    @Value.Lazy
    public boolean datatype2Enabled() {
      if (DDataMirror.isPresent(element())) {
        return true;
      }

      for (MetaAnnotated m : metaAnnotated()) {
        Optional<DataMirror> d = m.datatypeEnabled();
        if (d.isPresent()) return true;
      }

      return false;
    }

    @Value.Lazy
    public Optional<Long> serialVersion() {
      Optional<VersionMirror> version = VersionMirror.find(element());
      if (version.isPresent()) {
        return Optional.of(version.get().value());
      }

      for (MetaAnnotated metaAnnotated : metaAnnotated()) {
        Optional<Long> serialVersion = metaAnnotated.serialVersion();
        if (serialVersion.isPresent()) {
          return serialVersion;
        }
      }

      return Optional.absent();
    }

    @Value.Lazy
    public boolean isSerialStructural() {
      if (!environment().hasSerialModule()) return false;

      if (StructuralMirror.isPresent(element()) || AllStructuralMirror.isPresent(element())) {
        return true;
      }

      for (MetaAnnotated m : metaAnnotated()) {
        if (m.isSerialStructural()) return true;
      }

      return false;
    }

    @Value.Lazy
    public boolean isJacksonSerialized() {
      if (jacksonSerializeMode() == JacksonMode.DELEGATED) {
        // while DeclaringPackage cannot have those annotations
        // directly, just checking them as a general computation path
        // will not hurt much.
        return true;
      }
      for (MetaAnnotated metaAnnotated : metaAnnotated()) {
        if (metaAnnotated.isJacksonSerialized()) {
          return true;
        }
      }
      return false;
    }

    @Value.Lazy
    public JacksonMode jacksonSerializeMode() {
      return Proto.isJacksonSerializedAnnotated(element())
          ? JacksonMode.DELEGATED
          : JacksonMode.NONE;
    }

    @Value.Lazy
    public boolean isJacksonDeserialized() {
      if (isJacksonDeserializedAnnotated()) {
        // while DeclaringPackage cannot have those annotations
        // directly, just checking them as a general computation path
        // will not hurt much.
        return true;
      }
      for (MetaAnnotated metaAnnotated : metaAnnotated()) {
        if (metaAnnotated.isJacksonDeserialized()) {
          return true;
        }
      }
      return false;
    }

    protected void collectEncodings(Collection<EncodingInfo> encodings) {
      for (MetaAnnotated m : metaAnnotated()) {
        encodings.addAll(m.encodings());
      }
    }

    @Value.Lazy
    public Optional<String[]> depluralize() {
      @Nullable String[] dictionary = null;
      for (MetaAnnotated metaAnnotated : metaAnnotated()) {
        Optional<String[]> depluralize = metaAnnotated.depluralize();
        if (depluralize.isPresent()) {
          dictionary = concat(dictionary, depluralize.get());
        }
      }

      Optional<DepluralizeMirror> depluralize = DepluralizeMirror.find(element());
      if (depluralize.isPresent()) {
        dictionary = concat(dictionary, depluralize.get().dictionary());
      }

      return Optional.fromNullable(dictionary);
    }

    @Value.Lazy
    public boolean isJacksonDeserializedAnnotated() {
      return Proto.isJacksonDeserializedAnnotated(element());
    }

    @Value.Lazy
    public boolean isJacksonJsonTypeInfo() {
      if (isJacksonJsonTypeInfoAnnotated(element())) {
        // while DeclaringPackage cannot have those annotations
        // directly, just checking them as a general computation path
        // will not hurt much.
        return true;
      }
      for (MetaAnnotated metaAnnotated : metaAnnotated()) {
        if (metaAnnotated.isJacksonJsonTypeInfo()) {
          return true;
        }
      }
      return false;
    }
  }

  public enum JacksonMode {
    NONE, DELEGATED, BUILDER;
  }

  @Value.Immutable
  public static abstract class DeclaringPackage extends AbstractDeclaring {

    @Override
    @Value.Auxiliary
    public abstract PackageElement element();

    @Override
    public DeclaringPackage packageOf() {
      return this;
    }

    @Override
    @Value.Auxiliary
    @Value.Derived
    public String simpleName() {
      return element().isUnnamed() ? "" : element().getSimpleName().toString();
    }

    /**
     * Name is the only equivalence attribute. Basically packages are interned by name.
     * @return package name
     */
    @Override
    @Value.Derived
    public String name() {
      return element().isUnnamed() ? "" : element().getQualifiedName().toString();
    }

    @Value.Lazy
    Optional<DeclaringPackage> namedParentPackage() {
      String parentPackageName = SourceNames.parentPackageName(element());
      while (!parentPackageName.isEmpty()) {
        @Nullable PackageElement parentPackage =
            environment().processing()
                .getElementUtils()
                .getPackageElement(parentPackageName);

        if (parentPackage != null) {
          return Optional.of(interner().forPackage(
              ImmutableProto.DeclaringPackage.builder()
                  .environment(environment())
                  .interner(interner())
                  .element(parentPackage)
                  .build()));
        }

        // With JDK 9+ package elements are only returned for packages with a
        // `package-info.class` file. So although the parent may not be found,
        // there may be "ancestor packages" further up the hierarchy.
        parentPackageName = SourceNames.parentPackageName(parentPackageName);
      }
      return Optional.absent();
    }

    @Override
    @Value.Lazy
    public boolean isJacksonSerialized() {
      if (super.isJacksonSerialized()) {
        return true;
      }
      Optional<DeclaringPackage> parent = namedParentPackage();
      if (parent.isPresent()) {
        return parent.get().isJacksonSerialized();
      }
      return false;
    }

    @Override
    @Value.Lazy
    public boolean isJacksonDeserialized() {
      if (super.isJacksonDeserialized()) {
        return true;
      }
      Optional<DeclaringPackage> parent = namedParentPackage();
      if (parent.isPresent()) {
        return parent.get().isJacksonDeserialized();
      }
      return false;
    }

    @Override
    @Value.Lazy
    public boolean isJacksonJsonTypeInfo() {
      if (super.isJacksonJsonTypeInfo()) {
        return true;
      }
      Optional<DeclaringPackage> parent = namedParentPackage();
      if (parent.isPresent()) {
        return parent.get().isJacksonJsonTypeInfo();
      }
      return false;
    }

    @Override
    @Value.Lazy
    public boolean isSerialStructural() {
      boolean isSerialStructural = super.isSerialStructural();
      if (isSerialStructural) {
        return isSerialStructural;
      }
      Optional<DeclaringPackage> parent = namedParentPackage();
      if (parent.isPresent()) {
        return parent.get().isSerialStructural();
      }
      return false;
    }

    @Override
    @Value.Lazy
    public Optional<Long> serialVersion() {
      Optional<Long> serialVersion = super.serialVersion();
      if (serialVersion.isPresent()) {
        return serialVersion;
      }
      Optional<DeclaringPackage> parent = namedParentPackage();
      if (parent.isPresent()) {
        return parent.get().serialVersion();
      }
      return Optional.absent();
    }

    @Override
    @Value.Lazy
    public Optional<StyleInfo> style() {
      Optional<StyleInfo> style = super.style();
      if (style.isPresent()) {
        return style;
      }
      Optional<DeclaringPackage> parent = namedParentPackage();
      if (parent.isPresent()) {
        return parent.get().style();
      }
      return Optional.absent();
    }

    @Override
    @Value.Lazy
    public Optional<String[]> depluralize() {
      @Nullable String[] dictionary = null;
      Optional<DeclaringPackage> parent = namedParentPackage();
      if (parent.isPresent()) {
        Optional<String[]> depluralize = parent.get().depluralize();
        if (depluralize.isPresent()) {
          dictionary = concat(dictionary, depluralize.get());
        }
      }
      Optional<String[]> depluralize = super.depluralize();
      if (depluralize.isPresent()) {
        dictionary = concat(dictionary, depluralize.get());
      }
      return Optional.fromNullable(dictionary);
    }

    @Override
    protected void collectEncodings(Collection<EncodingInfo> encodings) {
      Optional<DeclaringPackage> parent = namedParentPackage();
      if (parent.isPresent()) {
        parent.get().collectEncodings(encodings);
      }
      super.collectEncodings(encodings);
    }

    @Override
    @Value.Lazy
    public Optional<DataMirror> datatypeEnabled() {
      Optional<DataMirror> datatypeMarker = super.datatypeEnabled();
      if (datatypeMarker.isPresent()) {
        return datatypeMarker;
      }
      Optional<DeclaringPackage> parent = namedParentPackage();
      if (parent.isPresent()) {
        return parent.get().datatypeEnabled();
      }
      return Optional.absent();
    }

    @Value.Lazy
    public boolean isJSpecifyNullMarked() {
      if (JSpecifyNullMarkedMirror.isPresent(element())) {
        return true;
      }

      @Nullable Element module = getModule();
      if (module != null && JSpecifyNullMarkedMirror.isPresent(module)) {
        return true;
      }
      // let's also check parent packages
      String parentPackageName = SourceNames.parentPackageName(element());
      while (!parentPackageName.isEmpty()) {
        @Nullable PackageElement parentPackage = environment().processing()
            .getElementUtils()
            .getPackageElement(parentPackageName);
        if (parentPackage != null && JSpecifyNullMarkedMirror.isPresent(parentPackage)) {
          return true;
        }
        parentPackageName = SourceNames.parentPackageName(parentPackageName);
      }
      return false;
    }

    private @Nullable Element getModule() {
      if (getModuleOf != null) {
        try {
          return (Element) getModuleOf.invoke(processing().getElementUtils(), element());
        } catch (InvocationTargetException | IllegalAccessException e) {
          return null;
        }
      }
      return null;
    }
  }

  // we're still on Java8 language level, getModuleOf is 9+
  // just avoiding compiler/linter problems
  private static final @Nullable Method getModuleOf;
  static {
    Method method;
    try {
      method = Elements.class.getMethod("getModuleOf", Element.class);
    } catch (NoSuchMethodException e) {
      method = null;
    }
    getModuleOf = method;
  }

  @Value.Immutable
  public static abstract class DeclaringType extends AbstractDeclaring {
    @Override
    @Value.Auxiliary
    public abstract TypeElement element();

    @Override
    @Value.Derived
    public String name() {
      return element().getQualifiedName().toString();
    }

    /**
     * returns this class if it's top level or enclosing top level type.
     * @return accossiated top level type.
     */
    public DeclaringType associatedTopLevel() {
      return enclosingTopLevel().or(this);
    }

    @Value.Lazy
    public Optional<DeclaringType> enclosingTopLevel() {
      TypeElement top = element();
      for (Element e = top; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
        top = (TypeElement) e;
      }
      if (top == element()) {
        return Optional.absent();
      }
      return Optional.of(interner().forType(
          ImmutableProto.DeclaringType.builder()
              .environment(environment())
              .interner(interner())
              .element(top)
              .build()));
    }

    @Value.Lazy
    public Optional<CriteriaMirror> criteria() {
      return CriteriaMirror.find(element());
    }

    @Value.Lazy
    public Optional<RepositoryMirror> repository() {
      return RepositoryMirror.find(element());
    }

    @Value.Lazy
    public Optional<CriteriaRepositoryMirror> criteriaRepository() {
      return CriteriaRepositoryMirror.find(element());
    }

    @Value.Lazy
    public Optional<DeclaringType> enclosingOf() {
      Optional<DeclaringType> topLevel = enclosingTopLevel();
      if (topLevel.isPresent() && topLevel.get().isEnclosing()) {
        return topLevel;
      }
      return Optional.absent();
    }

    @Override
    @Value.Derived
    @Value.Auxiliary
    public DeclaringPackage packageOf() {
      Element e = element();
      for (; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
      }
      return interner().forPackage(
          ImmutableProto.DeclaringPackage.builder()
              .environment(environment())
              .interner(interner())
              .element((PackageElement) e)
              .build());
    }

    @Value.Lazy
    public Optional<ValueImmutableInfo> features() {
      Optional<ValueImmutableInfo> immutableAnnotation =
          ImmutableMirror.find(element()).transform(ToImmutableInfo.FUNCTION);

      if (immutableAnnotation.isPresent()) {
        return immutableAnnotation;
      }

      for (String a : environment().round().customImmutableAnnotations()) {
        if (isAnnotatedWith(element(), a)) {
          return Optional.of(environment().defaultStyles().defaults());
        }
      }

      return Optional.absent();
    }

    @Value.Lazy
    public boolean isJSpecifyNullMarked() {
      return JSpecifyNullMarkedMirror.isPresent(element());
    }

    @Value.Lazy
    @Override
    public JacksonMode jacksonSerializeMode() {
      boolean wasJacksonSerialize = false;
      for (AnnotationMirror a : element().getAnnotationMirrors()) {
        TypeElement e = (TypeElement) a.getAnnotationType().asElement();
        if (!wasJacksonSerialize && e.getQualifiedName().contentEquals(JACKSON_SERIALIZE)) {
          wasJacksonSerialize = true;
        }
        if (e.getQualifiedName().contentEquals(JACKSON_DESERIALIZE)) {
          for (ExecutableElement attr : a.getElementValues().keySet()) {
            if (attr.getSimpleName().contentEquals("builder")) {
              // If builder attribute is specified, we don't consider this as
              // our, immutables, business to generate anything.
              return JacksonMode.BUILDER;
            }
          }
          return JacksonMode.DELEGATED;
        }
      }
      return wasJacksonSerialize
          ? JacksonMode.DELEGATED
          : JacksonMode.NONE;
    }

    @Value.Lazy
    public boolean useImmutableDefaults() {
      Optional<ValueImmutableInfo> immutables = features();
      if (immutables.isPresent()) {
        return immutables.get().isDefault();
      }
      return true;
    }

    @Value.Lazy
    public boolean isEnclosing() {
      if (EnclosingMirror.isPresent(element())) {
        return true;
      }
      if (isTopLevel()) {
        for (MetaAnnotated metaAnnotated : metaAnnotated()) {
          if (metaAnnotated.isEnclosing()) {
            return true;
          }
        }
      }
      return false;
    }

    @Value.Lazy
    public boolean isModifiable() {
      return ModifiableMirror.isPresent(element());
    }

    /**
     * @return true, if is top level
     */
    @Value.Derived
    @Value.Auxiliary
    public boolean isTopLevel() {
      return element().getNestingKind() == NestingKind.TOP_LEVEL;
    }

    /**
     * Checks if this element is a regular POJO (not an interface or abstract class) simple
     * class with getters and setters
     */
    @Value.Derived
    @Value.Auxiliary
    public boolean isJavaBean() {
      return element().getKind().isClass()
          && element().getKind() != ElementKind.ENUM
          && !element().getModifiers().contains(Modifier.PRIVATE)
          && !element().getModifiers().contains(Modifier.ABSTRACT)
          &&
          // restrict to Criteria and Repository annotations for now
          (CriteriaMirror.find(element()).isPresent() || CriteriaRepositoryMirror.find(element()).isPresent());
    }

    public boolean isImmutable() {
      return features().isPresent();
    }

    boolean verifiedFactory(ExecutableElement element) {
      if (!isTopLevel() || !suitableForBuilderFactory(element)) {
        report().withElement(element)
            .annotationNamed(FactoryMirror.simpleName())
            .error("@%s method '%s' should be static, non-private, non-void and enclosed in top level type",
                FactoryMirror.simpleName(),
                element.getSimpleName());
        return false;
      }

      return true;
    }

    boolean verifiedConstructor(ExecutableElement element) {
      if (!isTopLevel() || !suitableForBuilderConstructor(element)) {
        report().withElement(element)
            .annotationNamed(FConstructorMirror.simpleName())
            .error("@%s annotated element should be non-private constructor in a top level type",
                FConstructorMirror.simpleName(),
                element.getSimpleName());
        return false;
      }
      return true;
    }

    static boolean suitableForBuilderConstructor(ExecutableElement element) {
      return element.getKind() == ElementKind.CONSTRUCTOR
          && !element.getModifiers().contains(Modifier.PRIVATE);
    }

    static boolean suitableForBuilderFactory(ExecutableElement element) {
      return element.getKind() == ElementKind.METHOD
          && element.getReturnType().getKind() != TypeKind.VOID
          && !element.getModifiers().contains(Modifier.PRIVATE)
          && element.getModifiers().contains(Modifier.STATIC);
    }

    /**
     * Some validations, not exhaustive.
     */
    @Value.Check
    protected void validate() {
      if (include().isPresent() && !isTopLevel()) {
        report()
            .annotationNamed(IncludeMirror.simpleName())
            .error("@%s could not be used on nested types.", IncludeMirror.simpleName());
      }
      if (builderInclude().isPresent() && !isTopLevel()) {
        report()
            .annotationNamed(FIncludeMirror.simpleName())
            .error("@%s could not be used on nested types.", FIncludeMirror.simpleName());
      }
      if (isEnclosing() && !isTopLevel()) {
        report()
            .annotationNamed(EnclosingMirror.simpleName())
            .error("@%s should only be used on a top-level types.", EnclosingMirror.simpleName());
      }
      if (isImmutable() && element().getKind() == ElementKind.ENUM) {
        report()
            .annotationNamed(ImmutableMirror.simpleName())
            .error("@%s is not supported on enums", ImmutableMirror.simpleName());
      }
      if (isModifiable() && (isEnclosed() || isEnclosing())) {
        report()
            .annotationNamed(ModifiableMirror.simpleName())
            .error("@%s could not be used with or within @%s",
                ModifiableMirror.simpleName(),
                EnclosingMirror.simpleName());
      }
    }

    @Value.Lazy
    public CharSequence sourceCode() {
      if (!isTopLevel()) {
        return associatedTopLevel().sourceCode();
      }
      return SourceExtraction.extract(processing(), CachingElements.getDelegate(element()));
    }

    @Value.Lazy
    public CharSequence headerComments() {
      if (!isTopLevel()) {
        return associatedTopLevel().headerComments();
      }
      return SourceExtraction.headerFrom(sourceCode());
    }

    @Value.Lazy
    public SourceExtraction.Imports sourceImports() {
      if (!isTopLevel()) {
        return associatedTopLevel().sourceImports();
      }
      return SourceExtraction.importsFrom(sourceCode());
    }

    public boolean isTransformer() {
      return getTransform().isPresent();
    }

    public boolean isVisitor() {
      return getVisit().isPresent();
    }

    @Value.Lazy
    public Optional<TransformMirror> getTransform() {
      return environment().hasTreesModule()
          ? TransformMirror.find(element())
          : Optional.<TransformMirror>absent();
    }

    @Value.Lazy
    public Optional<TreesIncludeMirror> getTreesInclude() {
      return environment().hasTreesModule()
          ? TreesIncludeMirror.find(element())
          : Optional.<TreesIncludeMirror>absent();
    }

    @Value.Lazy
    public Optional<VisitMirror> getVisit() {
      return environment().hasTreesModule()
          ? VisitMirror.find(element())
          : Optional.<VisitMirror>absent();
    }

    @Value.Lazy
    public boolean isAst() {
      // considering ast is still in tree module
      return environment().hasTreesModule()
          && AstMirror.isPresent(element());
    }

    public boolean isEnclosed() {
      return enclosingOf().isPresent();
    }

    @Override
    protected void collectEncodings(Collection<EncodingInfo> encodings) {
      if (enclosingTopLevel().isPresent()) {
        enclosingTopLevel().get().collectEncodings(encodings);
      }
      super.collectEncodings(encodings);
    }
  }

  /**
   * Prototypical model for generated derived classes. {@code Protoclass} could be used to projects
   * different kind of derived classes.
   */
  @Value.Immutable
  public static abstract class Protoclass extends Diagnosable {

    @Value.Derived
    public String name() {
      return SourceNames.sourceQualifiedNameFor(sourceElement());
    }

    /**
     * Source type elements stores type element which is used as a source of value type model.
     * It is the annotated class for {@code @Value.Immutable} or type referenced in
     * {@code @Value.Include}.
     * @return source element
     */
    @Value.Auxiliary
    public abstract Element sourceElement();

    /**
     * Declaring package that defines value type (usually by import).
     * Or the package in which {@link #declaringType()} resides.
     * @return declaring package
     */
    public abstract DeclaringPackage packageOf();

    /**
     * The class, which is annotated to be a {@code @Value.Immutable}, {@code @Value.Include} or
     * {@code @Value.Enclosing}.
     * @return declaring type
     */
    public abstract Optional<DeclaringType> declaringType();

    @Value.Lazy
    public Optional<CriteriaMirror> criteria() {
      if (!declaringType().isPresent()) {
        return Optional.absent();
      }
      return kind().isIncluded() || kind().isDefinedValue() || kind().isJavaBean()
          ? declaringType().get().criteria()
          : Optional.<CriteriaMirror>absent();
    }

    @Value.Lazy
    public Optional<RepositoryMirror> repository() {
      if (!declaringType().isPresent()) {
        return Optional.absent();
      }
      return kind().isIncluded() || kind().isDefinedValue()
          ? declaringType().get().repository()
          : Optional.<RepositoryMirror>absent();
    }

    @Value.Lazy
    public Optional<CriteriaRepositoryMirror> criteriaRepository() {
      if (!declaringType().isPresent()) {
        return Optional.absent();
      }
      return kind().isIncluded() || kind().isDefinedValue() || kind().isJavaBean()
          ? declaringType().get().criteriaRepository()
          : Optional.<CriteriaRepositoryMirror>absent();
    }

    @Value.Lazy
    public Optional<TypeAdaptersMirror> gsonTypeAdapters() {
      Optional<AbstractDeclaring> typeAdaptersProvider = typeAdaptersProvider();
      if (typeAdaptersProvider.isPresent()) {
        return typeAdaptersProvider.get().typeAdapters();
      }
      if ((kind().isDefinedValue() || kind().isIncluded())
          && !kind().isNested()
          && repository().isPresent()) {
        return Optional.of(environment().defaultTypeAdapters());
      }
      return Optional.absent();
    }

    @Value.Lazy
    public Optional<AbstractDeclaring> typeAdaptersProvider() {
      Optional<DeclaringType> typeDefining =
          declaringType().isPresent()
              ? Optional.of(declaringType().get().associatedTopLevel())
              : Optional.<DeclaringType>absent();

      Optional<TypeAdaptersMirror> typeDefined =
          typeDefining.isPresent()
              ? typeDefining.get().typeAdapters()
              : Optional.<TypeAdaptersMirror>absent();

      Optional<TypeAdaptersMirror> packageDefined = packageOf().typeAdapters();

      if (packageDefined.isPresent()) {
        if (typeDefined.isPresent()) {
          report()
              .withElement(typeDefining.get().element())
              .annotationNamed(TypeAdaptersMirror.simpleName())
              .warning(About.INCOMPAT,
                  "@%s is also used on the package, this type level annotation is ignored",
                  TypeAdaptersMirror.simpleName());
        }
        return Optional.<AbstractDeclaring>of(packageOf());
      }

      return typeDefined.isPresent()
          ? Optional.<AbstractDeclaring>of(typeDefining.get())
          : Optional.<AbstractDeclaring>absent();
    }

    @Value.Lazy
    public Optional<OkTypeAdaptersMirror> okJsonTypeAdapters() {
      Optional<AbstractDeclaring> typeAdaptersProvider = okTypeAdaptersProvider();
      if (typeAdaptersProvider.isPresent()) {
        return typeAdaptersProvider.get().okTypeAdapters();
      }
      return Optional.absent();
    }

    @Value.Lazy
    public Optional<AbstractDeclaring> okTypeAdaptersProvider() {
      Optional<DeclaringType> typeDefining =
          declaringType().isPresent()
              ? Optional.of(declaringType().get().associatedTopLevel())
              : Optional.<DeclaringType>absent();

      Optional<OkTypeAdaptersMirror> typeDefined =
          typeDefining.isPresent()
              ? typeDefining.get().okTypeAdapters()
              : Optional.<OkTypeAdaptersMirror>absent();

      Optional<OkTypeAdaptersMirror> packageDefined = packageOf().okTypeAdapters();

      if (packageDefined.isPresent()) {
        if (typeDefined.isPresent()) {
          report()
              .withElement(typeDefining.get().element())
              .annotationNamed(OkTypeAdaptersMirror.simpleName())
              .warning("@%s is also used on the package, this type level annotation is ignored",
                  OkTypeAdaptersMirror.simpleName());
        }
        return Optional.<AbstractDeclaring>of(packageOf());
      }

      return typeDefined.isPresent()
          ? Optional.<AbstractDeclaring>of(typeDefining.get())
          : Optional.<AbstractDeclaring>absent();
    }

    @Value.Lazy
    public Optional<DataMirror> datatypeMarker() {
      Optional<AbstractDeclaring> provider = datatypeProvider();
      if (provider.isPresent()) {
        return provider.get().datatypeEnabled();
      }
      return Optional.absent();
    }

    @Value.Lazy
    public boolean datatype2Marker() {
      Optional<AbstractDeclaring> provider = datatypeProvider();
      return provider.isPresent() && provider.get().datatype2Enabled();
    }

    @Value.Lazy
    public Optional<AbstractDeclaring> datatypeProvider() {
      Optional<DeclaringType> typeDefining =
          declaringType().isPresent()
              ? Optional.of(declaringType().get().associatedTopLevel())
              : Optional.<DeclaringType>absent();

      Optional<DataMirror> typeDefined =
          typeDefining.isPresent()
              ? typeDefining.get().datatypeEnabled()
              : Optional.<DataMirror>absent();

      Optional<DataMirror> packageDefined = packageOf().datatypeEnabled();

      if (packageDefined.isPresent()) {
        if (typeDefined.isPresent()) {
          report()
              .withElement(typeDefining.get().element())
              .annotationNamed(DataMirror.simpleName())
              .warning("@%s is also used on the package, this type level annotation is ignored",
                  DataMirror.simpleName());
        }
        return Optional.<AbstractDeclaring>of(packageOf());
      }

      return typeDefined.isPresent()
          ? Optional.<AbstractDeclaring>of(typeDefining.get())
          : Optional.<AbstractDeclaring>absent();
    }

    @Value.Lazy
    public Optional<AbstractDeclaring> datatype2Provider() {
      Optional<DeclaringType> typeDefining =
          declaringType().isPresent()
              ? Optional.of(declaringType().get().associatedTopLevel())
              : Optional.<DeclaringType>absent();

      boolean typeDefined = typeDefining.isPresent()
          && typeDefining.get().datatype2Enabled();

      if (packageOf().datatype2Enabled()) {
        if (typeDefined) {
          report()
              .withElement(typeDefining.get().element())
              .annotationNamed(DDataMirror.simpleName())
              .warning("@%s is also used on the package, this type level annotation is ignored",
                  DDataMirror.simpleName());
        }
        return Optional.<AbstractDeclaring>of(packageOf());
      }

      return typeDefined
          ? Optional.of(typeDefining.get())
          : Optional.absent();
    }

    /**
     * Kind of protoclass declaration, it specifies how exactly the protoclass was declared.
     * @return definition kind
     */
    public abstract Kind kind();

    @Value.Derived
    public Visibility visibility() {
      return Visibility.of(sourceElement());
    }

    public Visibility declaringVisibility() {
      if (declaringType().isPresent()) {
        return Visibility.of(declaringType().get().element());
      }
      return Visibility.PUBLIC;
    }

    /**
     * Element used mostly for error reporting,
     * real model provided by {@link #sourceElement()}.
     */
    @Value.Derived
    @Value.Auxiliary
    @Override
    public Element element() {
      if (kind().isFactory()) {
        return sourceElement();
      }
      if (declaringType().isPresent()) {
        return declaringType().get().element();
      }
      return packageOf().element();
    }

    @Value.Lazy
    public Optional<Long> serialVersion() {
      if (declaringType().isPresent()) {
        DeclaringType t = declaringType().get();
        if (t.serialVersion().isPresent()) {
          return t.serialVersion();
        }
        if (t.enclosingTopLevel().isPresent()) {
          if (t.enclosingTopLevel().get().serialVersion().isPresent()) {
            return t.enclosingTopLevel().get().serialVersion();
          }
        }
      }
      return packageOf().serialVersion();
    }

    @Value.Lazy
    public boolean isJSpecifyNullMarked() {
      if (declaringType().isPresent()) {
        DeclaringType t = declaringType().get();
        if (t.isJSpecifyNullMarked()) {
          return true;
        }
        if (t.enclosingTopLevel().isPresent()) {
          if (t.enclosingTopLevel().get().isJSpecifyNullMarked()) {
            return true;
          }
        }
      }
      return packageOf().isJSpecifyNullMarked();
    }

    @Value.Lazy
    public boolean isSerialStructural() {
      if (declaringType().isPresent()) {
        DeclaringType t = declaringType().get();
        if (t.isSerialStructural()) {
          return true;
        }
        if (t.enclosingTopLevel().isPresent()) {
          if (t.enclosingTopLevel().get().isSerialStructural()) {
            return true;
          }
        }
      }
      return packageOf().isSerialStructural();
    }

    @Value.Lazy
    public boolean isJacksonSerialized() {
      if (!styles().style().jacksonIntegration()) {
        return false;
      }
      if (declaringType().isPresent()) {
        DeclaringType t = declaringType().get();
        if (t.isJacksonSerialized()) {
          return true;
        }
        if (t.enclosingTopLevel().isPresent()) {
          if (t.enclosingTopLevel().get().isJacksonSerialized()) {
            return true;
          }
        }
      }
      return packageOf().isJacksonSerialized();
    }

    @Value.Lazy
    public boolean isJacksonDeserialized() {
      if (!styles().style().jacksonIntegration()) {
        return false;
      }
      if (declaringType().isPresent()) {
        DeclaringType t = declaringType().get();
        if (t.isJacksonDeserialized()) {
          return true;
        }
        if (t.enclosingTopLevel().isPresent()) {
          if (t.enclosingTopLevel().get().isJacksonDeserialized()) {
            return true;
          }
        }
      }
      return packageOf().isJacksonDeserialized();
    }

    @Value.Lazy
    public ValueImmutableInfo features() {
      if (declaringType().isPresent()
          && !declaringType().get().useImmutableDefaults()) {
        Optional<ValueImmutableInfo> features = declaringType().get().features();
        if (features.isPresent()) {
          return features.get();
        }
      }
      return styles().defaults();
    }

    @Value.Lazy
    public Styles styles() {
      StyleInfo styleInfo = determineStyle().or(environment().defaultStyles());
      Optional<String[]> depluralize = depluralize();
      if (depluralize.isPresent()) {
        styleInfo = ImmutableStyleInfo.copyOf(styleInfo)
            .withDepluralize(true)
            .withDepluralizeDictionary(concat(styleInfo.depluralizeDictionary(), depluralize.get()));
      }
      return styleInfo.getStyles();
    }

    @Value.Lazy
    public Optional<String[]> depluralize() {
      @Nullable String[] dictionary = null;

      Optional<String[]> depluralize = packageOf().depluralize();
      if (depluralize.isPresent()) {
        dictionary = concat(dictionary, depluralize.get());
      }

      if (declaringType().isPresent()) {
        DeclaringType type = declaringType().get();

        if (type.enclosingTopLevel().isPresent()) {
          depluralize = type.enclosingTopLevel().get().depluralize();

          if (depluralize.isPresent()) {
            dictionary = concat(dictionary, depluralize.get());
          }
        }

        depluralize = type.depluralize();
        if (depluralize.isPresent()) {
          dictionary = concat(dictionary, depluralize.get());
        }
      }

      return Optional.fromNullable(dictionary);
    }

    private Optional<StyleInfo> determineStyle() {
      if (declaringType().isPresent()) {
        DeclaringType type = declaringType().get();

        Optional<DeclaringType> enclosing = type.enclosingOf();
        if (enclosing.isPresent()) {
          Optional<StyleInfo> enclosingStyle = enclosing.get().style();
          if (enclosing.get() != type) {
            Optional<StyleInfo> style = type.style();
            if (style.isPresent()
                && enclosingStyle.isPresent()
                && !style.equals(enclosingStyle)) {
              warnAboutIncompatibleStyles();
            }
          }
          if (enclosingStyle.isPresent()) {
            return enclosingStyle;
          }
        } else {
          Optional<StyleInfo> style = type.style();
          if (style.isPresent()) {
            return style;
          }
          Optional<DeclaringType> topLevel = type.enclosingTopLevel();
          if (topLevel.isPresent() && topLevel.get().style().isPresent()) {
            return topLevel.get().style();
          }
        }
      }
      return packageOf().style();
    }

    private void warnAboutIncompatibleStyles() {
      report().annotationNamed(StyleMirror.simpleName())
          .warning(About.INCOMPAT,
              "Use styles only on enclosing types."
                  + " All nested styles will inherit it."
                  + " Nested immutables cannot deviate in style from enclosing type,"
                  + " so generated stucture will be consistent");
    }

    @Value.Derived
    @Value.Auxiliary
    public Optional<DeclaringType> enclosingOf() {
      if (declaringType().isPresent()) {
        // this should come before isFactory check, bogus, I know
        if (kind().isNestedFactoryOrConstructor()) {
          Optional<DeclaringType> enclosingOf = declaringType().get().enclosingOf();
          if (!enclosingOf.isPresent() && kind().isFactory()) {
            return declaringType();
          }
          return enclosingOf;
        }
        if (kind().isFactory()) {
          return declaringType();
        }
        if (kind().isNested()) {
          if (kind().isIncluded()) {
            return declaringType();
          }
          return declaringType().get().enclosingOf();
        }
      }
      return Optional.absent();
    }

    TypeNames createTypeNames() {
      Element sourceElement = sourceElement();
      if (sourceElement.getKind() == ElementKind.CONSTRUCTOR) {
        sourceElement = sourceElement.getEnclosingElement();
      }
      return styles().forType(sourceElement.getSimpleName().toString());
    }

    public enum Kind {
      INCLUDED_IN_PACKAGE,
      INCLUDED_ON_TYPE,
      INCLUDED_FACTORY_IN_PACKAGE,
      INCLUDED_FACTORY_ON_TYPE,
      INCLUDED_CONSTRUCTOR_IN_PACKAGE,
      INCLUDED_CONSTRUCTOR_ON_TYPE,
      INCLUDED_IN_TYPE,
      DEFINED_FACTORY,
      DEFINED_NESTED_FACTORY,
      DEFINED_CONSTRUCTOR,
      DEFINED_NESTED_CONSTRUCTOR,
      DEFINED_TYPE,
      DEFINED_JAVABEAN,
      DEFINED_TYPE_AND_COMPANION,
      DEFINED_COMPANION,
      DEFINED_AND_ENCLOSING_TYPE,
      DEFINED_ENCLOSING_TYPE,
      DEFINED_NESTED_TYPE,
      DEFINED_RECORD,
      DEFINED_NESTED_RECORD;

      public boolean isNested() {
        switch (this) {
          case INCLUDED_IN_TYPE:
          case DEFINED_NESTED_TYPE:
            return true;
          default:
            return false;
        }
      }

      public boolean isNestedFactoryOrConstructor() {
        switch (this) {
          case DEFINED_NESTED_FACTORY:
          case DEFINED_NESTED_CONSTRUCTOR:
          case DEFINED_NESTED_RECORD:
            return true;
          default:
            return false;
        }
      }

      public boolean isIncluded() {
        switch (this) {
          case INCLUDED_IN_PACKAGE:
          case INCLUDED_IN_TYPE:
          case INCLUDED_ON_TYPE:
            return true;
          default:
            return false;
        }
      }

      public boolean isEnclosing() {
        switch (this) {
          case DEFINED_AND_ENCLOSING_TYPE:
          case DEFINED_ENCLOSING_TYPE:
            return true;
          default:
            return false;
        }
      }

      public boolean isJavaBean() {
        return this == DEFINED_JAVABEAN;
      }

      public boolean isValue() {
        switch (this) {
          case INCLUDED_IN_PACKAGE:
          case INCLUDED_ON_TYPE:
          case INCLUDED_IN_TYPE:
          case DEFINED_TYPE:
          case DEFINED_TYPE_AND_COMPANION:
          case DEFINED_AND_ENCLOSING_TYPE:
          case DEFINED_NESTED_TYPE:
            return true;
          default:
            return false;
        }
      }

      public boolean isDefinedValue() {
        switch (this) {
          case DEFINED_TYPE:
          case DEFINED_TYPE_AND_COMPANION:
          case DEFINED_AND_ENCLOSING_TYPE:
          case DEFINED_NESTED_TYPE:
            return true;
          default:
            return false;
        }
      }

      public boolean isModifiable() {
        return this == DEFINED_TYPE_AND_COMPANION
            || this == DEFINED_COMPANION;
      }

      public boolean isConstructor() {
        switch (this) {
          case DEFINED_CONSTRUCTOR:
          case INCLUDED_CONSTRUCTOR_IN_PACKAGE:
          case INCLUDED_CONSTRUCTOR_ON_TYPE:
            return true;
          default:
            return false;
        }
      }

      public boolean isFactoryNotNested() {
        switch (this) {
          case DEFINED_FACTORY:
          case INCLUDED_FACTORY_IN_PACKAGE:
          case INCLUDED_FACTORY_ON_TYPE:
          case DEFINED_CONSTRUCTOR:
          case INCLUDED_CONSTRUCTOR_IN_PACKAGE:
          case INCLUDED_CONSTRUCTOR_ON_TYPE:
            return true;
          default:
            return false;
        }
      }

      public boolean isFactory() {
        switch (this) {
          case DEFINED_FACTORY:
          case DEFINED_NESTED_FACTORY:
          case INCLUDED_FACTORY_IN_PACKAGE:
          case INCLUDED_FACTORY_ON_TYPE:
          case DEFINED_CONSTRUCTOR:
          case DEFINED_RECORD:
          case DEFINED_NESTED_RECORD:
          case DEFINED_NESTED_CONSTRUCTOR:
          case INCLUDED_CONSTRUCTOR_IN_PACKAGE:
          case INCLUDED_CONSTRUCTOR_ON_TYPE:
            return true;
          default:
            return false;
        }
      }

      public boolean isEnclosingOnly() {
        return this == DEFINED_ENCLOSING_TYPE;
      }

      public boolean isRecord() {
        return this == DEFINED_RECORD || this == DEFINED_NESTED_RECORD;
      }
    }

    @Value.Lazy
    public boolean isJacksonJsonTypeInfo() {
      if (!styles().style().jacksonIntegration()) {
        return false;
      }
      if (declaringType().isPresent()) {
        DeclaringType type = declaringType().get();
        if (type.isJacksonJsonTypeInfo()) {
          return true;
        }
      }
      return packageOf().isJacksonJsonTypeInfo();
    }

    public boolean isAst() {
      return declaringType().isPresent()
          && declaringType().get().isAst();
    }

    public boolean isTransformer() {
      return declaringType().isPresent()
          && declaringType().get().isTransformer();
    }

    public boolean isVisitor() {
      return declaringType().isPresent()
          && declaringType().get().isVisitor();
    }

    public Optional<TransformMirror> getTransform() {
      return declaringType().isPresent()
          ? declaringType().get().getTransform()
          : Optional.<TransformMirror>absent();
    }

    public Optional<VisitMirror> getVisit() {
      return declaringType().isPresent()
          ? declaringType().get().getVisit()
          : Optional.<VisitMirror>absent();
    }

    public Optional<TreesIncludeMirror> getTreesInclude() {
      return declaringType().isPresent()
          ? declaringType().get().getTreesInclude()
          : Optional.<TreesIncludeMirror>absent();
    }

    @Value.Lazy
    public Constitution constitution() {
      return ImmutableConstitution.builder()
          .protoclass(this)
          .build();
    }

    @Value.Lazy
    public Instantiator encodingInstantiator() {
      List<EncodingInfo> results = new ArrayList<>();
      packageOf().collectEncodings(results);
      if (declaringType().isPresent()) {
        declaringType().get().collectEncodings(results);
      }
      return environment().instantiatorFor(FluentIterable.from(results).toSet());
    }

    public boolean isJacksonProperties() {
      if (!styles().style().jacksonIntegration()) {
        return false;
      }
      if (declaringType().isPresent()) {
        if (declaringType().get().jacksonSerializeMode() != JacksonMode.NONE) {
          return true;
        }
      }
      return isJacksonSerialized();
    }

    Protoclass debug(String line) {
      environment().round().debug(line);
      return this;
    }

    List<String> getDebugLines() {
      return environment().round().getDebugLines();
    }
  }

  enum ElementToName implements Function<TypeElement, String> {
    FUNCTION;

    @Override
    public String apply(TypeElement input) {
      return input.getQualifiedName().toString();
    }
  }

  enum DeclatedTypeToElement implements Function<DeclaredType, TypeElement> {
    FUNCTION;

    @Override
    public TypeElement apply(DeclaredType input) {
      return (TypeElement) input.asElement();
    }
  }

  enum IsPublic implements Predicate<Element> {
    PREDICATE;

    @Override
    public boolean apply(Element input) {
      return input.getModifiers().contains(Modifier.PUBLIC);
    }
  }

  enum ToImmutableInfo implements Function<ImmutableMirror, ValueImmutableInfo> {
    FUNCTION;

    @Override
    public ValueImmutableInfo apply(ImmutableMirror input) {
      return ValueImmutableInfo.infoFrom(input);
    }
  }

  enum ToStyleInfo implements Function<StyleMirror, StyleInfo> {
    FUNCTION;

    @Override
    public StyleInfo apply(StyleMirror input) {
      return StyleInfo.infoFrom(input);
    }
  }

  enum ToInjectionInfo implements Function<InjectAnnotationMirror, InjectionInfo> {
    FUNCTION;

    @Override
    public InjectionInfo apply(InjectAnnotationMirror input) {
      return AnnotationInjections.infoFrom(input);
    }
  }

  static boolean isJacksonSerializedAnnotated(Element element) {
    return isAnnotatedWith(element, JACKSON_SERIALIZE)
        || isAnnotatedWith(element, JACKSON_DESERIALIZE);
  }

  static boolean isJacksonDeserializedAnnotated(Element element) {
    return isAnnotatedWith(element, JACKSON_DESERIALIZE);
  }

  static boolean isJacksonJsonTypeInfoAnnotated(Element element) {
    return isAnnotatedWith(element, JACKSON_TYPE_INFO);
  }

  static boolean isAnnotatedWith(Element element, String annotation) {
    for (AnnotationMirror a : element.getAnnotationMirrors()) {
      TypeElement e = (TypeElement) a.getAnnotationType().asElement();
      if (e.getQualifiedName().contentEquals(annotation)) {
        return true;
      }
      if (Annotations.hasJacksonPackagePrefix(annotation)
          && !annotation.equals(JACKSON_ANNOTATIONS_INSIDE)
          && isAnnotatedWith(e, JACKSON_ANNOTATIONS_INSIDE)
          && isAnnotatedWith(e, annotation)) {
        return true;
      }
    }
    return false;
  }

  static @Nullable String[] concat(@Nullable String[] first, @Nullable String[] second) {
    if (first == null)
      return second;
    if (second == null)
      return first;
    return ObjectArrays.concat(first, second, String.class);
  }

  static final String ORDINAL_VALUE_INTERFACE_TYPE = "org.immutables.ordinal.OrdinalValue";
  static final String JACKSON_TYPE_INFO = UnshadeJackson.qualify("annotation.JsonTypeInfo");
  static final String JACKSON_DESERIALIZE = UnshadeJackson.qualify("databind.annotation.JsonDeserialize");
  static final String JACKSON_SERIALIZE = UnshadeJackson.qualify("databind.annotation.JsonSerialize");
  static final String JACKSON_ANNOTATIONS_INSIDE = UnshadeJackson.qualify("annotation.JacksonAnnotationsInside");
  static final String PARCELABLE_INTERFACE_TYPE = "android.os.Parcelable";
  static final String PARCELABLE_CREATOR_FIELD = "CREATOR";
}
