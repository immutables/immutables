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
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.generator.processor.ImmutableTrees.Newline;
import org.immutables.generator.processor.ImmutableTrees.Template;
import org.immutables.generator.processor.ImmutableTrees.TextBlock;
import org.immutables.generator.processor.ImmutableTrees.TextFragment;
import org.immutables.generator.processor.ImmutableTrees.TextLine;
import org.immutables.generator.processor.ImmutableTrees.Unit;

/**
 * Spacing trimming and redistribution should be run before balancing
 */
public final class Spacing {
  private Spacing() {}

  public static Unit normalize(Unit unit) {
    return TRANSFORMER.transform((Void) null, unit);
  }

  private static final TreesTransformer<Void> TRANSFORMER = new TreesTransformer<Void>() {
    @Override
    protected Iterable<Trees.TemplatePart> transformTemplateListParts(
        Void context,
        Template value,
        final List<Trees.TemplatePart> parts) {

      final ArrayList<Trees.TemplatePart> results = Lists.newArrayList();

      class Normalizer {
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

      new Normalizer().collect();

      return results;
    }
  };
}
