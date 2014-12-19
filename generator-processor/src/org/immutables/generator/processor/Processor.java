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
package org.immutables.generator.processor;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
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
import org.immutables.generator.Generator;
import org.immutables.generator.processor.ImmutableTrees.Unit;
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

  private Unit parseUnit(String templateText) throws Exception {
    ParsingResult<Object> result = new ReportingParseRunner<>(parser.Unit()).run(templateText);

    if (result.hasErrors()) {
      String errors = ErrorUtils.printParseErrors(result.parseErrors);
      throw new Exception(errors);
    }

    return (Unit) Iterables.getOnlyElement(result.valueStack);
  }

  private Unit transformUnit(SwissArmyKnife knife, Unit unit) {
    Unit trimmed = Spacing.normalize(unit);
    Unit balanced = Balancing.balance(trimmed);
    Unit resolved = new TypeResolver(knife).resolve(balanced);
    return resolved;
  }
}
