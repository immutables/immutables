package org.immutables.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.isWhitespace;

// assumptions:
// - import optimization should be (mostly) optional
//   - if fail to recognize certain construct, skip - it
//   - line breaks can terminate sequence making it unprocessable
// - valid java syntax
// - UpperCamelCase convention for classes and interfaces
// - escapes are not processed (?) (except for in strings / chars to properly skip)

// Unqualified occurrences can come from
// - Explicit import
// - Star import
// - Same package
// - Inherited from supertype

public class ImportRewriter {
  private final CharSequence source;
  private final StringBuilder result = new StringBuilder();

  private final int len;
  private int at;

  private String thisPackage;

  private int beforeImportPosition;
  private int afterPackagePosition;

  private final List<Import> imports = new ArrayList<>();
  private final List<Declaration> declarations = new ArrayList<>();
  private final List<MaybeQualified> occurrences = new ArrayList<>();
  private final Map<String, String> usages = new HashMap<>();
  private final Set<String> newImports = new TreeSet<>();

  private static final String USE_DECLARED = "!DECLARED!";
  private static final String USE_UNKNOWN = "!UNKNOWN!";
  private static final List<String> JAVA_LANG = Arrays.asList("java", "lang");

  private ImportRewriter(CharSequence source) {
    this.source = source;
    this.len = source.length();
  }

  public static CharSequence rewrite(CharSequence content) {
    ImportRewriter rewriter = new ImportRewriter(content);
    rewriter.process();
    rewriter.rewrite();
    return rewriter.result;
  }

  abstract class Occurrence {
    int at;
    int len;

    void range(int at, int len) {
      this.at = at;
      this.len = len;
    }
  }

  class Import extends Occurrence {
    final LinkedList<String> packageSegments = new LinkedList<>();
    final LinkedList<String> classSegments = new LinkedList<>();
    final StringBuilder name = new StringBuilder();
    boolean isStatic;

    @Override
    public String toString() {
      return (packageSegments.isEmpty() ? "" : ("[" + String.join(".", packageSegments) + "]"))
          + String.join(".", classSegments);
    }

    String asKey() {
      return (packageSegments.isEmpty() ? "" : (String.join(".", packageSegments) + '.'))
          + String.join(".", classSegments);
    }

    String local() {
      return classSegments.getLast();
    }
  }

  class Declaration extends Occurrence {
    final StringBuilder name = new StringBuilder();

    @Override
    public String toString() {
      return name.toString();
    }
  }

  // @typeuse comment marker annotation can be merged-in
  // if there's no same-named annotation already,
  // we disregard (existence) repeatable annotations
  // or equality with regard to attributes
  class MaybeQualified extends Occurrence {
    final LinkedList<String> packageSegments = new LinkedList<>();
    final LinkedList<String> classSegments = new LinkedList<>();
    final StringBuilder name = new StringBuilder();
    final List<AnnotationUse> markerTypeAnnotations = new ArrayList<>();
    final List<AnnotationUse> typeAnnotations = new ArrayList<>();

    private @Nullable String importKey;
    boolean rewritten;

    @Override
    public String toString() {
      return (packageSegments.isEmpty() ? "" : ("[" + String.join(".", packageSegments) + "]"))
          + String.join(".", classSegments)
          + (typeAnnotations.isEmpty() ? ""
          : (" :: " + typeAnnotations.stream()
              .map(Objects::toString)
              .collect(Collectors.joining(", "))))
          + (markerTypeAnnotations.isEmpty() ? ""
          : (" !: " + markerTypeAnnotations.stream()
              .map(Objects::toString)
              .collect(Collectors.joining(", "))));
    }
  }

  class AnnotationUse {
    final MaybeQualified name = new MaybeQualified();
    final StringBuilder attributes = new StringBuilder();

    @Override
    public String toString() {
      return "@" + name + attributes;
    }
  }

  private void rewrite() {
    // first process occurrences from top to bottom
    // mark unknown usages (same package or inherited)
    occurrences.forEach(this::recordUnknownUsages);
    occurrences.forEach(this::maybeRewrite);
    // then replace from bottom to top
    replaceRewrittenBackwards();

    removeJavaLangImports();
    insertNewImports();
  }

