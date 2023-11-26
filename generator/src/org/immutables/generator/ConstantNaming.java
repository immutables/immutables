package org.immutables.generator;

public class ConstantNaming extends Naming {
    final String name;

    ConstantNaming(String name) {
        this.name = name;
    }

    @Override
    public String apply(String input) {
        return name;
    }

    @Override
    public String detect(String identifier) {
        return identifier.equals(name) ? name : NOT_DETECTED;
    }

    @Override
    public boolean isIdentity() {
        return false;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Naming requireNonConstant(Preference preference) {
        switch (preference) {
            case SUFFIX:
                return new PrefixSuffixNaming("", Usage.CAPITALIZED.apply(name));
            case PREFIX:
            default:
                return new PrefixSuffixNaming(name, "");
        }
    }

    @Override
    public Naming requireJavaBeanConvention() {
        return new JavaBeanNaming(name);
    }

    @Override
    public String toString() {
        return name;
    }
}