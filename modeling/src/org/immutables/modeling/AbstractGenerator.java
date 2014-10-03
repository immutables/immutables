package org.immutables.modeling;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import static com.google.common.base.Preconditions.*;

/**
 * Extend this abstract processor to propertly initalize and call templates.
 * @see #process()
 */
public abstract class AbstractGenerator extends AbstractProcessor {

  /**
   * Override process method and call {@link #invoke(org.immutables.modeling.Templates.Invokable)}
   * from inside it passing invokable fragments from generated template instances.
   */
  protected abstract void process();

  protected final void invoke(Templates.Invokable invokable) {
    checkArgument(invokable.arity() == 0, "Entry template fragment should not have parameters");
    invokable.invoke(Templates.Invokation.initial());
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
