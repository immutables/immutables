package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
    jdkOnly = true,
    implementationNestedInBuilder = true,
    visibility = Value.Style.ImplementationVisibility.PACKAGE,
    builderVisibility = Value.Style.BuilderVisibility.PUBLIC)
@JsonDeserialize(as = PackageHiddenBuilder.ImmutablePackageHidden.class)
public interface PackageHidden {
  List<String> getStrings();
}