  private void removeJavaLangImports() {
    Collections.reverse(imports);
    for (Import imp : imports) {
      if (!imp.isStatic && imp.classSegments.size() == 1 && imp.packageSegments.equals(JAVA_LANG)) {
        result.delete(imp.at, imp.at + imp.len);
      }
    }
  }

  private void insertNewImports() {
    StringBuilder buffer = new StringBuilder();
    for (String n : newImports) {
      buffer.append("import ").append(n).append(";\n");
    }
    int insertAt = Math.max(beforeImportPosition, afterPackagePosition);
    result.insert(insertAt, buffer);
  }

  private void recordUnknownUsages(MaybeQualified q) {
    for (AnnotationUse a : q.markerTypeAnnotations) recordUnknownUsages(a.name);
    for (AnnotationUse a : q.typeAnnotations) recordUnknownUsages(a.name);

    if (q.packageSegments.isEmpty()) {
      String local = q.classSegments.getFirst();
      usages.putIfAbsent(local, USE_UNKNOWN);
    }
  }

  private void maybeRewrite(MaybeQualified q) {
    //try {
    mergeNonConflictingMarkerTypeuseAnnotation(q);

    maybeRewriteQualifiedName(q);

    for (AnnotationUse a : q.typeAnnotations) {
      maybeRewriteQualifiedName(a.name);
      q.rewritten |= a.name.rewritten;
    }
    // } catch (Exception e) {
    //   throw new RuntimeException(q.toString(), e);
    // }
  }

  private void maybeRewriteQualifiedName(MaybeQualified q) {
    if (q.packageSegments.isEmpty()) return;
    String packagePrefix = String.join(".", q.packageSegments) + ".";

    LinkedList<String> topClassSegments = new LinkedList<>(q.classSegments);
    String localSegment;
    while (true) {
      localSegment = topClassSegments.removeLast();
      @Nullable String existing = usages.get(localSegment);

      if (existing != null && existing != USE_DECLARED && existing != USE_UNKNOWN) {
        String qualified = packagePrefix;
        if (!topClassSegments.isEmpty()) {
          qualified += String.join(".", topClassSegments) + ".";
        }
        qualified += localSegment;

        if (existing.equals(qualified)) {
          // can use existing import
          q.rewritten = true;

          assert topClassSegments.size() <= q.classSegments.size();
          while (!topClassSegments.isEmpty() && q.classSegments.getFirst().equals(topClassSegments.getFirst())) {
            q.classSegments.removeFirst();
            topClassSegments.removeFirst();
          }

          q.packageSegments.clear();
          return;
        }
      }

      if (topClassSegments.isEmpty()) {
        // if local segment is first segment now
        existing = usages.get(localSegment);
        // when it exising not null, we've handled it above
        if (existing == null) {
          String qualified = packagePrefix + localSegment;
          usages.put(localSegment, qualified);

          if (!packagePrefix.equals("java.lang.")) {
            newImports.add(qualified);
          }

          q.rewritten = true;
          // removing package segments, will rely on import
          q.packageSegments.clear();
        }
        return;
      }
    }
  }

  private void mergeNonConflictingMarkerTypeuseAnnotation(MaybeQualified q) {
    if (!q.markerTypeAnnotations.isEmpty()) {
      for (AnnotationUse markerAnnotation : q.markerTypeAnnotations) {
        if (!potentialConflictTypeName(markerAnnotation, q.typeAnnotations)) {
          q.typeAnnotations.add(markerAnnotation);
          q.rewritten = true;
        }
      }
    }
  }

  private boolean potentialConflictTypeName(AnnotationUse annotation, List<AnnotationUse> annotations) {
    next:
    for (AnnotationUse a : annotations) {
      // this is the best effort attempt to avoid conflicts,
      // it requires certain discipline on the design of annotation
      // and how they are spelled in qualified form
      if (a.name.classSegments.getLast().equals(annotation.name.classSegments.getLast())) {
        if (a.name.classSegments.size() == annotation.name.classSegments.size()
            && a.name.classSegments.size() > 1
            && !a.name.classSegments.equals(annotation.name.classSegments)) {
          continue next;
        }
        if (a.name.packageSegments.isEmpty()
            || annotation.name.packageSegments.isEmpty()
            || (a.name.packageSegments.equals(annotation.name.packageSegments)
            && a.name.classSegments.equals(annotation.name.classSegments))) {
          return true;
        }
      }
    }
    return false;
  }

