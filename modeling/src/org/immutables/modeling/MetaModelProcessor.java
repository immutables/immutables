package org.immutables.modeling;

import com.google.auto.service.AutoService;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@AutoService(javax.annotation.processing.Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class MetaModelProcessor extends AbstractProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    return false;
  }
}
