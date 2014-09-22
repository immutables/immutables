package org.immutables.modeling.templating;

import org.immutables.modeling.templating.ImmutableTrees.Unit;
import org.immutables.modeling.templating.ImmutableTrees.AccessExpression;
import org.immutables.modeling.templating.Trees.Expression;

public final class Typer extends TreesTransformer<Void> {

  @Override
  public Unit transform(Void contextValue, Unit unitValue) {
    return super.transform(contextValue, unitValue);
  }

  @Override
  protected Expression transformExpression(Void contextValue, AccessExpression value) {
    return super.transformExpression(contextValue, value);
  }

}