  private void replaceRewrittenBackwards() {
    Collections.reverse(occurrences);
    StringBuilder buffer = new StringBuilder();
    for (MaybeQualified q : occurrences) {
      if (q.rewritten) {
        buffer.setLength(0);
        printMaybeQualified(q, buffer);
        String ddd = buffer.toString();
        result.replace(q.at, q.at + q.len, ddd);
      }
    }
  }

  private void printMaybeQualified(MaybeQualified q, StringBuilder buffer) {
    if (q.packageSegments.isEmpty() && q.classSegments.size() == 1 && !q.typeAnnotations.isEmpty()) {
      // Type use annotations should be printed outside of local type segment
      for (AnnotationUse a : q.typeAnnotations) {
        buffer.append('@');
        printMaybeQualified(a.name, buffer);
        buffer.append(' ');
      }

      buffer.append(q.classSegments.getFirst()); // first and only
    } else {
      for (String p : q.packageSegments) {
        buffer.append(p).append('.');
      }
      // we can mutate it, we won't need it after
      String lastType = q.classSegments.removeLast();

      for (String c : q.classSegments) {
        buffer.append(c).append('.');
      }

      for (AnnotationUse a : q.typeAnnotations) {
        buffer.append('@');
        printMaybeQualified(a.name, buffer);
        buffer.append(' ');
      }

      buffer.append(lastType);
    }
  }

  private void process() {
    while (at < len) {
      char c = source.charAt(at);
      switch (c) {
        case '\"':
          consumeStringLiteral(result);
          continue;
        case '\'':
          consumeCharLiteral(result);
          continue;
        case '/':
          if (processTypeUseMarker()) continue;
          if (consumeEndOfLineComment(result) || consumeBlockComment(result)) continue;
          break;
        default:
          if (isWhitespace(c) && consumeWhitespace(result)) continue;

          char previous = at > 0 ? source.charAt(at - 1) : 0;
          if (isJavaIdentifierStart(c) && (!isJavaIdentifierPart(previous) || previous == 0)) {
            switch (c) {
              case 'p':
                if (processPackage()) continue;
                break;
              case 'i':
                if (processImport()) continue;
                if (processInterface()) continue;
                break;
              case 'c':
                if (processClass()) continue;
                break;
              case 'r':
                if (processRecord()) continue;
                break;
              case 'e':
                if (processEnum()) continue;
                break;
              default: // nothing
            }

            if (processIdentifier()) continue;
          }
          // default handling is just at++ and goto next char
      }
      result.append(c);
      at++;
    }
  }

  private boolean processTypeUseMarker() {
    int begin = at;
    StringBuilder throwaway = new StringBuilder();
    ok:
    {
      if (consumeKeyword(throwaway, '/', '*', '!', 't', 'y', 'p', 'e', 'u', 's', 'e')) {

        MaybeQualified qualified = new MaybeQualified();

        while (source.charAt(at) == '@') {
          at++;
          throwaway.append('@');
          consumeWhitespace(throwaway);
          AnnotationUse ann = new AnnotationUse();
          qualified.markerTypeAnnotations.add(ann);

          if (consumeQualifiedName(throwaway, ann.name.packageSegments::add, ann.name.classSegments::add)) {
            if (source.charAt(at) == '(') {
              if (!consumeToMatchingClosingParen(ann.attributes)) break ok;
              consumeWhitespace(throwaway);
            } // else seems like not a problem
          } else break ok;
        }

        advanceUntilCommentEnd();

        if (qualified.markerTypeAnnotations.isEmpty()) break ok;

        consumeWhitespace(throwaway);

        if (consumeMaybeAnnotatedQualifiedName(throwaway, qualified)) {
          qualified.range(begin, qualified.at + qualified.len - begin);
          addMaybeQualified(qualified);
          result.append(source, begin, at);
          return true;
        }
      }
    }
    at = begin;
    return false;
  }

  private boolean processPackage() {
    int begin = at;
    if (consumeKeyword(result, 'p', 'a', 'c', 'k', 'a', 'g', 'e')) {
      List<String> packageSegments = new ArrayList<>();
      consumeQualifiedName(result, packageSegments::add, a -> {});
      thisPackage = String.join(".", packageSegments);
      //System.err.println("PACKAGE " + thisPackage);
      consumeWhitespace(result);
      consume(result, ';');
      consumeWhitespace(result);
      afterPackagePosition = at;
      return true;
    }
    return false;
  }

