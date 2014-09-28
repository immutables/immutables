package org.immutables.modeling.processing;

import org.immutables.modeling.templating.Spacing;
import java.io.Writer;
import org.immutables.modeling.templating.TemplateWriter;
import com.google.common.base.Joiner;
import javax.tools.JavaFileObject;
import org.immutables.modeling.Template;
import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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
import javax.tools.StandardLocation;
import org.immutables.modeling.introspect.SwissArmyKnife;
import org.immutables.modeling.templating.Balancing;
import org.immutables.modeling.templating.ImmutableTrees.Unit;
import org.immutables.modeling.templating.Parser;
import org.immutables.modeling.templating.Resolver;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

@AutoService(javax.annotation.processing.Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class TemplateGenerator extends AbstractProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
    if (!round.processingOver() && !round.errorRaised()) {
      processTemplates(round.getElementsAnnotatedWith(Template.class));
    }
    return true;
  }

  private void processTemplates(Set<? extends Element> templates) {
    for (TypeElement templateType : ElementFilter.typesIn(templates)) {
      try {
        SwissArmyKnife knife = new SwissArmyKnife(processingEnv, templateType);
        PackageElement packageElement = knife.elements.getPackageOf(templateType);
        FileObject templateResource =
            knife.environment.getFiler().getResource(
                StandardLocation.SOURCE_PATH,
                packageElement.getQualifiedName(),
                templateType.getSimpleName() + ".template");

        String string = templateResource.getCharContent(true).toString();

        Parser templateParser = Parboiled.createParser(Parser.class);

        ParsingResult<Object> result = new ReportingParseRunner<>(templateParser.Unit()).run(string);

        if (result.hasErrors()) {
          throw new Exception(ErrorUtils.printParseErrors(result.parseErrors));
        }

        Unit unit = (Unit) Iterables.getOnlyElement(result.valueStack);

        Unit trimmed = Spacing.trim(unit);
        Unit balanced = Balancing.balance(trimmed);
        Unit resolved = new Resolver(knife).resolve(balanced);

        String simpleName = "Template_" + templateType.getSimpleName();
        JavaFileObject templateFile =
            knife.environment.getFiler().createSourceFile(
                Joiner.on('.').join(packageElement.getQualifiedName(), simpleName),
                templateType);

        TemplateWriter writingTransformer = new TemplateWriter(knife, templateType, simpleName);

        try (Writer writer = templateFile.openWriter()) {
          writer.append(writingTransformer.toCharSequence(resolved));
        }

        System.out.println(resolved);

        // templatesType
      } catch (Exception ex) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.ERROR,
                ex.getMessage() + "\n\n" + Throwables.getStackTraceAsString(ex),
                templateType);
      }
    }
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(Template.class.getName());
  }
}
