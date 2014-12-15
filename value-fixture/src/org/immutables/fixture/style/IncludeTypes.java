package org.immutables.fixture.style;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import org.immutables.value.Value;

/**
 * Feature combination
 * <ul>
 * <li>Included on types and packages
 * <li>Included as nested types
 * <li>Package style application
 * </ul>
 */
@Value.Immutable.Include({Serializable.class})
@Value.Immutable
public class IncludeTypes {

  void use() {
    // this immutable type
    ImmutableIncludeTypes.of();
    // included on this type
    ImmutableSerializable.of();
    // included on package
    ImmutableTicker.builder().read(1).build();

    // included in IncludeNestedTypes
    ImmutableIncludeNestedTypes.Retention retention =
        ImmutableIncludeNestedTypes.Retention.builder()
            .value(RetentionPolicy.CLASS)
            .build();

    // included in IncludeNestedTypes
    ImmutableIncludeNestedTypes.Target target =
        ImmutableIncludeNestedTypes.Target.builder()
            .value(ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE)
            .build();

    // package applied style "copyWith*" test
    // see PackageStyle
    retention.copyWithValue(RetentionPolicy.RUNTIME);
    target.copyWithValue(ElementType.CONSTRUCTOR, ElementType.LOCAL_VARIABLE);
  }
}
