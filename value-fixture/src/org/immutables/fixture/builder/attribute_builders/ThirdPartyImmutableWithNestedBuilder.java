package org.immutables.fixture.builder.attribute_builders;

public class ThirdPartyImmutableWithNestedBuilder {

  private final String value;

  private ThirdPartyImmutableWithNestedBuilder(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static class Builder {

    private String value;

    public Builder() {
    }

    public Builder(ThirdPartyImmutableWithNestedBuilder copy) {
      this.value = copy.getValue();
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }

    public ThirdPartyImmutableWithNestedBuilder doTheBuild() {
      return new ThirdPartyImmutableWithNestedBuilder(value);
    }
  }
}
