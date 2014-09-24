package org.immutables.modeling;

import javax.lang.model.util.Types;
import javax.lang.model.util.Elements;
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
  public final Elements elements;
  public final Types types;
  public final TypeElement type;

  public SwissArmyKnife(ProcessingEnvironment environment, TypeElement type) {
    this.type = type;
    this.elements = environment.getElementUtils();
    this.types = environment.getTypeUtils();
    this.imports = new Imports(environment).importsIn(type);
    this.facetResolver = new Facets(environment).resolverFrom(imports.values());
    this.accessors = new Accessors(environment);
    this.binder = new Binder(accessors, facetResolver);
  }

}