  private boolean consume(StringBuilder into, char c) {
    if (source.charAt(at) == c) {
      into.append(c);
      at++;
      return true;
    }
    return false;
  }

  private boolean consumeQualifiedName(
      StringBuilder into,
      Consumer<String> packageSegments,
      Consumer<String> classSegments) {

    char c = source.charAt(at);
    if (!isJavaIdentifierStart(c)) return false;

    while (at < len && isJavaIdentifierStart(c = source.charAt(at)) && isLowerCase(c)) {
      StringBuilder segment = new StringBuilder();
      consumeIdentifierSegment(segment);
      into.append(segment);
      packageSegments.accept(segment.toString());

      consumeWhitespace(into);
      if (consume(into, '.')) {
        consumeWhitespace(into);
      } else break;
    }

    while (at < len && isJavaIdentifierStart(c = source.charAt(at)) && isUpperCase(c)) {
      StringBuilder segment = new StringBuilder();
      consumeIdentifierSegment(segment);
      into.append(segment);
      classSegments.accept(segment.toString());

      consumeWhitespace(into);
      if (consume(into, '.')) {
        consumeWhitespace(into);
      } else break;
    }

    consumeWhitespace(into);
    return true;
  }

  private boolean consumeMaybeAnnotatedQualifiedName(StringBuilder into, MaybeQualified qualified) {
    StringBuilder throwaway = new StringBuilder();

    int begin = at;
    char c = source.charAt(at);
    if (!isJavaIdentifierStart(c)) return false;

    ok:
    {
      int beforeWhitespace = at;
      StringBuilder segment = new StringBuilder();
      boolean qualifyingType = false;
      while (at < len && isJavaIdentifierStart(c = source.charAt(at))) {
        // once we're getting qualified classes we're unlikely to switch back
        // to packages, but this is syntactic convention only (Uppercase or lowercase)
        boolean isTypeCase = isUpperCase(c);
        qualifyingType |= isTypeCase;

        segment.setLength(0);
        consumeIdentifierSegment(segment);

        (qualifyingType ? qualified.classSegments : qualified.packageSegments)
            .add(segment.toString());

        // maybe break if keyword (need dictionary)

        beforeWhitespace = at;

        consumeWhitespace(throwaway);
        if (consume(throwaway, '.')) {
          consumeWhitespace(throwaway);
          boolean hadTypeUseAnnotations = false;
          while (source.charAt(at) == '@') {
            at++;
            hadTypeUseAnnotations = true;
            consumeWhitespace(throwaway);
            AnnotationUse ann = new AnnotationUse();
            qualified.typeAnnotations.add(ann);

            if (consumeQualifiedName(throwaway, ann.name.packageSegments::add, ann.name.classSegments::add)) {
              if (source.charAt(at) == '(') {
                if (!consumeToMatchingClosingParen(ann.attributes)) break ok;
                beforeWhitespace = at;
                consumeWhitespace(throwaway);
              } // else seems like not a problem
            } else break ok;
          }
          if (hadTypeUseAnnotations) {
            segment.setLength(0);
            if (isJavaIdentifierStart(c = source.charAt(at))
                && isUpperCase(c)
                && consumeIdentifierSegment(segment)) {
              beforeWhitespace = at;
              qualified.classSegments.add(segment.toString());
              qualified.range(begin, beforeWhitespace - begin); // range before consume whitespace
              into.append(source, begin, at);
              consumeWhitespace(into);
              return true;
            } else break ok;
          }
          // doing some lookahead to see if after type comes be field or method reference
          // we don't consider whitespace here, so it is relying on "normal" formatting/style
          if (isTypeCase && at < len
              && ((isJavaIdentifierStart(c = source.charAt(at)) && isLowerCase(c))
              || c == '<'/* this is for Collections.<String>emptySet() */)) {
            qualified.range(begin, beforeWhitespace - begin); // range before consume whitespace
            into.append(source, begin, at);
            consumeWhitespace(into);
            return true;
          }
        } else {
          // this will cover method references ok java.util.Collections::sort
          if (isTypeCase) break;
          break ok;
        }
      }
      if (qualified.classSegments.isEmpty()) break ok;

      qualified.range(begin, beforeWhitespace - begin); // range before consume whitespace
      into.append(source, begin, at);
      consumeWhitespace(into);
      return true;
    }
    // bailing out, something wrong
    at = begin;
    return false;
  }

