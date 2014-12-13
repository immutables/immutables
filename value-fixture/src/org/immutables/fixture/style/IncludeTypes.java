package org.immutables.fixture.style;

import org.immutables.fixture.style.ImmutableIncludeNestedTypes.Retention;
import org.immutables.fixture.style.ImmutableIncludeNestedTypes.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.io.Serializable;
import org.immutables.value.Value;

@Value.Immutable.Include({Serializable.class})
@Value.Immutable
public class IncludeTypes {

  void use() {
    ImmutableIncludeTypes.of();
    ImmutableSerializable.of();
    Retention retention = ImmutableIncludeNestedTypes.Retention.builder()
        .value(RetentionPolicy.CLASS)
        .build();
    Target target = ImmutableIncludeNestedTypes.Target.builder()
        .value(ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE)
        .build();

    retention.withValue(RetentionPolicy.RUNTIME);
    target.withValue(ElementType.CONSTRUCTOR, ElementType.LOCAL_VARIABLE);
  }
}
