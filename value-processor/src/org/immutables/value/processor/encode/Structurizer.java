/*
   Copyright 2016 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value.processor.encode;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;
import org.immutables.value.processor.encode.Code.Term;

// All parsing assume well-formed Java code
@Enclosing
final class Structurizer {
  private static final ImmutableSet<String> modifiers = allModifiers();
  private final PeekingIterator<Term> terms;
  private final WhitespaceEnabler whitespaces = new WhitespaceEnabler();

  Structurizer(Iterable<Term> terms) {
    this.terms = Iterators.peekingIterator(Iterators.filter(terms.iterator(), whitespaces));
  }

  private final class WhitespaceEnabler implements Predicate<Term> {
    private int count;

    void on() {
      count++;
    }

    void off() {
      if (--count < 0)
        throw new IllegalStateException("unmatched off");
    }

    @Override
    public boolean apply(Term input) {
      return count > 0 || (!input.isWhitespace() && !input.isComment());
    }
  }

  @Immutable
  abstract static class Statement {
    abstract List<Term> annotations();

    abstract List<Term> signature();

    abstract List<Term> parameters();

    abstract List<Term> expression();

    abstract List<Term> block();

    abstract List<Statement> definitions();

    @Derived
    boolean isClassOrInterface() {
      for (Term t : signature()) {
        if (t.is("class") || t.is("interface")) {
          return true;
        }
      }
      return false;
    }

    @Lazy
    List<Term> returnType() {
      return parseReturnType(signature());
    }

    @Default
    Optional<String> name() {
      if (signature().isEmpty()) {
        return Optional.absent();
      }
      Term last = Iterables.getLast(signature());
      if (last.isWordOrNumber() && !last.is("static")) {
        return Optional.of(last.toString());
      }
      return Optional.absent();
    }

    static class Builder extends ImmutableStructurizer.Statement.Builder {}
  }

  List<Statement> structurize() {
    List<Statement> result = new ArrayList<>();
    while (terms.hasNext()) {
      result.add(statement());
    }
    return result;
  }

  private Statement statement() {
    Statement.Builder builder = new Statement.Builder();
    boolean classDecl = false;
    boolean wasParameters = false;
    for (; ; ) {
      Term t = terms.peek();
      if (t.is("=")) {
        terms.next();
        expressionUpToSemicolon(builder);
        return builder.build();
      } else if (t.is("(") && !wasParameters) {
        builder.addAllParameters(collectUntilMatching(")"));
        wasParameters = true;
      } else if (t.is("{")) {
        block(builder, classDecl);
        return builder.build();
      } else if (t.is(";")) {
        terms.next();
        return builder.build();
      } else {
        if (wasParameters) {
          terms.next();// just throwing away throws information
        } else {
          if (signature(builder)) {
            classDecl = true;
            // take class name as next token after class keyword
            builder.name(Optional.of(terms.peek().toString()));
          }
        }
      }
    }
  }

  private List<Term> collectUntilMatching(String end) {
    List<Term> result = new ArrayList<>();
    doCollectMatching(result, terms.peek().toString(), end);
    return result;
  }


  private static void collectUntilMatching(Deque<Term> terms,
      List<Term> accumulator, String start, String end) {
    accumulator.add(terms.remove());
    for (; ; ) {
      Term t = terms.peek();
      if (t.is(start)) {
        collectUntilMatching(terms, accumulator, start, end);
      } else if (t.is(end)) {
        accumulator.add(terms.remove());
        return;
      } else {
        accumulator.add(terms.remove());
      }
    }
  }

  private void doCollectMatching(List<Term> accumulator, String start, String end) {
    whitespaces.on();
    try {
      accumulator.add(terms.next());
      for (; ; ) {
        Term t = terms.peek();
        if (t.is(start)) {
          doCollectMatching(accumulator, start, end);
        } else if (t.is(end)) {
          accumulator.add(terms.next());
          return;
        } else {
          accumulator.add(terms.next());
        }
      }
    } finally {
      whitespaces.off();
    }
  }

  private void expressionUpToSemicolon(Statement.Builder builder) {
    terms.peek();
    whitespaces.on();
    try {
      List<Term> result = new ArrayList<>();
      for (; ; ) {
        Term t = terms.peek();
        if (t.is("(")) {
          doCollectMatching(result, "(", ")");
        } else if (t.is("{")) {
          doCollectMatching(result, "{", "}");
        } else if (t.is("[")) {
          doCollectMatching(result, "[", "]");
        } else {
          if (t.is(";")) {
            builder.addAllExpression(result);
            return;
          }
          result.add(terms.next());
        }
      }
    } finally {
      whitespaces.off();
    }
  }

  private void block(Statement.Builder builder, boolean classDecl) {
    if (classDecl) {
      Verify.verify(terms.peek().is("{"));
      terms.next();
      while (terms.hasNext() && !terms.peek().is("}")) {
        builder.addDefinitions(statement());
      }
      Verify.verify(terms.next().is("}"));
    } else {
      builder.addAllBlock(collectUntilMatching("}"));
    }
  }

  private boolean signature(Statement.Builder builder) {
    Term t = terms.peek();
    if (t.is("@")) {
      List<Term> annotations = collectAnnotations();
      builder.addAllAnnotations(annotations);
      builder.addAllSignature(annotations);
      return false;
    } else if (t.is("<")) {
      builder.addAllSignature(collectUntilMatching(">"));
      return false;
    } else if (t.is("class") || t.is("interface")) {
      builder.addSignature(terms.next());
      return true;
    } else {
      builder.addSignature(terms.next());
      return false;
    }
  }

  private List<Term> collectAnnotations() {
    if (!terms.peek().is("@")) return Collections.emptyList();
    List<Term> list = new ArrayList<>();
    while (terms.peek().is("@")) {
      do {
        list.add(terms.next());
        assert terms.peek().isWordOrNumber();
        list.add(terms.next());
      } while (terms.peek().is("."));
      if (terms.peek().is("(")) {
        list.addAll(collectUntilMatching(")"));
      }
    }
    return list;
  }

  private static List<Term> collectAnnotations(Deque<Term> terms) {
    if (!terms.peek().is("@")) return Collections.emptyList();
    List<Term> list = new ArrayList<>();
    while (!terms.isEmpty() && terms.peek().is("@")) {
      do {
        list.add(terms.remove());
        assert terms.peek().isWordOrNumber();
        list.add(terms.remove());
      } while (!terms.isEmpty() && terms.peek().is("."));
      if (!terms.isEmpty() && terms.peek().is("(")) {
        collectUntilMatching(terms, list, "(", ")");
      }
    }
    return list;
  }

  private static List<Term> parseReturnType(List<Term> signature) {
    if (signature.isEmpty()) {
      return ImmutableList.of();
    }
    Deque<Term> terms = new ArrayDeque<>(signature);
    Term last = terms.removeLast();
    if (!last.isWordOrNumber() || last.is("static")) {
      return ImmutableList.of();
    }
    while (!terms.isEmpty()) {
      Term t = terms.peek();
      if (t.is("@")) {
        // throwaway front annotations
        collectAnnotations(terms);
      } else if (t.is("<")) {
        removeTillMatching(terms, "<", ">");
      } else if (modifiers.contains(t.toString())) {
        terms.remove();
      } else {
        // remove type annotations in-between type constructs
        // and whitespace or comments too (just in case)
        List<Term> clearTerms = new ArrayList<>();

        while (!terms.isEmpty()) {
          Term n = terms.peek();
          if (n.is("@")) {
            // throwaway
            collectAnnotations(terms);
            continue;
          }
          n = terms.remove();
          if (!n.isComment() && !n.isWhitespace()) {
            clearTerms.add(n);
          }
        }
        return ImmutableList.copyOf(clearTerms);
      }
    }
    return ImmutableList.of();
  }

  private static void removeTillMatching(Deque<Term> terms, String begin, String end) {
    assert terms.peek().is(begin);
    terms.remove();
    for (; ; ) {
      if (terms.peek().is(begin)) {
        removeTillMatching(terms, begin, end);
      } else if (terms.remove().is(end)) {
        return;
      }
    }
  }

  private static ImmutableSet<String> allModifiers() {
    ImmutableSet.Builder<String> b = ImmutableSet.builder();
    for (Modifier m : Modifier.values()) {
      b.add(m.toString());
    }
    return b.build();
  }
}
