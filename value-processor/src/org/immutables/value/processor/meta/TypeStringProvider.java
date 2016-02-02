/*
   Copyright 2015 Immutables Authors and Contributors

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

import org.immutables.value.processor.meta.Proto.DeclaringType;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import org.immutables.generator.AnnotationMirrors;
import org.immutables.generator.SourceExtraction;
import org.immutables.generator.SourceTypes;

/**
 * Encapsulates routines for get relevant strings for the raw types and type parameters,
 * while attempting to resolve unresolved types using source imports.
 */
class TypeStringProvider {
  private final TypeMirror startType;
  private final Element element;
  private final List<String> typeParameterStrings = Lists.newArrayListWithCapacity(2);
  private StringBuilder buffer;
  boolean unresolvedTypeHasOccured;
  boolean hasMaybeUnresolvedYetAfter;
  private ImmutableMap<String, String> sourceClassesImports;

  private String rawTypeName;
  private String returnTypeName;
  private boolean ended;
  @Nullable
  private List<String> workaroundTypeParameters;
  @Nullable
  private String workaroundTypeString;
  private final Reporter reporter;
  private final DeclaringType declaringType;

  TypeStringProvider(Reporter reporter, Element element, TypeMirror returnType, DeclaringType declaringType) {
    this.reporter = reporter;
    this.declaringType = declaringType;
    this.startType = returnType;
    this.element = element;
  }

  String rawTypeName() {
    return rawTypeName;
  }

  String returnTypeName() {
    return returnTypeName;
  }

  boolean hasSomeUnresovedTypes() {
    return hasMaybeUnresolvedYetAfter;
  }

  ImmutableList<String> typeParameters() {
    return ImmutableList.copyOf(workaroundTypeParameters != null
        ? workaroundTypeParameters
        : typeParameterStrings);
  }

  void process() {
    if (startType.getKind().isPrimitive()) {
      // taking a shortcut for primitives
      String typeName = Ascii.toLowerCase(startType.getKind().name());
      this.rawTypeName = typeName;
      this.returnTypeName = typeName;
      List<? extends AnnotationMirror> annotations = AnnotationMirrors.from(startType);
      if (!annotations.isEmpty()) {
        returnTypeName = typeAnnotationsToBuffer(annotations).append(typeName).toString();
      }
    } else {
      this.buffer = new StringBuilder(100);
      caseType(startType);

      if (workaroundTypeString != null) {
        // to not mix the mess, we just replace buffer with workaround produced type string
        this.buffer = new StringBuilder(workaroundTypeString);
      }

      // It seems that array type annotations are not exposed in javac
      // Nested type argument's type annotations are not exposed as well (in javac)
      // So currently we instert only for top level, declared type (here),
      // and primitives (see above)
      TypeKind k = startType.getKind();
      if (k == TypeKind.DECLARED || k == TypeKind.ERROR) {
        insertTypeAnnotationsIfPresent(startType, 0, rawTypeName.length());
      }

      this.returnTypeName = buffer.toString();
    }
  }

  private void appendResolved(DeclaredType type) {
    TypeElement typeElement = (TypeElement) type.asElement();
    String typeName = typeElement.getQualifiedName().toString();
    if (unresolvedTypeHasOccured) {
      boolean assumedNotQualified = Ascii.isUpperCase(typeName.charAt(0));
      if (assumedNotQualified) {
        typeName = resolveIfPossible(typeName);
      }
    }
    buffer.append(typeName);
    if (startType == type) {
      rawTypeName = typeName;
    }
  }

  private String resolveIfPossible(String typeName) {
    String resolvable = typeName;
    int indexOfDot = resolvable.indexOf('.');
    if (indexOfDot > 0) {
      resolvable = resolvable.substring(0, indexOfDot);
    }
    @Nullable String resolved = getFromSourceImports(resolvable);
    if (resolved != null) {
      if (indexOfDot > 0) {
        typeName = resolved + '.' + resolvable.substring(indexOfDot + 1);
      } else {
        typeName = resolved;
      }
    } else {
      hasMaybeUnresolvedYetAfter = true;
    }
    return typeName;
  }

  @Nullable
  private String getFromSourceImports(String resolvable) {
    if (sourceClassesImports == null) {
      sourceClassesImports = declaringType
          .associatedTopLevel()
          .sourceImports().classes;
    }
    return sourceClassesImports.get(resolvable);
  }

  private void insertTypeAnnotationsIfPresent(TypeMirror type, int typeStart, int typeEnd) {
    List<? extends AnnotationMirror> annotations = AnnotationMirrors.from(type);
    if (!annotations.isEmpty()) {
      StringBuilder annotationBuffer = typeAnnotationsToBuffer(annotations);
      int insertionIndex = typeStart + buffer.substring(typeStart, typeEnd).lastIndexOf(".") + 1;
      buffer.insert(insertionIndex, annotationBuffer);
    }
  }

