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

package org.immutables.fixture;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

class HashTest {
  @Test
  void includeHash() {
    check(ImmutableIncludeHashCode.builder().build().hashCode())
        .is(IncludeHashCode.class.hashCode() + 42);
  }

  @Test
  void lazyHash() throws NoSuchFieldException {
    ImmutableLazyHash h1 = ImmutableLazyHash.builder().s("a").i(1).b(true).build();
    ImmutableLazyHash h2 = ImmutableLazyHash.builder().s("b").i(2).b(false).build();

    check(h1.hashCode()).not().is(0); // ensure hashcode is computed
    check(h1.hashCode()).not().is(h2.hashCode());
    check(h1.equals(ImmutableLazyHash.builder().s("a").i(1).b(true).build()));
    check(!h1.equals(h2));

    checkHashCodeFieldIsTransient(ImmutableLazyHash.class);
  }

  @Test
  void lazyHashSerializable() throws NoSuchFieldException {
    ImmutableLazyHashSerializable h1 = ImmutableLazyHashSerializable.builder().s("a").i(1).b(true).build();
    ImmutableLazyHashSerializable h2 = ImmutableLazyHashSerializable.builder().s("b").i(2).b(false).build();

    check(h1.hashCode()).not().is(0); // ensure hashcode is computed
    check(h1.hashCode()).not().is(h2.hashCode());
    check(h1.equals(ImmutableLazyHashSerializable.builder().s("a").i(1).b(true).build()));
    check(!h1.equals(h2));

    checkHashCodeFieldIsTransient(ImmutableLazyHashSerializable.class);
  }

  @SuppressWarnings("MethodCanBeStatic")
  private void checkHashCodeFieldIsTransient(Class<?> clazz) throws NoSuchFieldException {
    // ensure hashCode field is transient
    Field field = clazz.getDeclaredField("hashCode");
    field.setAccessible(true);
    check("hashCode should be transient", Modifier.isTransient(field.getModifiers()));
  }
}
