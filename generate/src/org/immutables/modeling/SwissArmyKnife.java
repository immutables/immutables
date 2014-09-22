package org.immutables.modeling;

import org.immutables.modeling.Facets.FacetResolver;
import com.google.common.collect.ImmutableMap;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.element.TypeElement;
import javax.annotation.processing.ProcessingEnvironment;

public class SwissArmyKnife {

  public final ImmutableMap<String, TypeMirror> imports;
  public final FacetResolver facetResolver;
  public final Accessors accessors;
  public final Binder binder;

  public SwissArmyKnife(ProcessingEnvironment environment, TypeElement element) {
    this.imports = new Imports(environment).importsIn(element);
    this.facetResolver = new Facets(environment).resolverFrom(imports.values());
    this.accessors = new Accessors(environment);
    this.binder = new Binder(accessors, facetResolver);
  }

}
