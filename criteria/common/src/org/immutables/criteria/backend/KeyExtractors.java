/*
 * Copyright 2020 Immutables Authors and Contributors
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Path;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Various implementations supporting {@link KeyExtractor} interface. Hides all
 * implementations in package-private class.
 */
final class KeyExtractors {

  /**
   * Extractor based on generic {@link Function}. Does not contain any key expression metadata
   */
  static class Generic implements KeyExtractor {

    private final Class<?> entityType;
    private final Function<Object, Object> extractor;

    Generic(Class<?> entityType, Function<Object, Object> extractor) {
      this.entityType = entityType;
      this.extractor = Objects.requireNonNull(extractor, "extractor");
    }

    @Override
    public Object extract(Object instance) {
      return extractor.apply(instance);
    }

    @Override
    public KeyMetadata metadata() {
      return new KeyMetadata() {

        @Override
        public boolean isExpression() {
          return false;
        }

        @Override
        public List<Expression> keys() {
          throw new UnsupportedOperationException("This extractor is defined for generic java function. " +
                  "Key AST (as Expression) is unknown for " + entityType.getName()
                  + " by current " + Generic.class.getSimpleName() + " extractor. Did you check isExpression() ?");
        }

        @Override
        public boolean isKeyDefined() {
          return true;
        }
      };
    }
  }

  /**
   * Empty extractor which always throws exceptions
   */
  static class NoKey implements KeyExtractor {

    private final Class<?> entityType;

    NoKey(Class<?> entityType) {
      this.entityType = Objects.requireNonNull(entityType, "entityType");
    }

    @Override
    public Object extract(Object instance) {
      throw new UnsupportedOperationException(String.format("Entity %s does not have a key defined. " +
              " extract() method is not supposed to be called on %s using this class (%s). " +
              "Did you check for metadata().isKeyDefined() ?", entityType.getName(),
              NoKey.class.getSimpleName(),
              instance == null ? null : instance.getClass().getSimpleName()));
    }

    @Override
    public KeyMetadata metadata() {
      return new KeyMetadata() {
        @Override
        public boolean isKeyDefined() {
          return false;
        }

        @Override
        public boolean isExpression() {
          return false;
        }

        @Override
        public List<Expression> keys() {
          return Collections.emptyList();
        }
      };
    }
  }

  /**
   * Extractor based on a single member (field or method)
   */
  static class SingleMember implements KeyExtractor {

    private final Member member;
    private final List<Expression> keys;
    private final org.immutables.criteria.reflect.MemberExtractor extractor;

    SingleMember(Class<?> entityType, Member member) {
      this.member = Objects.requireNonNull(member, "member");
      this.keys = Collections.singletonList(Path.ofMember(member));
      this.extractor = org.immutables.criteria.reflect.MemberExtractor.ofReflection();
    }

    @Override
    public Object extract(Object instance) {
      return extractor.extract(member, instance);
    }

    @Override
    public KeyMetadata metadata() {
      return new KeyMetadata() {
        @Override
        public boolean isExpression() {
          return true;
        }

        @Override
        public List<Expression> keys() {
          return keys;
        }
      };
    }
  }

  static class MultipleMembers implements KeyExtractor {

    private final Class<?> entityType;
    private final List<Member> members;
    private final List<Expression> keys;
    private final org.immutables.criteria.reflect.MemberExtractor extractor;

    MultipleMembers(Class<?> entityType, List<Member> members) {
      this.entityType = Objects.requireNonNull(entityType, "entityType");
      Preconditions.checkArgument(!members.isEmpty(), "No keys defined for %s", entityType);
      this.members = ImmutableList.copyOf(members);
      this.keys = ImmutableList.copyOf(members.stream().map(Path::ofMember).collect(Collectors.toList()));
      this.extractor = org.immutables.criteria.reflect.MemberExtractor.ofReflection();
    }

    @Override
    public Object extract(Object instance) {
      return members.stream()
              .map(m -> extractor.extract(m, instance))
              .collect(Collectors.toList());
    }

    @Override
    public KeyMetadata metadata() {
      return new KeyMetadata() {

        @Override
        public boolean isExpression() {
          return true;
        }

        @Override
        public List<Expression> keys() {
          return keys;
        }
      };
    }

  }

  private KeyExtractors() {}
}
