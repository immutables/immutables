package org.immutables.modeling.processing;

import java.io.IOException;
import javax.annotation.processing.Filer;
import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.Writer;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.immutables.modeling.Generator;
import org.immutables.modeling.templating.Balancing;
import org.immutables.modeling.templating.ImmutableTrees.Unit;
import org.immutables.modeling.templating.Parser;
import org.immutables.modeling.templating.Resolver;
import org.immutables.modeling.templating.Spacing;
import org.immutables.modeling.templating.TemplateWriter;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

@AutoService(javax.annotation.processing.Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class Processor extends AbstractProcessor {

  private final Parser parser = Parboiled.createParser(Parser.class);

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(Generator.Template.class.getCanonicalName());
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
    if (!round.processingOver() && !round.errorRaised()) {
      processTemplates(round.getElementsAnnotatedWith(Generator.Template.class));
    }
    return true;
  }

  private void processTemplates(Set<? extends Element> templates) {
    for (TypeElement templateType : ElementFilter.typesIn(templates)) {
      try {
        generateTemplateType(templateType);
      } catch (Exception ex) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.ERROR,
                ex.getMessage() + "\n\n" + Throwables.getStackTraceAsString(ex),
                templateType);
      }
    }
  }

  private void generateTemplateType(TypeElement templateType) throws IOException, Exception {
    SwissArmyKnife knife = new SwissArmyKnife(processingEnv, templateType);
    String string = readTemplateResource(templateType, knife);

    Unit unit = parseUnit(string);

    Unit resolved = transformUnit(knife, unit);

    TemplateWriter writingTransformer =
        new TemplateWriter(knife, templateType, GeneratedTypes.getSimpleName(templateType));

    CharSequence template = writingTransformer.toCharSequence(resolved);

    JavaFileObject templateFile =
        knife.environment.getFiler().createSourceFile(
            GeneratedTypes.getQualifiedName(knife.elements, templateType),
            templateType);

    try (Writer writer = templateFile.openWriter()) {
      writer.append(template);
    }
  }

  private String readTemplateResource(TypeElement templateType, SwissArmyKnife knife) throws IOException {
    PackageElement packageElement = knife.elements.getPackageOf(templateType);

    Filer filer = knife.environment.getFiler();

    FileObject templateResource =
        filer.getResource(
            StandardLocation.SOURCE_PATH,
            packageElement.getQualifiedName(),
            templateType.getSimpleName() + ".generator");

    return templateResource.getCharContent(true).toString();
  }

  private Unit parseUnit(String string) throws Exception {
    ParsingResult<Object> result = new ReportingParseRunner<>(parser.Unit()).run(string);

    if (result.hasErrors()) {
      throw new Exception(ErrorUtils.printParseErrors(result.parseErrors));
    }

    return (Unit) Iterables.getOnlyElement(result.valueStack);
  }

  private Unit transformUnit(SwissArmyKnife knife, Unit unit) {
    Unit trimmed = Spacing.trim(unit);
    Unit balanced = Balancing.balance(trimmed);
    Unit resolved = new Resolver(knife).resolve(balanced);
    return resolved;
  }
}
