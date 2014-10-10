package org.immutables.generator;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import org.immutables.generator.Generator.SupportedAnnotations;
import static com.google.common.base.Preconditions.*;

/**
 * Extend this abstract processor to propertly initalize and call templates.
 * @see #process()
 */
public abstract class AbstractGenerator extends AbstractProcessor {

  /**
   * Override process method and call {@link #invoke(org.immutables.generator.Templates.Invokable)}
   * from inside it passing invokable fragments from generated template instances.
   */
  protected abstract void process();

  protected final void invoke(Templates.Invokable invokable) {
    checkArgument(invokable.arity() == 0, "Entry template fragment should not have parameters");
    invokable.invoke(Templates.Invokation.initial());
  }

  @Override
  public final Set<String> getSupportedAnnotationTypes() {
    Set<String> annotationNames = Sets.newHashSet();
    @Nullable
    SupportedAnnotations annotations = getClass().getAnnotation(Generator.SupportedAnnotations.class);
    if (annotations != null) {
      for (Class<?> c : annotations.value()) {
        annotationNames.add(c.getCanonicalName());
      }
    }
    annotationNames.addAll(super.getSupportedAnnotationTypes());
    return ImmutableSet.copyOf(annotationNames);
  }

  @Override
  public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
    try {
      StaticEnvironment.init(annotations, round, processingEnv);
      if (!round.processingOver() && !round.errorRaised()) {
        process();
      }
      StaticEnvironment.shutdown();
    } catch (Exception ex) {
      processingEnv.getMessager()
          .printMessage(Diagnostic.Kind.ERROR,
              Joiner.on('\n').join(
                  AbstractGenerator.class.getName() + " threw", Throwables.getStackTraceAsString(ex)));
    }
    return false;
  }
}
