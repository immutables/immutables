package org.immutables.generator;

import java.util.Objects;

public class JavaBeanNaming extends Naming {
    private final String prefix;

    JavaBeanNaming(String prefix) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    @Override
    public String apply(String input) {
        return prefix + Usage.CAPITALIZED.apply(input);
    }

    @Override
    public String detect(String identifier) {
        if (!identifier.startsWith(prefix)) {
            return NOT_DETECTED;
        }

        String name = identifier.substring(prefix.length());

        if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            // leave name as is
            // URL -> URL
            return name;
        }

        return Usage.LOWERIZED.apply(name);
    }

    @Override
    public boolean isIdentity() {
        return false;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Naming requireNonConstant(Preference preference) {
        if (preference != Preference.PREFIX) {
            throw new IllegalArgumentException(String.format("Preference %s not supported by %s", preference, getClass().getSimpleName()));
        }
        return this;
    }

    @Override
    public Naming requireJavaBeanConvention() {
        return this;
    }
}