  private boolean consumeToMatchingClosingParen(StringBuilder attributes) {
    int begin = at;
    assert source.charAt(at) == '(';
    attributes.append('(');
    at++;
    int open = 1;
    while (at < len) {
      char c = source.charAt(at);
      switch (c) {
        case '(':
          attributes.append(c);
          open++;
          break;
        case ')':
          attributes.append(c);
          if (--open == 0) {
            at++;
            return true;
          }
          break;
        case '\'':
          consumeCharLiteral(attributes);
          continue; // do not advance
        case '"':
          consumeStringLiteral(attributes);
          continue; // do not advance
        default:
          attributes.append(c);
      }
      at++;
    }
    at = begin;
    return false;
  }

  private boolean consumeIdentifierSegment(StringBuilder into) {
    if (isJavaIdentifierStart(source.charAt(at))) {
      char c;
      while (isJavaIdentifierPart(c = source.charAt(at))) {
        into.append(c);
        at++;
      }
      return true;
    }
    return false;
  }

  private boolean processImport() {
    int begin = at;

    if (consumeKeyword(result, 'i', 'm', 'p', 'o', 'r', 't')) {
      if (beforeImportPosition == 0) {
        beforeImportPosition = begin;
      }
      Import imp = new Import();
      imp.isStatic = consumeKeyword(result, 's', 't', 'a', 't', 'i', 'c');

      consumeQualifiedName(result, imp.packageSegments::add, imp.classSegments::add);
      if (consume(result, ';')) {
        consumeWhitespace(result);
        // if we have semicolon here, we don't have any lowercase field or method imports
        // uppercase constants we can still add as import and actually ignore

        imp.range(begin, at - begin);
        addImport(imp);
      }
      // if we've not added import, we still recognized it to some degree, so return true
      // and not backtracking to begin
      return true;
    }
    at = begin;
    return false;
  }

  private void addImport(Import imp) {
    imports.add(imp);
    usages.put(imp.local(), imp.asKey());
    //System.err.println("IMPORT " + (imp.isStatic ? "STATIC " : "") + imp);
  }

  private boolean processRecord() {
    if (consumeKeyword(result, 'r', 'e', 'c', 'o', 'r', 'd')) {
      consumeDeclared(result);
      return true;
    }
    return false;
  }

  private boolean processInterface() {
    if (consumeKeyword(result, 'i', 'n', 't', 'e', 'r', 'f', 'a', 'c', 'e')) {
      consumeDeclared(result);
      return true;
    }
    return false;
  }

  private boolean processEnum() {
    if (consumeKeyword(result, 'e', 'n', 'u', 'm')) {
      consumeDeclared(result);
      return true;
    }
    return false;
  }

  private boolean processClass() {
    if (consumeKeyword(result, 'c', 'l', 'a', 's', 's')) {
      consumeDeclared(result);
      return true;
    }
    return false;
  }

  private void consumeDeclared(StringBuilder into) {
    int begin = at;
    Declaration declaration = new Declaration();
    consumeIdentifierSegment(declaration.name);
    into.append(declaration.name);
    declaration.range(begin, at - begin);
    addDeclared(declaration);
  }

  private void addDeclared(Declaration declared) {
    declarations.add(declared);
    usages.put(declared.name.toString(), USE_DECLARED);
    //System.err.println("DECLARED " + declared);
  }

  private boolean processIdentifier() {
    char c = source.charAt(at);
    int begin = at;
    if (isJavaIdentifierStart(c)) {
      MaybeQualified qualified = new MaybeQualified();
      if (consumeMaybeAnnotatedQualifiedName(result, qualified)) {
        addMaybeQualified(qualified);
        return true;
      }
    }
    at = begin;
    return false;
  }

  private void addMaybeQualified(MaybeQualified qualified) {
    if (qualified.packageSegments.isEmpty()
        && qualified.typeAnnotations.isEmpty()
        && qualified.markerTypeAnnotations.isEmpty()) {
      // this is not interesting to handle, no package prefix to add import
      // and no type use annotations to process
      usages.putIfAbsent(qualified.classSegments.getFirst(), USE_UNKNOWN);
      return;
    }

    occurrences.add(qualified);

    //System.err.println("?QUALIFIED " + qualified);
  }

