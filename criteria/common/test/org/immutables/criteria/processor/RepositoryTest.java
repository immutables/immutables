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

package org.immutables.criteria.processor;

import org.immutables.criteria.Criteria;
import org.immutables.criteria.repository.reactive.ReactiveReadable;
import org.immutables.criteria.repository.reactive.ReactiveRepository;
import org.immutables.criteria.repository.reactive.ReactiveWritable;
import org.immutables.value.processor.meta.MoreElements;
import org.immutables.value.processor.meta.ProcessorRule;
import org.immutables.value.processor.meta.ValueType;
import org.immutables.value.processor.meta.ValueTypeRepository;
import org.junit.Rule;
import org.junit.Test;

import javax.lang.model.element.TypeElement;
import java.util.function.IntFunction;

import static org.immutables.check.Checkers.check;

public class RepositoryTest {

  @Rule
  public final ProcessorRule rule = new ProcessorRule();

  @Test
  public void basic() {
    ValueType value = rule.value(Model.class);
    ValueTypeRepository repository = value.getCriteriaRepository();
    check(repository.facets()).hasSize(2);
    IntFunction<TypeElement> get = i -> MoreElements.asType(rule.types().asElement(repository.facets().get(i).interfaceType()));
    check(get.apply(0).getQualifiedName().toString()).is(ReactiveRepository.Writable.class.getCanonicalName());
    check(get.apply(1).getQualifiedName().toString()).is(ReactiveRepository.Readable.class.getCanonicalName());
  }

  @ProcessorRule.TestImmutable
  @Criteria.Repository(facets = {ReactiveWritable.class, ReactiveReadable.class})
  interface Model {
    String string();
  }



}