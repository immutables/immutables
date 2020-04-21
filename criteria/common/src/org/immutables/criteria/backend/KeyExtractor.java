/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.backend;

import org.immutables.criteria.Criteria;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.reflect.ClassScanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Strategy to find and extract key value and metadata about this key from a class (or instance)
 *
 * Key can be single or composite (eg. derived from multiple attributes).
 */
public interface KeyExtractor {

  /**
   * Creates extractor for a concrete type
   */
  interface Factory {
    KeyExtractor create(Class<?> entity);
  }

  /**
   * Extract key from current instance. For composite keys will return a list of values.
   */
  Object extract(Object instance);

  /**
   * Expose key metadata for current entity
   */
  KeyMetadata metadata();

  /**
   * Metadata about key(s) for a class. Entity can have zero (no key), one (single key) or more keys (composite key).
   */
  interface KeyMetadata {

    /**
     * Wherever key(s) can be resolved to a known AST (as in {@link Expression}).
     * If key expression is known at runtime, further query optimizations can be
     * made.
     *
     * If key is provided as java lambda function then this method will return {@code false}
     */
    boolean isExpression();

    /**
     * Is there a key (at all) for current entity ? Most this method will return {@code true}
     * but for entities without a key {@code false} is expected
     */
    default boolean isKeyDefined() {
      return !keys().isEmpty();
    }

    /**
     * Lists available keys as {@link Expression}. For single key will have a list with single
     * element. For composite keys list will have several elements.
     *
     * @throws UnsupportedOperationException if current key is not an expression (see {@link #isExpression()}
     */
    List<Expression> keys();

  }

  /**
   * Factory which extracts key information by scanning class for {@link org.immutables.criteria.Criteria.Id} annotation.
   */
  static KeyExtractor.Factory defaultFactory() {
    return fromAnnotation(Criteria.Id.class);
  }

  /**
   * Extract key information based by scanning a class for a particular annotation. Presence
   * of annotation is required otherwise exception will be thrown.
   *
   * @throws NullPointerException if argument is null
   * @throws IllegalArgumentException if annotation is not present on entity class
   */
  static KeyExtractor.Factory fromAnnotation(Class<? extends Annotation> annotation) {
    Objects.requireNonNull(annotation, "annotation");

    Predicate<Member> predicate = m -> m instanceof AnnotatedElement
            && ((AnnotatedElement) m).isAnnotationPresent(annotation);

    // there can be multiple members with the same annotation
    Function<Class<?>, List<Member>> membersFn = type -> ClassScanner.of(type)
            .stream()
            .filter(predicate)
            .collect(Collectors.toList());

    return entityType -> {
      Objects.requireNonNull(entityType, "entityType");
      List<Member> members = membersFn.apply(entityType);
      if (members.isEmpty()) {
        // be strict and don't allow entities without a key
        throw new IllegalArgumentException(String.format("Annotation %s not found in members of %s", annotation.getName(), entityType));
      }

      if (members.size() == 1) {
        return new KeyExtractors.SingleMember(entityType, members.get(0));
      }

      return new KeyExtractors.MultipleMembers(entityType, members);
    };
  }

  /**
   * Factory to extract key information using just one member (field or method) of a class.
   * In this case it will be a single key extractor using reflection.
   *
   * @param keyLookupFn function extracting single member representing a key from a entity
   */
  static KeyExtractor.Factory forSingleMember(Function<Class<?>, Member> keyLookupFn) {
    Objects.requireNonNull(keyLookupFn, "keyLookupFn");
    return entityType -> new KeyExtractors.SingleMember(entityType, keyLookupFn.apply(entityType));
  }

  /**
   * Factory to extract composite key using multiple members of a class.
   *
   * @param keyLookupFn function extracting multiple members from a entity representing composite key
   */
  static KeyExtractor.Factory forMultipleMembers(Function<Class<?>, List<Member>> keyLookupFn) {
    Objects.requireNonNull(keyLookupFn, "keyLookupFn");
    return entityType -> new KeyExtractors.MultipleMembers(entityType, keyLookupFn.apply(entityType));
  }

  /**
   * Creates extractor from a generic java function. Metadata on keys will not be available.
   */
  static KeyExtractor.Factory generic(UnaryOperator<Object> extractor) {
    Objects.requireNonNull(extractor, "extractor");
    return entityType -> new KeyExtractors.Generic(entityType, extractor);
  }

  /**
   * Similar to <a href="https://en.wikipedia.org/wiki/Null_object_pattern">Null Object Pattern</a>
   * creates an extractor without any key. Throws exception on any extract method call. Useful when
   * entity doesn't have any key.
   */
  static KeyExtractor.Factory noKey() {
    return KeyExtractors.NoKey::new;
  }

}
