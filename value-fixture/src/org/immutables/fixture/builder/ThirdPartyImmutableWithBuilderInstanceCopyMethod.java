package org.immutables.fixture.builder;

public class ThirdPartyImmutableWithBuilderInstanceCopyMethod {
  private final String value;

  private ThirdPartyImmutableWithBuilderInstanceCopyMethod(String value) {
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

    public ThirdPartyImmutableWithBuilderInstanceCopyMethod doTheBuild() {
      return new ThirdPartyImmutableWithBuilderInstanceCopyMethod(value);
    }

    public Builder merge(ThirdPartyImmutableWithBuilderInstanceCopyMethod immutable) {
      return setValue(immutable.getValue());
    }
  }
}
