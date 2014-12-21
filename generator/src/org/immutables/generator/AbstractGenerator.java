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
package org.immutables.generator;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
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

  protected final ProcessingEnvironment processing() {
    return StaticEnvironment.processing();
  }

  protected final RoundEnvironment round() {
    return StaticEnvironment.round();
  }

  protected final Set<TypeElement> annotations() {
    return StaticEnvironment.annotations();
  }

  protected final void invoke(Templates.Invokable invokable) {
    checkArgument(invokable.arity() == 0, "Entry template fragment should not have parameters");
    invokable.invoke(Templates.Invokation.initial());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    @Nullable SupportedSourceVersion sourceVersion = this.getClass().getAnnotation(SupportedSourceVersion.class);
    if (sourceVersion != null) {
      return sourceVersion.value();
    }
    return SourceVersion.latestSupported();
  }

  @Override
  public final Set<String> getSupportedAnnotationTypes() {
    @Nullable SupportedAnnotations annotations = getClass().getAnnotation(Generator.SupportedAnnotations.class);
    if (annotations != null) {
      Set<String> annotationNames = Sets.newHashSet();
      for (Class<?> c : annotations.value()) {
        annotationNames.add(c.getCanonicalName());
      }
      return ImmutableSet.copyOf(annotationNames);
    }
    return super.getSupportedAnnotationTypes();
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
