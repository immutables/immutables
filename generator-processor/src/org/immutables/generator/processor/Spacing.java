/*
   Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.generator.processor;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import javax.annotation.Nullable;
import org.immutables.generator.processor.ImmutableTrees.Newline;
import org.immutables.generator.processor.ImmutableTrees.Template;
import org.immutables.generator.processor.ImmutableTrees.TextBlock;
import org.immutables.generator.processor.ImmutableTrees.TextFragment;
import org.immutables.generator.processor.ImmutableTrees.TextLine;
import org.immutables.generator.processor.ImmutableTrees.Unit;
import org.immutables.generator.processor.Trees.TextPart;

/**
 * Spacing trimming and redistribution should be run before balancing
 */
final class Spacing extends TreesTransformer {
  private Spacing() {}

  static Unit normalize(Unit unit) {
    return new Spacing().toUnit(unit);
  }

  @Override
  public Template toTemplate(final Template template) {
    final ArrayList<Trees.TemplatePart> results = Lists.newArrayList();
    class Normalizer {
      private @Nullable TextFragment fragment;

      void collect() {
        for (Trees.TemplatePart part : template.parts()) {
          if (part instanceof TextBlock) {
            collectTextParts(((TextBlock) part).parts());
          } else {
            flushFragment();
            results.add(part);
          }
        }

        flushFragment();
      }

      void collectTextParts(Iterable<TextPart> parts) {
        for (Trees.TextPart text : parts) {
          if (text instanceof TextFragment) {
            if (fragment != null) {
              fragment = joinFraments(fragment, (TextFragment) text);
            } else {
              flushFragment();
              fragment = (TextFragment) text;
            }
          }
          if (text instanceof Newline) {
            flushNewline();
          }
        }
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

      TextFragment joinFraments(TextFragment left, TextFragment right) {
        return TextFragment.of(left.value().concat(right.value()));
      }
    }

    new Normalizer().collect();

    return template.withParts(results);
  }
}