  private void consumeStringLiteral(StringBuilder result) {
    assert source.charAt(at) == '"';
    int begin = at;
    at++;
    while (at < len) {
      if (source.charAt(at) == '"' && source.charAt(at - 1) != '\\') {
        at++;
        break;
      }
      at++;
    }
    result.append(source, begin, at);
  }

  private void consumeCharLiteral(StringBuilder into) {
    assert source.charAt(at) == '\'';
    int begin = at;
    while (at < len) {
      if (source.charAt(at) == '\'' && source.charAt(at - 1) != '\\') {
        at++;
        break;
      }
      at++;
    }
    result.append(source, begin, at);
  }

  private boolean consumeWhitespace(StringBuilder into) {
    if (isWhitespace(source.charAt(at))) {
      int begin = at;
      while (at < len && isWhitespace(source.charAt(at))) {
        at++;
      }
      into.append(source, begin, at);
      return true;
    }
    return false;
  }

  private boolean consumeBlockComment(StringBuilder into) {
    if (matches('/', '*')) {
      int begin = at;
      at += 2;
      advanceUntilCommentEnd();
      into.append(source, begin, at);
      return true;
    }
    return false;
  }

  private void advanceUntilCommentEnd() {
    while (at < len) {
      if (matches('*', '/')) {
        at += 2;
        break;
      }
      at++;
    }
  }

  private boolean consumeEndOfLineComment(StringBuilder into) {
    if (matches('/', '/')) {
      int begin = at;
      at += 2;
      while (at < len) {
        if (source.charAt(at) == '\n') {
          at++;
          break;
        }
        at++;
      }
      into.append(source, begin, at);
      return true;
    }
    return false;
  }

  private boolean matches(char c0, char c1) {
    return at + 2 < len
        && source.charAt(at) == c0
        && source.charAt(at + 1) == c1;
  }

  private boolean consumeKeyword(StringBuilder into, char... chars) {
    int begin = at;
    if (matches(chars)
        && at + chars.length < len
        && isWhitespace(source.charAt(at + chars.length))) {
      at += chars.length;
      into.append(source, begin, at);
      return consumeWhitespace(into);
    }
    return false;
  }

  private boolean matches(char... chars) {
    if (at + chars.length < len) {
      for (int i = 0; i < chars.length; i++) {
        if (source.charAt(at + i) != chars[i]) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  public static void main(String[] args) {
    String[] source = {
        "package some.pack;",
        "",
        "  // line comment",
        "  /* block comment */",
        "import static java.util.Collections.emptyList;",
        "import static java.util.List.*;",
        "import java.util.*;",
        "import java.util.Set;",
        "import java.lang.Integer;",
        "import static java.lang.Long.MIN_VALUE;",
        "import static java.util.Map.Entry;",
        "import java.lang.Object; // something",
        "import java.lang.String; // another",
        "  ",
        "public record Hello() {}",
        "class Another {",
        "  private enum Or {",
        "    ABC,",
        "    GHT,",
        "  }",
        "  public final Something.@pack.Nullable @quack.Nullable Something goes() { return null; }",
        "  public final p1.p2.@Empty @els.bor.Or(value = {true, false}) Epty goes2() { return null; }",
        "  public final p1.p3.Yaz vock(int a) { return null; }",
        "  public final p1.p3.Nop lock(int a, String b) { return null; }",
        "  public /*!typeuse @Nully */ p1.p3.Nop nock(int a, String b) { return null; }",
        "  public /*!typeuse @stl.Nully @Other(\"ABV\")*/ Nop lock(int a, String b) {",
        "    int[] x = acc.some.Be.EMPTY;",
        "    java.util.List.<String>of();",
        "    java.util.Collections.emptySet();",
        "    something.to.Impo.RT;",
        "    java.lang.Long.MIN_VALUE.oops;",
        "    java.lang.Integer.MAX_VALUE.oops;",
        "    java.lang.String.format(\"%d\", 1);",
        "    return java.util.Collections::sort;",
        "  }",
        "  /*private void Another() {}*/",
        "}",
    };

    CharSequence result = rewrite(String.join("\n", source));

    System.out.println(result);
  }
}
