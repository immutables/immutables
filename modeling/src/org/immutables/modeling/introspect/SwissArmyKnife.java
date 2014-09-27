package org.immutables.modeling.introspect;

import com.google.common.collect.ImmutableMap;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.immutables.modeling.introspect.Facets.FacetResolver;

/**
 * Temporary "global context" class while interactions and structure is not sorted out well.
 * While there is no injection
 */
public class SwissArmyKnife {

  public final ImmutableMap<String, TypeMirror> imports;
  public final FacetResolver facetResolver;
  public final Accessors accessors;
  public final Accessors.Binder binder;
  public final Elements elements;
  public final Types types;
  public final TypeElement type;
  public final ProcessingEnvironment environment;

  public SwissArmyKnife(ProcessingEnvironment environment, TypeElement type) {
    this.environment = environment;
    this.type = type;
    this.elements = environment.getElementUtils();
    this.types = environment.getTypeUtils();
    this.imports = new Imports(environment).importsIn(type);
    this.facetResolver = new Facets(environment).resolverFrom(imports.values());
    this.accessors = new Accessors(environment);
    this.binder = accessors.binder(facetResolver);
  }

}
