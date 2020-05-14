/*
   Copyright 2018 Immutables Authors and Contributors

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
package org.immutable.fixture.annotate;

import java.lang.reflect.Method;
import org.immutable.fixture.annotate.InjAnn.OnTypeAndAccessorCascadeToInitializerInterpolate;
import org.immutable.fixture.annotate.InjAnn.ToInj;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

// Validates presense of injected annotations
public class AnnotateTest {
  @Test
  public void hasToInjPresentFromPackage() throws Exception {
    Method method = ImmutableCtopParamFromPackageIfPresent.class.getDeclaredMethod("of", int.class);
    check(method.getParameterAnnotations()[0][0]).isA(ToInj.class);
  }

  @Test
  public void fieldAnnotationFromType() throws Exception {
    ToInj a = ImmutableOnTypeToField.class.getDeclaredField("a").getAnnotation(ToInj.class);
    check(a.a()).is(21);
    check(a.b()).is("a");

    ToInj b = ImmutableOnTypeToField.class.getDeclaredField("b").getAnnotation(ToInj.class);
    check(b.a()).is(21);
    check(b.b()).is("b");
  }

  @Test
  public void fieldAnnotationFromAccessor() throws Exception {
    ToInj a = ImmutableOnAccessorToField.class.getDeclaredField("a").getAnnotation(ToInj.class);
    check(a.a()).is(44);
    check(a.b()).is("");
  }

  @Test
  public void attributeNamesArray() throws Exception {
    ToInj a = ImmutableOnAccessorToField.class.getAnnotation(ToInj.class);
    check(a.a()).is(0);
    check(a.attrs()).isOf("a", "b");
  }

  @Test
  public void typeAndBuilderFromType() throws Exception {
    ToInj t = ImmutableOnTypeAndBuilder.class.getAnnotation(ToInj.class);
    check(t.a()).is(33);
    ToInj b = ImmutableOnTypeAndBuilder.Builder.class.getAnnotation(ToInj.class);
    check(b.a()).is(33);
    ToInj m = ModifiableOnTypeAndBuilder.class.getAnnotation(ToInj.class);
    check(m.a()).is(33);
  }

  @Test
  public void syntheticField() throws Exception {
    ToInj h = ModifiableOnTypeAndAccessorCascadeToInitializerInterpolate.class
        .getDeclaredField("initBits")
        .getAnnotation(ToInj.class);

    // this one is injected from accessor
    check(h.a()).is(71);
    check(h.b()).is("synthetic of " + OnTypeAndAccessorCascadeToInitializerInterpolate.class.getSimpleName());
  }

  @Test
  public void initializersFromTypeAndCascade() throws Exception {
    ToInj h = ImmutableOnTypeAndAccessorCascadeToInitializerInterpolate.Builder.class
        .getDeclaredMethod("h", int.class)
        .getAnnotation(ToInj.class);

    // this one is injected from accessor
    check(h.a()).is(15);
    check(h.b()).is("EF");

    ToInj x = ImmutableOnTypeAndAccessorCascadeToInitializerInterpolate.Builder.class
        .getDeclaredMethod("x", String.class)
        .getAnnotation(ToInj.class);

    // this one is injected from type
    check(x.a()).is(31);
    check(x.b()).is("UO");
  }

  @Test
  public void settersFromTypeAndCascade() throws Exception {
    ToInj h = ModifiableOnTypeAndAccessorCascadeToInitializerInterpolate.class
        .getDeclaredMethod("setH", int.class)
        .getAnnotation(ToInj.class);

    // this one is injected from accessor
    check(h.a()).is(15);
    check(h.b()).is("EF");

    ToInj x = ModifiableOnTypeAndAccessorCascadeToInitializerInterpolate.class
        .getDeclaredMethod("setX", String.class)
        .getAnnotation(ToInj.class);

    // this one is injected from type
    check(x.a()).is(31);
    check(x.b()).is("UO");
  }
  
  @Test
	public void manyRepeatableAnnotationInjections() {
    ToInj t = ImmutableOnTypeAndBuilderMany.class.getAnnotation(ToInj.class);
    check(t.a()).is(1);
    ToInj b = ImmutableOnTypeAndBuilderMany.Builder.class.getAnnotation(ToInj.class);
    check(b.a()).is(2);
	}
}
