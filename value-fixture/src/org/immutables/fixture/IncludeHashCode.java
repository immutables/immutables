package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Style(includeHashCode = "([[type]].class.hashCode() + 42)")
@Value.Immutable
@Value.Modifiable
public class IncludeHashCode {}

@Value.Style(includeHashCode = "super.hashCode()")
@Value.Immutable
interface JustCompileItSuperHashCode {
  int a();
  String b();
}
