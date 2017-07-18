package org.immutables.fixture.builder;

public class ThirdPartyImmutableWithValueInstanceCopyMethod {
  private final String value;

  private ThirdPartyImmutableWithValueInstanceCopyMethod(String value) {
    this.value = value;
  }

  public static Builder generateNewBuilder() {
    return new Builder();
  }

  public String getValue() {
    return value;
  }

  public Builder toBuilder() {
    return new Builder().setValue(getValue());
  }

  public static class Builder {
    private String value;

    protected Builder() {
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }

    public ThirdPartyImmutableWithValueInstanceCopyMethod build() {
      return new ThirdPartyImmutableWithValueInstanceCopyMethod(value);
    }
  }
}
