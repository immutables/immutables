/*
    Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.builder.fixture;

import java.lang.annotation.RetentionPolicy;
import javax.annotation.Nullable;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

@Value.Style
class FactoryParameters {
  @Builder.Factory
  public static String factory1(int theory, String reality, @Nullable @Builder.Parameter Void evidence) {
    return theory + " != " + reality + ", " + evidence;
  }

  @Builder.Factory
  public static String factory2(@Builder.Parameter int theory, @Builder.Parameter String reality) {
    return theory + " != " + reality;
  }

  @SuppressWarnings("unused")
  @Builder.Factory
  public static String throwing() throws Exception, Error {
    throw new Exception();
  }
}

@Value.Style(newBuilder = "newBuilder")
class FactoryParametersAndSwitchers {

  @Builder.Factory
  public static String factory3(@Builder.Parameter int theory, String reality) {
    return theory + " != " + reality;
  }

  @Builder.Factory
  public static String factory4(@Builder.Parameter int value, @Builder.Switch RetentionPolicy policy) {
    return policy + "" + value;
  }

  @Builder.Factory
  public static String factory5(@Builder.Switch(defaultName = "SOURCE") RetentionPolicy policy) {
    return policy.toString();
  }
}

@Value.Style(strictBuilder = true)
class FactoryStrictSwitches {
  @Builder.Factory
  public static String factory6(@Builder.Parameter int theory, String reality) {
    return theory + " != " + reality;
  }

  @Builder.Factory
  public static String factory7(
      @Builder.Parameter int value,
      @Builder.Switch(defaultName = "CLASS") RetentionPolicy policy) {
    return policy + "" + value;
  }
}
