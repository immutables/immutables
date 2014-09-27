package org.immutables.modeling.templating;

import org.immutables.modeling.templating.ImmutableTrees.TextBlock;
import com.google.common.collect.Lists;
import java.util.List;
import org.immutables.modeling.templating.ImmutableTrees.Template;
import org.immutables.modeling.templating.ImmutableTrees.Unit;

/**
 * Spacing redistribution should be run before balancing
 */
public final class Spacing {
  private Spacing() {}

  public static Unit redistribute(Unit unit) {
    return TRANSFORMER.transform((Void) null, unit);
  }

  private static final TreesTransformer<Void> TRANSFORMER = new TreesTransformer<Void>() {
    @Override
    protected Iterable<Trees.TemplatePart> transformTemplateParts(
        Void context,
        Template value,
        List<Trees.TemplatePart> parts) {

      List<Trees.TemplatePart> results = Lists.newArrayListWithExpectedSize(parts.size());

      for (Trees.TemplatePart part : parts) {
        if (part instanceof TextBlock) {

        }
        if (part instanceof Trees.Directive) {
          
        }
      }

      return results;
    }
  };
}
