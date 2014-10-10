package org.immutables.generator;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.Reflection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * This class was carried over fro the old implementation and should be replaced
 * with something that has same or better performance plus needs to be more accurate.
 * @deprecated should be replaced with more accurate post processor
 */
@Deprecated
final class LegacyJavaPostprocessing {
  private static final Pattern FULLY_QUALIFIED_PATTERN =
      Pattern.compile("(\\W)(([a-z0-9_]+\\.)+)([A-Z][A-Za-z0-9_]*)");

  private static final Pattern PACKAGE_DECLARATION =
      Pattern.compile("package ([a-z0-9_\\.]+)");

  private static final Joiner LINE_JOINER = Joiner.on('\n');
  private static final Splitter LINE_SPLITTER = Splitter.on('\n');

  static CharSequence rewrite(CharSequence content) {
    List<String> modifiedLines = Lists.newArrayList();
    Set<String> importStatements = Sets.newTreeSet();
    @Nullable
    String packageName = null;
    int indexOfGenImportsPlaceholder = -1;
    int indexOfPackageLine = -1;
    for (String l : LINE_SPLITTER.split(content)) {
      if (packageName == null) {
        if (l.startsWith("package ")) {
          packageName = extractPackageName(l);
          indexOfPackageLine = modifiedLines.size();
          modifiedLines.add(l);
          continue;
        }
      }
      if (l.startsWith("// Generated imports")) {
        indexOfGenImportsPlaceholder = modifiedLines.size();
        continue;
      }
      if (l.startsWith("import ")) {
        if (indexOfGenImportsPlaceholder < 0) {
          indexOfGenImportsPlaceholder = modifiedLines.size();
        }
        modifiedLines.add(l);
        continue;
      }
      Matcher matcher = FULLY_QUALIFIED_PATTERN.matcher(l);
      while (matcher.find()) {
        String importClass = matcher.group().substring(1);
        if (!Reflection.getPackageName(importClass).equals("java.lang")) {
          importStatements.add("import " + importClass + ";");
        }
      }
      modifiedLines.add(FULLY_QUALIFIED_PATTERN
          .matcher(l)
          .replaceAll("$1$4")
          .replace(",", ", ")
          .replace(",  ", ", "));
    }

    if (indexOfGenImportsPlaceholder < 0) {
      indexOfGenImportsPlaceholder = indexOfPackageLine + 1;
    }
    modifiedLines.addAll(indexOfGenImportsPlaceholder, importStatements);
    return LINE_JOINER.join(modifiedLines);
  }

  private static String extractPackageName(String l) {
    return PACKAGE_DECLARATION.matcher(l).replaceAll("$1");
  }
}
