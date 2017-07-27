package org.immutables.fixture.builder.attribute_builders;

public class ThirdPartyImmutableWithValueClassCopyMethod {
  private final String value;

  private ThirdPartyImmutableWithValueClassCopyMethod(String value) {
    this.value = value;
  }

  public static Builder generateNewBuilder() {
    return new Builder();
  }

  public static Builder geterateNewBuilderFrom(ThirdPartyImmutableWithValueClassCopyMethod third) {
    return generateNewBuilder().setValue(third.getValue());
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

    public ThirdPartyImmutableWithValueClassCopyMethod doTheBuild() {
      return new ThirdPartyImmutableWithValueClassCopyMethod(value);
    }
  }
}
