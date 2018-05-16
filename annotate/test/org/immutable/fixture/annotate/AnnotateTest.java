package org.immutable.fixture.annotate;

import java.lang.reflect.Method;
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
  public void typeAndBuilderFromType() throws Exception {
    ToInj t = ImmutableOnTypeAndBuilder.class.getAnnotation(ToInj.class);
    check(t.a()).is(33);
    ToInj b = ImmutableOnTypeAndBuilder.Builder.class.getAnnotation(ToInj.class);
    check(b.a()).is(33);
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
}
