package org.immutables.generator;

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;

public class PrefixSuffixNaming extends Naming {
    final String prefix;
    final String suffix;
    final int lengthsOfPrefixAndSuffix;

    PrefixSuffixNaming(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.lengthsOfPrefixAndSuffix = suffix.length() + prefix.length();
        Preconditions.checkArgument(lengthsOfPrefixAndSuffix > 0);
    }

    @Override
    public String apply(String input) {
        Usage resultFormat = prefix.isEmpty()
                ? Usage.INDIFFERENT
                : Usage.CAPITALIZED;

        return prefix + resultFormat.apply(input) + suffix;
    }

    @Override
    public String detect(String identifier) {
        if (identifier.length() <= lengthsOfPrefixAndSuffix) {
            return NOT_DETECTED;
        }

        boolean prefixMatches = prefix.isEmpty() ||
                (identifier.startsWith(prefix) && Ascii.isUpperCase(identifier.charAt(prefix.length())));

        boolean suffixMatches = suffix.isEmpty() || identifier.endsWith(suffix);

        if (prefixMatches && suffixMatches) {
            String detected = identifier.substring(prefix.length(), identifier.length() - suffix.length());
            return prefix.isEmpty()
                    ? detected
                    : CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, detected);
        }

        return NOT_DETECTED;
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
        return this;
    }

    @Override
    public Naming requireJavaBeanConvention() {
        return new JavaBeanNaming(prefix);
    }

    @Override
    public String toString() {
        return prefix + NAME_PLACEHOLDER + suffix;
    }
}
