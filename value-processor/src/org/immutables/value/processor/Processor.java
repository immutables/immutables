/*
   Copyright 2014-2019 Immutables Authors and Contributors

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
package org.immutables.value.processor;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.immutables.generator.AbstractGenerator;
import org.immutables.generator.ForwardingFiler;
import org.immutables.generator.ForwardingProcessingEnvironment;
import org.immutables.value.processor.encode.EncodingMirror;
import org.immutables.value.processor.encode.Generator_Encodings;
import org.immutables.value.processor.meta.*;
import org.immutables.value.processor.meta.Proto.DeclaringPackage;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

@SupportedAnnotationTypes({
    ImmutableMirror.QUALIFIED_NAME,
    EnclosingMirror.QUALIFIED_NAME,
    IncludeMirror.QUALIFIED_NAME,
    ModifiableMirror.QUALIFIED_NAME,
    ValueUmbrellaMirror.QUALIFIED_NAME,
    FactoryMirror.QUALIFIED_NAME,
    FConstructorMirror.QUALIFIED_NAME,
    FBuilderMirror.QUALIFIED_NAME,
    VBuilderMirror.QUALIFIED_NAME,
    FIncludeMirror.QUALIFIED_NAME,
    EncodingMirror.QUALIFIED_NAME,
    CriteriaMirror.QUALIFIED_NAME,
    CriteriaRepositoryMirror.QUALIFIED_NAME
})
public final class Processor extends AbstractGenerator {
  @Override
  protected void process() {
    prepareOptions();

    Round round = ImmutableRound.builder()
        .addAllAnnotations(annotations())
        .processing(processing())
        .addAllCustomImmutableAnnotations(CustomImmutableAnnotations.annotations())
        .round(round())
        .build();

    Multimap<DeclaringPackage, ValueType> values = round.collectValues();

    invoke(new Generator_Immutables().usingValues(values).generate());
    invoke(new Generator_Modifiables().usingValues(values).generate());

    if (round.environment().hasGsonLib()) {
      invoke(new Generator_Gsons().usingValues(values).generate());
    }
    if (round.environment().hasCriteriaModule()) {
      invoke(new Generator_Criteria().usingValues(values).generate());
      invoke(new Generator_CriteriaRepository().usingValues(values).generate());
    }
    if (round.environment().hasMongoModule()) {
      invoke(new Generator_Repositories().usingValues(values).generate());
    }
    if (round.environment().hasFuncModule()) {
      invoke(new Generator_Funcs().usingValues(values).generate());
    }
    if (round.environment().hasTreesModule()) {
      invoke(new Generator_Transformers().usingValues(values).generate());
      invoke(new Generator_Visitors().usingValues(values).generate());
    }
    if (round.environment().hasAstModule()) {
      invoke(new Generator_Asts().usingValues(values).generate());
    }
    if (round.environment().hasEncodeModule()) {
      invoke(new Generator_Encodings().generate());
    }
    if (round.environment().hasDatatypesModule()) {
      invoke(new Generator_Datatypes().usingValues(values).generate());
    }
  }

  private void prepareOptions() {
    UnshadeGuava.overridePrefix(processing().getOptions().get(GUAVA_PREFIX));
    UnshadeGuava.overridePrefix(processing().getOptions().get(JACKSON_PREFIX));
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return FluentIterable.from(super.getSupportedAnnotationTypes())
        .append(CustomImmutableAnnotations.annotations())
        .toSet();
  }

  private static final String GRADLE_INCREMENTAL = "immutables.gradle.incremental";
  private static final String GUAVA_PREFIX = "immutables.guava.prefix";
  private static final String JACKSON_PREFIX = "immutables.jackson.prefix";

  @Override
  public Set<String> getSupportedOptions() {
    ImmutableSet.Builder<String> options = ImmutableSet.builder();
    options.add(GRADLE_INCREMENTAL);
    if (processingEnv.getOptions().containsKey(GRADLE_INCREMENTAL)) {
      options.add("org.gradle.annotation.processing.isolating");
    }
    options.add(GUAVA_PREFIX);
    options.add(JACKSON_PREFIX);
    return options.build();
  }

  @Override
  public synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(new RestrictingIncrementalProcessingEnvironment(processingEnv));
  }

  private final class RestrictingIncrementalProcessingEnvironment extends ForwardingProcessingEnvironment {
    private final ProcessingEnvironment processingEnv;
    boolean incrementalRestrictions;
    private Filer restrictedFiler;

    private RestrictingIncrementalProcessingEnvironment(ProcessingEnvironment processingEnv) {
      this.processingEnv = processingEnv;
      this.incrementalRestrictions = processingEnv.getOptions().containsKey(GRADLE_INCREMENTAL);
    }

    @Override
    protected ProcessingEnvironment delegate() {
      return processingEnv;
    }

    @Override
    public Filer getFiler() {
      final Filer filer = super.getFiler();
      if (incrementalRestrictions) {
        if (restrictedFiler == null) {
          restrictedFiler = new ForwardingFiler() {
            @Override
            protected Filer delegate() {
              return filer;
            }

            @Override
            public FileObject createResource(
                Location location,
                CharSequence pkg,
                CharSequence relativeName,
                Element... originatingElements)
                throws IOException {
              String message = String.format("Suppressed writing of resource %s/%s/%s (triggered by enabling -A%s)",
                  location,
                  pkg,
                  relativeName,
                  GRADLE_INCREMENTAL);
              getMessager().printMessage(Kind.MANDATORY_WARNING, message);
              throw new FileNotFoundException(message);
            }
          };
        }
        return restrictedFiler;
      }
      return filer;
    }
  }
}
