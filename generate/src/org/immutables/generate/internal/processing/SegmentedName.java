package org.immutables.generate.internal.processing;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

final class SegmentedName {
  private static final Joiner JOINER = Joiner.on('.').skipNulls();

  final String packageName;
  final String enclosingClassName;
  final String simpleName;
  final String referenceClassName;
  final String fullyQualifedName;

  private SegmentedName(String packageName, String enclosingClassName, String simpleName) {
    this.packageName = packageName;
    this.enclosingClassName = enclosingClassName;
    this.simpleName = simpleName;
    this.referenceClassName = enclosingClassName.isEmpty()
        ? simpleName
        : JOINER.join(
            Strings.emptyToNull(enclosingClassName),
            Strings.emptyToNull(simpleName));

    this.fullyQualifedName =
        JOINER.join(
            Strings.emptyToNull(packageName),
            Strings.emptyToNull(enclosingClassName),
            Strings.emptyToNull(simpleName));
  }

  public String[] toArray() {
    return new String[] { packageName, enclosingClassName, simpleName };
  }

  @Override
  public String toString() {
    return String.format("[%s].[%s].[%s]", packageName, enclosingClassName, simpleName);
  }

  public static SegmentedName from(CharSequence name) {
    Preconditions.checkArgument(name.length() > 1);

    int lastDotAfterPackageIndex = 0;
    int startOfSimpleName = -1;

    for (int i = name.length() - 2; i >= 0; i--) {
      char c = name.charAt(i);

      if (c == '.') {
        char nextChar = name.charAt(i + 1);
        if (Character.isUpperCase(nextChar)) {
          lastDotAfterPackageIndex = i;

          if (startOfSimpleName < 0) {
            startOfSimpleName = i + 1;
          }
        }
        if (Character.isLowerCase(nextChar)) {
          break;
        }
      }
      if (i == 0) {
        lastDotAfterPackageIndex = 0;
      }
    }

    if (startOfSimpleName < 0) {
      if (Character.isUpperCase(name.charAt(0))) {
        return new SegmentedName("", "", name.toString());
      }
      return new SegmentedName(name.toString(), "", "");
    }

    return new SegmentedName(
        name.subSequence(0, lastDotAfterPackageIndex).toString(),
        (lastDotAfterPackageIndex != 0 && lastDotAfterPackageIndex + 1 < startOfSimpleName)
            ? name.subSequence(lastDotAfterPackageIndex + 1, startOfSimpleName - 1).toString() :
            (lastDotAfterPackageIndex == 0 && startOfSimpleName > 0)
                ? name.subSequence(0, startOfSimpleName - 1).toString()
                : "",
        name.subSequence(startOfSimpleName, name.length()).toString());
  }

  public static void main(String... args) {
    System.out.println(from("xx.ss.Mm"));
    System.out.println(from("Mm"));
    System.out.println(from("Mss.Mm"));
    System.out.println(from("Mss.Zdd.Mm"));
    System.out.println(from("sss.ss"));
  }
}
