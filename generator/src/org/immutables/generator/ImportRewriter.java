package org.immutables.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import org.immutables.extgenerator.GeneratedImportsModifier;
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

  static final ImmutableList<GeneratedImportsModifier> importsModifiers =
      ImmutableList.copyOf(ServiceLoader.load(
          GeneratedImportsModifier.class,
          ImportRewriter.class.getClassLoader()));

  private final int len;
  private int at;

  private String thisPackage;
  private String thisPackagePrefix;

  private int beforeImportPosition;
  private int afterPackagePosition;

  private final List<Import> imports = new ArrayList<>();
  private final List<Declaration> declarations = new ArrayList<>();
  private final List<MaybeQualified> occurrences = new ArrayList<>();
  private final Map<String, String> usages = new HashMap<>();
  private final Set<String> newImports = new TreeSet<>();
  private final StringBuilder segmentBuffer = new StringBuilder();

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
    return rewriter.replace();
  }

  private static abstract class Occurrence {
    int at;
    int len;

    void range(int at, int len) {
      this.at = at;
      this.len = len;
    }
  }

  private static final class Import extends Occurrence {
    final LinkedList<String> packageSegments = new LinkedList<>();
    final LinkedList<String> classSegments = new LinkedList<>();
    @Nullable String more;
    boolean isStatic;
    boolean isStar;

    @Override
    public String toString() {
      return (isStatic ? "static " : "") + asKey()
          + (more != null ? "." + more : "") + (isStar ? ".*" : "");
    }

    String asKey() {
      return (packageSegments.isEmpty()
          ? "" : (String.join(".", packageSegments) + (classSegments.isEmpty() ? "" : ".")))
          + String.join(".", classSegments);
    }

    String local() {
      return classSegments.getLast();
    }
  }

  private static final class Declaration extends Occurrence {
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
  private static final class MaybeQualified extends Occurrence {
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

  private static final class AnnotationUse {
    final MaybeQualified name = new MaybeQualified();
    final StringBuilder attributes = new StringBuilder();

    @Override
    public String toString() {
      return "@" + name + attributes;
    }
  }

  private String replace() {
    int sourceAt = 0;

    StringBuilder result = new StringBuilder(len); // the size should be roughly the same

    // Ad-hoc for omitting strange single newline in the beginning,
    // maybe coming from templates, but it's not intended to
    if (source.charAt(sourceAt) == '\n') {
      sourceAt++;
    }

    // these blocks were kept inline to better see overall work done
    // and preserve context/state of result content and sourceAt pointer

    // copy all generated imports preserving their sorted ordering
    // this new set will maintain insertion order
    Set<String> rewrittenImports = new LinkedHashSet<>(newImports);

    // append useful (non-skipped) imports to the end of our rewrittenImports set
    for (Import imp : imports) {
      if (imp.isStatic
          || imp.isStar
          || imp.classSegments.size() != 1
          || !imp.packageSegments.equals(JAVA_LANG)) {

        rewrittenImports.add(imp.toString());
      }
    }

    // Run import modifiers if present on the classpath
    for (GeneratedImportsModifier modifier : importsModifiers) {
      modifier.modify(thisPackage, rewrittenImports);
    }

    // Insert rewritten imports
    if (!rewrittenImports.isEmpty()) {
      int insertAt = Math.max(beforeImportPosition, afterPackagePosition);
      // append all header and package declaration before insert position
      result.append(source, sourceAt, insertAt);
      sourceAt = insertAt;

      for (String n : rewrittenImports) {
        result.append("import ").append(n).append(";\n");
      }

      // guess I know why we don't have a newline after inserted imports,
      // but it's too cumbersome to solve "properly", better just insert newline here
      // this might cause a blank line between generated and preexisting imports,
      // however this is fine most of the time.
      result.append("\n");
    }

    // if we have any imports originally, regardless if we had new imports already written
    // we have to check that we've writing the head before imports, and then we
    // set the source pointer to the end of imports;
    if (!imports.isEmpty()) {
      Import first = imports.get(0);
      Import last = imports.get(imports.size() - 1);

      result.append(source, sourceAt, first.at);
      sourceAt = last.at + last.len;
    }

    // Replace rewritten occurrences
    for (MaybeQualified q : occurrences) {
      if (q.rewritten) {
        // append what was before rewritten occurrence
        result.append(source, sourceAt, q.at);
        sourceAt = q.at + q.len;
        printMaybeQualified(q, result);
      }
    }
    // append the tail remaining since last replace
    result.append(source, sourceAt, source.length());

    return result.toString();
  }

  private void rewrite() {
    // first process occurrences
    // mark unknown usages (same package or inherited)
    occurrences.forEach(this::recordUnknownUsages);
    occurrences.forEach(this::maybeRewrite);
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
    mergeNonConflictingMarkerTypeuseAnnotation(q);

    maybeRewriteQualifiedName(q);

    for (AnnotationUse a : q.typeAnnotations) {
      maybeRewriteQualifiedName(a.name);
      q.rewritten |= a.name.rewritten;
    }
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

        String qualified = packagePrefix + localSegment;
        // when it exising not null, we've handled it above
        if (existing == null) {
          usages.put(localSegment, qualified);

          if (!packagePrefix.equals("java.lang.") && !packagePrefix.equals(thisPackagePrefix)) {
            newImports.add(qualified);
          }

          q.rewritten = true;
          // removing package segments, will rely on import
          q.packageSegments.clear();
        } else if (packagePrefix.equals(thisPackagePrefix)) {
          if (existing == USE_DECLARED) {
            // declared here, in this very package
            q.rewritten = true;
            q.packageSegments.clear();
          } else if (existing == USE_UNKNOWN || existing.equals(qualified)) {
            if (existing == USE_UNKNOWN) {
              // assert this qualified name
              usages.put(localSegment, qualified);
            }
            q.rewritten = true;
            q.packageSegments.clear();
            // no import necessary, same package - no conflicts
          }
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
          consumeStringLiteral();
          continue;
        case '\'':
          consumeCharLiteral();
          continue;
        case '/':
          if (processTypeUseMarker()) continue;
          if (consumeEndOfLineComment() || consumeBlockComment()) continue;
          break;
        default:
          if (isWhitespace(c) && consumeWhitespace()) continue;

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
      at++;
    }
  }

  private boolean processTypeUseMarker() {
    int begin = at;

    ok:
    {
      if (consumeKeyword('/', '*', '!', 't', 'y', 'p', 'e', 'u', 's', 'e')) {

        MaybeQualified qualified = new MaybeQualified();

        while (source.charAt(at) == '@') {
          at++;
          consumeWhitespace();
          AnnotationUse ann = new AnnotationUse();
          qualified.markerTypeAnnotations.add(ann);

          if (consumeQualifiedName(ann.name.packageSegments::add, ann.name.classSegments::add)) {
            if (source.charAt(at) == '(') {
              if (!consumeToMatchingClosingParen(ann.attributes)) break ok;
              consumeWhitespace();
            } // else seems like not a problem
          } else break ok;
        }

        advanceUntilCommentEnd();

        if (qualified.markerTypeAnnotations.isEmpty()) break ok;

        consumeWhitespace();

        if (consumeMaybeAnnotatedQualifiedName(qualified)) {
          qualified.range(begin, qualified.at + qualified.len - begin);
          addMaybeQualified(qualified);
          //into.append(source, begin, at);
          return true;
        }
      }
    }
    at = begin;
    return false;
  }

  private boolean processPackage() {
    int begin = at;
    if (consumeKeyword('p', 'a', 'c', 'k', 'a', 'g', 'e')) {
      List<String> packageSegments = new ArrayList<>();
      consumeQualifiedName(packageSegments::add, a -> {});
      thisPackage = String.join(".", packageSegments);
      thisPackagePrefix = thisPackage + ".";
      consume(';');
      consumeWhitespace();
      afterPackagePosition = at;
      return true;
    }
    return false;
  }

  private boolean consume(char c) {
    if (source.charAt(at) == c) {
      at++;
      return true;
    }
    return false;
  }

  private boolean consumeQualifiedName(
      Consumer<String> packageSegments,
      Consumer<String> classSegments) {

    char c = source.charAt(at);
    if (!isJavaIdentifierStart(c)) return false;

    int beforeDot = at;
    boolean endsWithDot = false;

    while (at < len && isJavaIdentifierStart(c = source.charAt(at)) && isLowerCase(c)) {
      segmentBuffer.setLength(0);
      consumeIdentifierSegment(segmentBuffer);
      packageSegments.accept(segmentBuffer.toString());

      endsWithDot = false;
      beforeDot = at;

      consumeWhitespace();
      if (consume('.')) {
        endsWithDot = true;
        consumeWhitespace();
      } else break;
    }

    while (at < len && isJavaIdentifierStart(c = source.charAt(at)) && isUpperCase(c)) {
      segmentBuffer.setLength(0);
      consumeIdentifierSegment(segmentBuffer);
      classSegments.accept(segmentBuffer.toString());

      endsWithDot = false;
      beforeDot = at;

      consumeWhitespace();
      if (consume('.')) {
        endsWithDot = true;
        consumeWhitespace();
      } else break;
    }

    if (endsWithDot) {
      at = beforeDot;
      consumeWhitespace();
    }

    return true;
  }

  private boolean consumeMaybeAnnotatedQualifiedName(MaybeQualified qualified) {
    int begin = at;
    char c = source.charAt(at);
    if (!isJavaIdentifierStart(c)) return false;

    ok:
    {
      int beforeWhitespace = at;

      boolean qualifyingType = false;
      while (at < len && isJavaIdentifierStart(c = source.charAt(at))) {
        // once we're getting qualified classes we're unlikely to switch back
        // to packages, but this is syntactic convention only (Uppercase or lowercase)
        boolean isTypeCase = isUpperCase(c);
        qualifyingType |= isTypeCase;

        segmentBuffer.setLength(0);
        consumeIdentifierSegment(segmentBuffer);

        (qualifyingType ? qualified.classSegments : qualified.packageSegments)
            .add(segmentBuffer.toString());

        // maybe break if keyword (need dictionary)

        beforeWhitespace = at;

        consumeWhitespace();
        if (consume('.')) {
          consumeWhitespace();
          boolean hadTypeUseAnnotations = false;
          while (source.charAt(at) == '@') {
            at++;
            hadTypeUseAnnotations = true;
            consumeWhitespace();
            AnnotationUse ann = new AnnotationUse();
            qualified.typeAnnotations.add(ann);

            if (consumeQualifiedName(ann.name.packageSegments::add, ann.name.classSegments::add)) {
              if (source.charAt(at) == '(') {
                if (!consumeToMatchingClosingParen(ann.attributes)) break ok;
                beforeWhitespace = at;
                consumeWhitespace();
              } // else seems like not a problem
            } else break ok;
          }
          if (hadTypeUseAnnotations) {
            segmentBuffer.setLength(0);
            if (isJavaIdentifierStart(c = source.charAt(at))
                && isUpperCase(c)
                && consumeIdentifierSegment(segmentBuffer)) {
              beforeWhitespace = at;
              qualified.classSegments.add(segmentBuffer.toString());
              qualified.range(begin, beforeWhitespace - begin); // range before consume whitespace
              //into.append(source, begin, at);
              consumeWhitespace();
              return true;
            } else break ok;
          }
          // doing some lookahead to see if after type comes be field or method reference
          // we don't consider whitespace here, so it is relying on "normal" formatting/style
          if (isTypeCase && at < len
              && ((isJavaIdentifierStart(c = source.charAt(at)) && isLowerCase(c))
              || c == '<'/* this is for Collections.<String>emptySet() */)) {
            qualified.range(begin, beforeWhitespace - begin); // range before consume whitespace
            //into.append(source, begin, at);
            consumeWhitespace();
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
      //into.append(source, begin, at);
      consumeWhitespace();
      return true;
    }
    // bailing out, something wrong
    at = begin;
    return false;
  }

  private boolean consumeToMatchingClosingParen(StringBuilder attributes) {
    int begin = at;
    assert source.charAt(at) == '(';
    at++;
    int open = 1;
    while (at < len) {
      char c = source.charAt(at);
      switch (c) {
        case '(':
          open++;
          break;
        case ')':
          if (--open == 0) {
            at++;
            attributes.append(source, begin, at);
            return true;
          }
          break;
        case '\'':
          consumeCharLiteral();
          continue; // do not advance
        case '"':
          consumeStringLiteral();
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

    if (consumeKeyword('i', 'm', 'p', 'o', 'r', 't')) {
      if (beforeImportPosition == 0) {
        beforeImportPosition = begin;
      }
      Import imp = new Import();
      imp.isStatic = consumeKeyword('s', 't', 'a', 't', 'i', 'c');

      consumeQualifiedName(imp.packageSegments::add, imp.classSegments::add);

      if (consume(';')) {
        consumeWhitespace();
        // if we have semicolon here, we don't have any lowercase field or method imports
        // uppercase constants we can still add as import and actually ignore

        imp.range(begin, at - begin);

        addImport(imp);
      } else if (consume('.')) {
        consumeWhitespace();
        if (consume('*')) {
          consumeWhitespace();
          if (consume(';')) {
            imp.isStar = true;
            imp.range(begin, at - begin);

            addImport(imp);
          }
        } else {
          int moreAt = at;
          consumeUntilOnSameLine(';');
          // the 'more' part excludes closing ';',
          // but the range includes it, hence `at - 1` for 'more',
          // and `at - begin` for the range length
          imp.more = source.subSequence(moreAt, at - 1).toString();
          imp.range(begin, at - begin);

          addImport(imp);
        }
      } else if (consume('*')) {
        // after recent changes in how  consumeQualifiedName works,
        // this case will not be called ever, because the star will
        // go after the dot
        consumeWhitespace();
        if (consume(';')) {
          imp.isStar = true;
          imp.range(begin, at - begin);

          addImport(imp);
        }
      } else {
        // just in case we have something else, not handled in the cases before,
        // not sure we would handle block comments here,
        // but at least line comments could work
        int moreAt = at;
        consumeUntilOnSameLine(';');
        imp.more = source.subSequence(moreAt, at - 1).toString();
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

  private void consumeUntilOnSameLine(char c1) {
    char c;
    while ((c = source.charAt(at++)) != c1 && c != '\n') ;
  }

  private void addImport(Import imp) {
    imports.add(imp);
    // we can use condition !imp.isStar, but we use more general check
    if (!imp.classSegments.isEmpty()) {
      usages.put(imp.local(), imp.asKey());
    }
  }

  private boolean processRecord() {
    if (consumeKeyword('r', 'e', 'c', 'o', 'r', 'd')) {
      consumeDeclared();
      return true;
    }
    return false;
  }

  private boolean processInterface() {
    if (consumeKeyword('i', 'n', 't', 'e', 'r', 'f', 'a', 'c', 'e')) {
      consumeDeclared();
      return true;
    }
    return false;
  }

  private boolean processEnum() {
    if (consumeKeyword('e', 'n', 'u', 'm')) {
      consumeDeclared();
      return true;
    }
    return false;
  }

  private boolean processClass() {
    if (consumeKeyword('c', 'l', 'a', 's', 's')) {
      consumeDeclared();
      return true;
    }
    return false;
  }

  private void consumeDeclared() {
    int begin = at;
    Declaration declaration = new Declaration();
    consumeIdentifierSegment(declaration.name);
    //into.append(declaration.name);
    declaration.range(begin, at - begin);
    addDeclared(declaration);
  }

  private void addDeclared(Declaration declared) {
    declarations.add(declared);
    usages.put(declared.name.toString(), USE_DECLARED);
  }

  private boolean processIdentifier() {
    char c = source.charAt(at);
    int begin = at;
    if (isJavaIdentifierStart(c)) {
      MaybeQualified qualified = new MaybeQualified();
      if (consumeMaybeAnnotatedQualifiedName(qualified)) {
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
  }

  private void consumeStringLiteral() {
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
  }

  private void consumeCharLiteral() {
    assert source.charAt(at) == '\'';
    int begin = at;
    at++;
    while (at < len) {
      if (source.charAt(at) == '\'' && source.charAt(at - 1) != '\\') {
        at++;
        break;
      }
      at++;
    }
  }

  private boolean consumeWhitespace() {
    if (isWhitespace(source.charAt(at))) {
      int begin = at;
      while (at < len && isWhitespace(source.charAt(at))) {
        at++;
      }
      return true;
    }
    return false;
  }

  private boolean consumeBlockComment() {
    if (matches('/', '*')) {
      int begin = at;
      at += 2;
      advanceUntilCommentEnd();
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

  private boolean consumeEndOfLineComment() {
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
      return true;
    }
    return false;
  }

  private boolean matches(char c0, char c1) {
    return at + 2 < len
        && source.charAt(at) == c0
        && source.charAt(at + 1) == c1;
  }

  // All keywords we use must end with whitespace
  // so these are not just any keyword
  private boolean consumeKeyword(char... chars) {
    int begin = at;
    if (matches(chars)
        && at + chars.length < len
        && isWhitespace(source.charAt(at + chars.length))) {
      at += chars.length;
      return consumeWhitespace();
    }
    return false;
  }

  private boolean matches(char... chars) {
    if (at + chars.length >= len) return false;

    for (int i = 0; i < chars.length; i++) {
      if (source.charAt(at + i) != chars[i]) {
        return false;
      }
    }

    return true;
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
        "import static java.util.Map.*;",
        "import static java.util.Map.entry;",
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
        "  public /*!typeuse @hoppy.Jully */ java.lang.@hoppy.Jully String ask;",
        "  public /*!typeuse @Nully */ p1.p3.Nop nock(int a, String b) { return null; }",
        "  public /*!typeuse @stl.Nully @Other(\"ABV\")*/ Nop lock(int a, String b) {",
        "    char c = '\"';",
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

    String[] source2 = {
       "/*!typeuse @hoppy.Jully */ java.lang.@hoppy.Jully String ask;"
    };

    CharSequence result = rewrite(String.join("\n", source2));

    System.out.println(result);
  }
}
