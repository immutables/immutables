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

    check(attributes).hasContentInAnyOrder("set", "foo", "base", "dep", "deps", "nullableDep", "boxedInteger");
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

  /**
   * JavaBean(s) have nullable types by default (except for primitives / optionals / iterables)
   */
  @Test
  public void nullableByDefault() {
    ValueType valueType = rule.value(Model1.class);
    Function<String, ValueAttribute> findFn = name -> valueType.attributes.stream().filter(a -> a.name().equals(name)).findAny().get();

    // TODO: generate optional version of criteria ?
    check(findFn.apply("dep").isNullable());

    // boxed versions are nullable
    check(findFn.apply("boxedInteger").isNullable());

    // base is string (and can be null)
    check(findFn.apply("base").isNullable());

    // exclude nullable property from primitives / collections and optionals
    // foo is int (primitive)
    check(!findFn.apply("foo").isNullable());
    // set is boolean
    check(!findFn.apply("set").isNullable());
    // deps is collection
    check(!findFn.apply("deps").isNullable());
  }

  @Test
  public void visibility() {
    ValueType valueType = rule.value(VisibilityModel.class);
    List<String> names = valueType.attributes.stream().map(ValueAttribute::name).collect(Collectors.toList());
    // only public getters should be visible
    check(names).hasContentInAnyOrder("publicField");
  }

  @Test
  public void wierdBean1() {
    ValueType valueType = rule.value(WeirdLegacyBean1.class);
    List<String> names = valueType.attributes.stream().map(ValueAttribute::name).collect(Collectors.toList());
    check(names).hasContentInAnyOrder("foo");
    check(names).not().has("_foo");
  }

  @Test
  public void wierdBean2() {
    ValueType valueType = rule.value(WeirdLegacyBean2.class);
    List<String> names = valueType.attributes.stream().map(ValueAttribute::name).collect(Collectors.toList());
    check(names).hasContentInAnyOrder("foo", "BAR", "a", "AB", "ABC");
    check(names).not().hasContentInAnyOrder("Foo", "A", "Ab", "ab", "ab", "Abc");
  }

  /**
   * Sometimes bean accessors don't have associated fields.
   * It is somewhat questionable wherever they should be included as attributes or not.
   * Right now the approach is conservative and getter / setter / field are all required.
   * Having a test to raise awareness about this usecase.
   */
  @Test
  public void beanWithoutFields() {
    ValueType valueType = rule.value(BeanWithoutFields.class);
    List<String> names = valueType.attributes.stream().map(ValueAttribute::name).collect(Collectors.toList());
    check(names).isEmpty();
  }

  @Test
  public void beanWithUpperCaseAttributes() {
    ValueType valueType = rule.value(BeanWithUpperCaseAttributes.class);
    List<String> names = valueType.attributes.stream().map(ValueAttribute::name).collect(Collectors.toList());
    check(names).hasContentInAnyOrder("a", "ab", "abc");
  }

  @Test
  public void invalidIsGetter() {
    ValueType valueType = rule.value(InvalidIsGetter.class);
    List<String> names = valueType.attributes.stream().map(ValueAttribute::name).collect(Collectors.toList());
    check(names).isEmpty();
  }

  @ProcessorRule.TestImmutable
  @Criteria
  @Criteria.Repository
  static class Model1  extends Base {

    private boolean set;
    private int foo;
    private List<Dep> deps;

    private Integer boxedInteger;

    private Dep dep;

    @Nullable
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

    public void setDeps(List<Dep> deps) {
      this.deps = deps;
    }

    public void setDep(Dep dep) {
      this.dep = dep;
    }

    public void setNullableDep(Dep nullableDep) {
      this.nullableDep = nullableDep;
    }

    public Integer getBoxedInteger() {
      return boxedInteger;
    }

    public void setBoxedInteger(Integer boxedInteger) {
      this.boxedInteger = boxedInteger;
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

  /**
   * Model to test getter setters
   */
  @ProcessorRule.TestImmutable
  @Criteria
  @SuppressWarnings("unused")
  static class VisibilityModel {
    public int publicField;
    int packageField;
    private int privateField;
    protected int protectedField;

    static private int staticField;

    // only public getter
    public int getPublicField() {
      return publicField;
    }

    int getPackageField() {
      return packageField;
    }

    private int getPrivateField() {
      return privateField;
    }

    protected int getProtectedField() {
      return protectedField;
    }

    public static int getStaticField() {
      return staticField;
    }

    public void setPublicField(int publicField) {
      this.publicField = publicField;
    }

    public void setPackageField(int packageField) {
      this.packageField = packageField;
    }

    public void setPrivateField(int privateField) {
      this.privateField = privateField;
    }

    public void setProtectedField(int protectedField) {
      this.protectedField = protectedField;
    }

  }

  @ProcessorRule.TestImmutable
  @Criteria
  static class WeirdLegacyBean1 {
    private String _foo; // some generators have under-score

    public String getFoo() {
      return _foo;
    }

    public void setFoo(String _foo) {
      this._foo = _foo;
    }
  }


  @ProcessorRule.TestImmutable
  @Criteria
  static class WeirdLegacyBean2 {
    private String Foo; // some generators don't use lower-case attributes

    private int a;
    private int AB; // See 8.8 Capitalization of inferred names
    private int ABC; 

    private String BAR;

    public String getFoo() {
      return Foo;
    }

    public void setFoo(String foo) {
      Foo = foo;
    }

    public String getBAR() {
      return BAR;
    }

    public void setBAR(String BAR) {
      this.BAR = BAR;
    }

    public int getA() {
      return a;
    }

    public void setA(int a) {
      this.a = a;
    }

    public int getAB() {
      return AB;
    }

    public void setAB(int AB) {
      this.AB = AB;
    }

    public int getABC() {
      return ABC;
    }

    public void setABC(int ABC) {
      this.ABC = ABC;
    }
  }


  @ProcessorRule.TestImmutable
  @Criteria
  static class BeanWithoutFields {
    // intentionally no foo field

    public String getFoo() {
      return toString();
    }

    public void setFoo(String foo) {
      // nop
    }
  }

  @ProcessorRule.TestImmutable
  @Criteria
  static class BeanWithUpperCaseAttributes {
    private String A;
    private String Ab;
    private String Abc;

    public String getA() {
      return A;
    }

    public void setA(String a) {
      A = a;
    }

    public String getAb() {
      return Ab;
    }

    public void setAb(String ab) {
      Ab = ab;
    }

    public String getAbc() {
      return Abc;
    }

    public void setAbc(String abc) {
      Abc = abc;
    }
  }

  @ProcessorRule.TestImmutable
  @Criteria
  static class InvalidIsGetter {

    private int a;
    private String is2;

    public boolean is() {
      return false;
    }

    public int isA() {
      return a;
    }

    public String is2() {
      return is2;
    }

    public void setA(int a) {
      this.a = a;
    }

    public void setIs2(String is2) {
      this.is2 = is2;
    }
  }

}
