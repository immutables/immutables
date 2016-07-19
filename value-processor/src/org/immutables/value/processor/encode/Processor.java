package org.immutables.value.processor.encode;

import javax.annotation.processing.SupportedAnnotationTypes;
import org.immutables.generator.AbstractGenerator;
import org.immutables.metainf.Metainf;

@Metainf.Service
@SupportedAnnotationTypes(EncodingMirror.QUALIFIED_NAME)
public class Processor extends AbstractGenerator {
  @Override
  protected void process() {
		invoke(newTemplate(Encodings.class).generate());
  }
}
