package samplegenerators;

import javax.lang.model.SourceVersion;
import javax.annotation.processing.SupportedSourceVersion;
import javax.annotation.processing.SupportedAnnotationTypes;
import org.immutables.modeling.AbstractGenerator;
import javax.annotation.processing.Processor;
import com.google.auto.service.AutoService;

@AutoService(Processor.class)
@SupportedAnnotationTypes("samplegenerators.Doit")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class DoitProcessor extends AbstractGenerator {
  @Override
  protected void process() {
    invoke(new Generator_Doer().main());
  }
}
