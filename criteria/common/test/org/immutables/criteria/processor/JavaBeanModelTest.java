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
import org.immutables.value.processor.meta.ProcessorRule;
import org.immutables.value.processor.meta.ValueAttribute;
import org.immutables.value.processor.meta.ValueType;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.immutables.check.Checkers.check;

public class JavaBeanModelTest {

  @Rule // TODO migrate to JUnit5 Extension
  public final ProcessorRule rule = new ProcessorRule();

  @Test
  public void javaBean() {
    ValueType valueType = rule.value(Model1.class);
    check(valueType.constitution.protoclass().criteria().isPresent());
    check(valueType.constitution.protoclass().criteriaRepository().isPresent());
    check(valueType.typeDocument().toString()).is(Model1.class.getCanonicalName());
    check(valueType.attributes).notEmpty();

    List<String> attributes = valueType.allMarshalingAttributes().stream().map(ValueAttribute::name).collect(Collectors.toList());

    check(attributes).hasContentInAnyOrder("set", "foo", "base", "dep", "deps", "nullableDep");
    // java.lang.Object should not be included
    check(attributes).not().hasContentInAnyOrder("equals", "hashCode", "toString", "getClass");
    check(attributes).not().hasContentInAnyOrder("ignore", "missingField");
  }

  @Test
  public void attributes() {
    ValueType valueType = rule.value(Model1.class);
    Function<String, ValueAttribute> findFn = name -> valueType.attributes.stream().filter(a -> a.name().equals(name)).findAny().get();

    check(findFn.apply("foo").getType()).is("int");
    ValueAttribute dep = findFn.apply("dep");
    check(dep.hasCriteria());
    check(dep.criteria().matcher().creator()).contains("DepCriteria.creator()");
    check(dep.criteria().matcher().creator()).not().contains("DepCriteriaTemplate.creator()");
  }

  @Test
  public void nullable() {
    ValueType valueType = rule.value(Model1.class);
    Function<String, ValueAttribute> findFn = name -> valueType.attributes.stream().filter(a -> a.name().equals(name)).findAny().get();
    check(findFn.apply("nullableDep").isNullable());
  }

  @ProcessorRule.TestImmutable
  @Criteria
  @Criteria.Repository
  static class Model1  extends Base {

    private boolean set;
    private int foo;
    private List<Dep> deps;

    private Dep dep;

    private Dep nullableDep;

    public int getFoo() {
      return foo;
    }

    public void setFoo(int foo) {
      this.foo = foo;
    }

    public boolean isSet() {
      return set;
    }

    public void setSet(boolean set) {
      this.set = set;
    }

    public Dep getDep() {
      return dep;
    }

    public List<Dep> getDeps() {
      return deps;
    }

    @Nullable
    public Dep getNullableDep() {
      return nullableDep;
    }

    /**
     * Should not be included in attribute list. Since not a java bean getter
     */
    public int ignore() {
      return 0;
    }

    /**
     * Should not be in the criteria because there is no such field as {@code missingField}
     */
    public int getMissingField() {
      return 0;
    }
  }

  /**
   * Used for testing nested criterias
   */
  @ProcessorRule.TestImmutable
  @Criteria
  static class Dep {

    private final Date date = new Date();

    public Date getDate() {
      return date;
    }


  }

  static abstract class Base {
    private String base;

    public String getBase() {
      return base;
    }

    public void setBase(String base) {
      this.base = base;
    }
  }
}
