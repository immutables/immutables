package org.immutables.modeling.templating;

import org.immutables.modeling.templating.Trees.DirectiveStart;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.modeling.templating.ImmutableTrees.Newline;
import org.immutables.modeling.templating.ImmutableTrees.Template;
import org.immutables.modeling.templating.ImmutableTrees.TextBlock;
import org.immutables.modeling.templating.ImmutableTrees.TextFragment;
import org.immutables.modeling.templating.ImmutableTrees.TextLine;
import org.immutables.modeling.templating.ImmutableTrees.Unit;

/**
 * Spacing trimming and redistribution should be run before balancing
 */
public final class Spacing {
  private Spacing() {}

  public static Unit trim(Unit unit) {
    unit = TRANSFORMER.transform((Void) null, unit);
    return unit;
  }

  private static final TreesTransformer<Void> TRANSFORMER = new TreesTransformer<Void>() {
    @Override
    protected Iterable<Trees.TemplatePart> transformTemplateParts(
        Void context,
        Template value,
        final List<Trees.TemplatePart> parts) {

      final List<Trees.TemplatePart> results = Lists.newArrayList();

      class Collector {
        @Nullable
        TextFragment fragment;

        void collect() {
          for (Trees.TemplatePart part : parts) {
            if (part instanceof TextBlock) {
              TextBlock block = (TextBlock) part;
              for (Trees.TextPart text : block.parts()) {
                if (text instanceof TextFragment) {
                  flushFragment();
                  fragment = (TextFragment) text;
                }
                if (text instanceof Newline) {
                  flushNewline();
                }
              }
            } else {
              flushFragment();
              results.add(part);
            }
          }

          flushFragment();
        }

        void flushFragment() {
          if (fragment != null && !fragment.value().isEmpty()) {
            results.add(TextLine.builder()
                .fragment(fragment)
                .build());
          }

          fragment = null;
        }

        void flushNewline() {
          results.add(TextLine.builder()
              .fragment(fragment != null ? fragment : TextFragment.of(""))
              .newline(true)
              .build());

          fragment = null;
        }
      }

      new Collector().collect();

      return trimWhitespace(results);
    }

    private Iterable<Trees.TemplatePart> trimWhitespace(final List<Trees.TemplatePart> results) {
      for (int i = 1; i < results.size() - 1; i++) {
        Trees.TemplatePart prev = results.get(i - 1);
        Trees.TemplatePart current = results.get(i);
        Trees.TemplatePart next = results.get(i + 1);

        if (prev instanceof TextLine
            && current instanceof Trees.Directive
            && next instanceof TextLine) {

          TextLine before = (TextLine) prev;
          Trees.Directive directive = (Trees.Directive) current;
          TextLine after = (TextLine) next;

          if (!before.newline()
              && before.isBlank()
              && after.newline()
              && after.isBlank()) {

            if (directive instanceof DirectiveStart) {
              // this used when doing invocation to use proper indenting
              results.set(i, ((DirectiveStart) directive).withBefore(before.fragment()));
            }

            // clearing whitespaces around tag on the same line
            results.set(i - 1, null);
            results.set(i + 1, null);
          }
        }
      }

      return FluentIterable.from(results)
          .filter(Predicates.notNull())
          .toList();
    }
  };
}
