package org.immutables.fixture.builder.attribute_builders;

public class ThirdPartyImmutableWithPrimitive {

    private final int value;

    private ThirdPartyImmutableWithPrimitive(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Builder generateNewBuilder() {
        return new Builder();
    }

    public static Builder generateNewBuilder(ThirdPartyImmutableWithPrimitive immutable) {
        return Builder.builderFromValue(immutable);
    }


    public Builder toBuilder() {
        return Builder.builderFromValue(this);
    }

    public static class Builder {

        private int value;

        protected Builder() {
        }

        public Builder setValue(int value) {
            this.value = value;
            return this;
        }

        public ThirdPartyImmutableWithPrimitive doTheBuild() {
            return new ThirdPartyImmutableWithPrimitive(value);
        }

        public static Builder builderFromValue(ThirdPartyImmutableWithPrimitive immutable) {
            return new Builder().setValue(immutable.getValue());
        }
    }
}
