package org.immutables.fixture.jackson;

import java.util.Optional;
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

@Value.Immutable(builder = false)
@Value.Style(
    jdkOnly = true,
    overshadowImplementation = true,
    implementationNestedInBuilder = true,
    visibility = Value.Style.ImplementationVisibility.PACKAGE)
@JsonDeserialize(as = ImmutablePackageNoBuilderHidden.class)
interface PackageNoBuilderHidden {
  List<String> getStrings();

  Optional<Integer> getInt();
}
