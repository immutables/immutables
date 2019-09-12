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
import org.immutables.criteria.repository.Facet;
import org.immutables.criteria.repository.reactive.ReactiveReadable;
import org.immutables.criteria.repository.reactive.ReactiveRepository;
import org.immutables.criteria.repository.reactive.ReactiveWritable;
import org.immutables.criteria.repository.sync.SyncReadable;
import org.immutables.criteria.repository.sync.SyncRepository;
import org.immutables.criteria.repository.sync.SyncWritable;
import org.immutables.value.processor.meta.ProcessorRule;
import org.immutables.value.processor.meta.RepositoryModel;
import org.immutables.value.processor.meta.ValueType;
import org.junit.Rule;
import org.junit.Test;

import javax.lang.model.element.TypeElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.immutables.check.Checkers.check;

/**
 * Validation for repository model. It is the one generated in {@link RepositoryModel}
 * @see ValueType
 */
public class RepositoryTest {

  @Rule // TODO migrate to JUnit5 Extension
  public final ProcessorRule rule = new ProcessorRule();

  @Test
  public void reactive() {
    ValueType value = rule.value(ReactiveModel.class);
    RepositoryModel repository = value.getCriteriaRepository();
    check(repository.facets()).hasSize(2);

    assertFacet(findFacet(repository, ReactiveRepository.Readable.class), ReactiveRepository.Readable.class);
    assertFacet(findFacet(repository, ReactiveRepository.Writable.class), ReactiveRepository.Writable.class);
  }

  @Test
  public void sync() {
    ValueType value = rule.value(SyncModel.class);
    RepositoryModel repository = value.getCriteriaRepository();
    check(repository.facets()).hasSize(2);

    assertFacet(findFacet(repository, SyncRepository.Readable.class), SyncRepository.Readable.class);
    assertFacet(findFacet(repository, SyncRepository.Writable.class), SyncRepository.Writable.class);
  }

  @Test
  public void empty() {
    ValueType value = rule.value(EmptyModel.class);
    check(value.getCriteriaRepository().facets()).isEmpty();
  }

  /**
   * Find expected facet in repository model
   */
  private RepositoryModel.Facet findFacet(RepositoryModel repository, Class<? extends Facet> type) {
    check(repository.facets()).notEmpty();
    TypeElement toFind = rule.elements().getTypeElement(type.getCanonicalName());
    for (RepositoryModel.Facet facet: repository.facets()) {
      if (rule.types().asElement(facet.interfaceType()).equals(toFind)) {
        return facet;
      }
    }

    throw new IllegalArgumentException("Facet not found: " + type.getName());
  }

  /**
   * Series of checks on existing facet
   */
  private void assertFacet(RepositoryModel.Facet facet, Class<? extends Facet> type) {
    // compare name
    check(facet.name()).is(type.getSimpleName().toLowerCase());

    // check interface type
    TypeElement canonical = rule.elements().getTypeElement(type.getCanonicalName());
    check(rule.types().asElement(facet.interfaceType())).is(canonical);

    // non-default non-Object methods are implemented
    final List<Method> methods = new ArrayList<>(Arrays.asList(type.getMethods()));
    methods.removeAll(Arrays.asList(Object.class.getMethods()));

    final List<String> actual = facet.methods().stream().map(RepositoryModel.DelegateMethod::name).collect(Collectors.toList());
    final List<String> expected = methods.stream().map(Method::getName).collect(Collectors.toList());
    check(actual).hasContentInAnyOrder(expected);
  }

  @ProcessorRule.TestImmutable
  @Criteria.Repository(facets = {ReactiveWritable.class, ReactiveReadable.class})
  interface ReactiveModel {
    String string();
  }

  @ProcessorRule.TestImmutable
  @Criteria.Repository(facets = {SyncReadable.class, SyncWritable.class})
  interface SyncModel {
    String string();
  }

  @ProcessorRule.TestImmutable
  @Criteria.Repository(facets = {})
  interface EmptyModel {
    String string();
  }

}