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

}

@Value.Style(builder = "newBuilder")
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
  public static String factory5(@Builder.Switch(defaultOrdinal = 0) RetentionPolicy policy) {
    return policy.toString();
  }
}
