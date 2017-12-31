package org.immutables.fixture.style;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(stagedBuilder = true)
abstract class StagedBuilderSuperBuilder {
  abstract String name();
  abstract int age();
  abstract static class Builder {}
}

@Value.Immutable
@Value.Style(stagedBuilder = true)
interface StagedBuilderExtendingBuilder {
  String name();
  int age();
  class Builder extends ImmutableStagedBuilderExtendingBuilder.Builder {}
}

@Value.Immutable
@Value.Style(stagedBuilder = true)
public interface StagedBuilderImplementingBuilder {
	String name();
	int age();
  interface Builder {}
}