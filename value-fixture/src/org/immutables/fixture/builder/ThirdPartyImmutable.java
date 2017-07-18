package org.immutables.fixture.builder;

public class ThirdPartyImmutable {
  private final String value;

  private ThirdPartyImmutable(String value) {
    this.value = value;
  }

  public static Builder generateNewBuilder() {
    return new Builder();
  }

  public static Builder generateNewBuilder(ThirdPartyImmutable immutable) {
    return Builder.builderFromValue(immutable);
  }


  public String getValue() {
    return value;
  }

  public Builder toBuilder() {
    return Builder.builderFromValue(this);
  }

  public static class Builder {
    private String value;

    protected Builder() {
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }

    public ThirdPartyImmutable doTheBuild() {
      return new ThirdPartyImmutable(value);
    }

    public static Builder builderFromValue(ThirdPartyImmutable immutable) {
      return new Builder().setValue(immutable.getValue());
    }
  }
}
