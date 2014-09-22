package org.immutables.modeling.templating;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import org.immutables.modeling.templating.ImmutableTrees.Template;
import org.immutables.modeling.templating.ImmutableTrees.Unit;
import org.immutables.modeling.templating.Trees.UnitPart;

abstract class UnitTransformer implements Function<Unit, Unit> {
  @Override
  public Unit apply(Unit unit) {
    return unit.withParts(transformParts(unit.parts()));
  }

  Iterable<? extends UnitPart> transformParts(Iterable<UnitPart> parts) {
    ImmutableList.Builder<UnitPart> builder = ImmutableList.builder();
    for (Trees.UnitPart part : parts) {
      builder.add(transformPart(part));
    }
    return builder.build();
  }

  UnitPart transformPart(UnitPart part) {
    if (part instanceof Template) {
      Template template = (Template) part;

      return transformTemplate(template);
    }
    return part;
  }

  Template transformTemplate(Template template) {
    return template;
  }
}