  private StringBuilder typeAnnotationsToBuffer(List<? extends AnnotationMirror> annotations) {
    StringBuilder annotationBuffer = new StringBuilder(100);
    for (AnnotationMirror annotationMirror : annotations) {
      annotationBuffer
          .append(AnnotationMirrors.toCharSequence(annotationMirror))
          .append(' ');
    }
    return annotationBuffer;
  }

  private boolean tryToUseSourceAsAWorkaround() {
    if (element.getKind() != ElementKind.METHOD) {
      // we don't bother with non-method attributes
      // (like factory builder, where attributes are parameters)
      return false;
    }

    CharSequence returnTypeString = SourceExtraction.getReturnTypeString((ExecutableElement) element);
    if (returnTypeString.length() == 0) {
      // no source could be extracted for some reason, workaround will not work
      return false;
    }

    Entry<String, List<String>> extractedTypes = SourceTypes.extract(returnTypeString);

    // forces source imports based resolution,
    // we should not rely that types would be fully qualified
    Entry<String, List<String>> resolvedTypes = resolveTypes(extractedTypes);

    this.rawTypeName = resolvedTypes.getKey();
    this.workaroundTypeParameters = resolvedTypes.getValue();
    this.workaroundTypeString = SourceTypes.stringify(resolvedTypes);

    // workaround may have successed, need to continue with whatever we have
    return true;
  }

  private Entry<String, List<String>> resolveTypes(Entry<String, List<String>> sourceTypes) {
    String typeName = sourceTypes.getKey();
    boolean assumedNotQualified = Ascii.isUpperCase(typeName.charAt(0));
    if (assumedNotQualified) {
      typeName = resolveIfPossible(typeName);
    }
    List<String> typeArguments = Lists.newArrayListWithCapacity(sourceTypes.getValue().size());
    for (String typeArgument : sourceTypes.getValue()) {
      String resolvedTypeArgument = SourceTypes.stringify(resolveTypes(SourceTypes.extract(typeArgument)));
      typeArguments.add(resolvedTypeArgument);
    }
    return Maps.immutableEntry(typeName, typeArguments);
  }

  void caseType(TypeMirror type) {
    if (ended) {
      // to prevent additional recursive effects when using workaround
      return;
    }
    switch (type.getKind()) {
    case ERROR:
      unresolvedTypeHasOccured = true;
      //$FALL-THROUGH$
    case DECLARED:
      DeclaredType declaredType = (DeclaredType) type;
      appendResolved(declaredType);
      appendTypeArguments(type, declaredType);
      break;
    case ARRAY:
      TypeMirror componentType = ((ArrayType) type).getComponentType();
      int mark = buffer.length();
      caseType(componentType);
      cutTypeArgument(type, mark);
      buffer.append('[').append(']');
      break;
    case WILDCARD:
      WildcardType wildcard = (WildcardType) type;
      @Nullable TypeMirror extendsBound = wildcard.getExtendsBound();
      @Nullable TypeMirror superBound = wildcard.getSuperBound();
      if (extendsBound != null) {
        buffer.append("? extends ");
        caseType(extendsBound);
      } else if (superBound != null) {
        buffer.append("? super ");
        caseType(superBound);
      } else {
        buffer.append('?');
      }
      break;
    case TYPEVAR:
      // this workaround breaks this recursive flow, so we set up
      // ended flag

      if (tryToUseSourceAsAWorkaround()) {
        ended = true;
        break;
      }

      reporter.error("It is a compiler/annotation processing bug to receive type variables '%s' here."
              + " To avoid it — do not use not yet generated types in %s attribute",
              type,
              element.getSimpleName());

      // just append as toString whatever we have
      buffer.append(type);
      break;
    default:
      buffer.append(type);
    }
  }

  private void appendTypeArguments(TypeMirror type, DeclaredType declaredType) {
    List<? extends TypeMirror> arguments = declaredType.getTypeArguments();
    if (!arguments.isEmpty()) {
      buffer.append('<');
      boolean notFirst = false;
      for (TypeMirror argument : arguments) {
        if (notFirst) {
          buffer.append(',').append(' ');
        }
        notFirst = true;
        int mark = buffer.length();
        caseType(argument);
        cutTypeArgument(type, mark);
      }
      buffer.append('>');
    }
  }

  private void cutTypeArgument(TypeMirror type, int mark) {
    if (startType == type) {
      typeParameterStrings.add(buffer.substring(mark));
    }
  }
}
