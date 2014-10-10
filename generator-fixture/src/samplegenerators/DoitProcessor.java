package samplegenerators;

import com.google.auto.service.AutoService;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import org.immutables.generator.AbstractGenerator;
import org.immutables.generator.Generator;

@AutoService(Processor.class)
@Generator.SupportedAnnotations(Doit.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class DoitProcessor extends AbstractGenerator {
  @Override
  protected void process() {
    invoke(new Generator_Doer().main());
  }
}
