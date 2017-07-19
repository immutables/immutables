package org.immutables.fixture.builder;

public class ThirdPartyImmutableWithBuilderClassCopyMethod {
  private final String value;

  private ThirdPartyImmutableWithBuilderClassCopyMethod(String value) {
    this.value = value;
  }

  public static Builder generateNewBuilder() {
    return new Builder();
  }


  public String getValue() {
    return value;
  }
  public static class Builder {

    private String value;

    protected Builder() {
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }

    public ThirdPartyImmutableWithBuilderClassCopyMethod doTheBuild() {
      return new ThirdPartyImmutableWithBuilderClassCopyMethod(value);
    }

    public static Builder builderFromValue(ThirdPartyImmutableWithBuilderClassCopyMethod immutable) {
      return new Builder().setValue(immutable.getValue());
    }
  }
}
