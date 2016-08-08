package org.immutables.value.processor.encode;

import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.generator.Templates;
import org.immutables.value.processor.meta.ValueAttribute;

@Generator.Template
public abstract class Renderers extends AbstractTemplate {
  @Generator.Typedef
  Instantiation Inst;

  @Generator.Typedef
  ValueAttribute Attribute;

  @Generator.Typedef
  Templates.Invokable Invokable;

  @Generator.Typedef
  Code.Interpolator Interpolator;

  @Generator.Typedef
  EncodedElement Elem;

  @Generator.Typedef
  EncodingInfo Enc;

  @Generator.Typedef
  Code.Term Term;

  public abstract Templates.Invokable declareFields();

  public abstract Templates.Invokable defaultValue();

  public abstract Templates.Invokable assignDefaultFields();

  public abstract Templates.Invokable staticFields();

  public abstract Templates.Invokable staticMethods();

  public abstract Templates.Invokable builderFields();

  public abstract Templates.Invokable builderInit();

  public abstract Templates.Invokable builderStaticFields();

  public abstract Templates.Invokable builderHelperMethods();

  public abstract Templates.Invokable valueHelperMethods();

  public abstract Templates.Invokable copyMethods();

  public abstract Templates.Invokable accessor();

  public abstract Templates.Invokable string();

  public abstract Templates.Invokable hash();

  public abstract Templates.Invokable equals();

  public abstract Templates.Invokable from();

  public abstract Templates.Invokable implType();

  public abstract Templates.Invokable fromBuild();

  public abstract Templates.Invokable builderCopyFrom();
}
