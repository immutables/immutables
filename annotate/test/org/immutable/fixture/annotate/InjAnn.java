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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.immutables.annotate.InjectAnnotation;
import org.immutables.annotate.InjectAnnotation.Where;
import org.immutables.value.Value;

public interface InjAnn {
  @Retention(RetentionPolicy.RUNTIME)
  @interface ToInj {
    int a();

    String b() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @InjectAnnotation(code = "(a=21, b=\"[[!name]]\")", type = ToInj.class, target = Where.FIELD)
  @interface An1 {}

  @Retention(RetentionPolicy.RUNTIME)
  @InjectAnnotation(code = "([[*]])", type = ToInj.class, target = Where.FIELD)
  @interface Bn2 {
    int a();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @InjectAnnotation(code = "@org.immutable.fixture.annotate.InjAnn.ToInj"
      + "(a=[[c]], b=[[d]])",
      target = Where.INITIALIZER)
  @interface Cn3 {
    int c();

    String d();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @InjectAnnotation(code = "(a=33)",
      type = ToInj.class,
      target = {Where.BUILDER_TYPE, Where.IMMUTABLE_TYPE, Where.MODIFIABLE_TYPE})
  @interface ImB {}

  @Retention(RetentionPolicy.RUNTIME)
  @InjectAnnotation(code = "(a=71, b=\"synthetic of [[!name]]\")",
      type = ToInj.class,
      target = {Where.SYNTHETIC_FIELDS})
  @interface BmS {}

  @Retention(RetentionPolicy.RUNTIME)
  @InjectAnnotation(type = ToInj.class, ifPresent = true, target = Where.CONSTRUCTOR_PARAMETER)
  @interface Ifp {}

  @Value.Immutable
  interface CtopParamFromPackageIfPresent {
    @Value.Parameter
    @ToInj(a = 1)
    int a();
  }

  @Value.Immutable
  @An1
  interface OnTypeToField {
    int a();

    int b();
  }

  @Value.Immutable
  interface OnAccessorToField {
    @Bn2(a = 44)
    int a();

    int b();
  }

  @Value.Immutable
  @Value.Modifiable
  @Cn3(c = 31, d = "UO")
  @BmS
  interface OnTypeAndAccessorCascadeToInitializerInterpolate {
    @Cn3(c = 15, d = "EF")
    int h();

    String x();
  }

  @Value.Immutable
  @Value.Modifiable
  @ImB
  interface OnTypeAndBuilder {}
}
