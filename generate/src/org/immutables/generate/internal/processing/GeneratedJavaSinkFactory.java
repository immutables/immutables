/*
    Copyright 2013-2014 Immutables.org authors

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
package org.immutables.generate.internal.processing;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteSink;
import com.google.common.reflect.Reflection;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

public class GeneratedJavaSinkFactory {

  private final ProcessingEnvironment environment;
  private final TypeElement originatingElement;

  public GeneratedJavaSinkFactory(ProcessingEnvironment environment, TypeElement originatingElement) {
    this.environment = environment;
    this.originatingElement = originatingElement;
  }

  public GeneratedJavaSink sinkFor(String fullyQualifiedName) throws IOException {
    return new GeneratedJavaSink(
        environment.getFiler().createSourceFile(fullyQualifiedName, originatingElement));
  }

  public static class GeneratedJavaSink {
    private static final Pattern FULLY_QUALIFIED_PATTERN =
        Pattern.compile("(\\W)(([a-z0-9_]+\\.)+)([A-Z][A-Za-z0-9_]*)");

    private static final Splitter LINE_SPLITTER = Splitter.on('\n');
    private final JavaFileObject sourceFile;

    private GeneratedJavaSink(JavaFileObject sourceFile) {
      this.sourceFile = sourceFile;
    }

    public void write(CharSequence content) throws IOException {
      CharSequence postProcessedLines = postProcessGeneratedLines(content);

      new ByteSink() {
        @Override
        public OutputStream openStream() throws IOException {
          return sourceFile.openOutputStream();
        }
      }.asCharSink(Charsets.UTF_8).write(postProcessedLines);
    }

    private CharSequence postProcessGeneratedLines(CharSequence content) {
      List<String> modifiedLines = Lists.newArrayList();
      Set<String> importStatements = Sets.newTreeSet();
      @Nullable
      String packageName = null;
      int indexOfGenImportsPlaceholder = -1;
      for (String l : LINE_SPLITTER.split(content)) {
        if (packageName == null) {
          if (l.startsWith("package ")) {
            packageName = extractPackageName(l);
          }
        }
        if (l.startsWith("// Generated imports")) {
          indexOfGenImportsPlaceholder = modifiedLines.size();
          continue;
        }
        if (l.startsWith("import ")) {
          modifiedLines.add(l);
          continue;
        }
        Matcher matcher = FULLY_QUALIFIED_PATTERN.matcher(l);
        while (matcher.find()) {
          String importClass = matcher.group().substring(1);
          if (!Reflection.getPackageName(importClass).equals("java.lang")) {
            importStatements.add("import " + importClass + ";");
          }
        }
        modifiedLines.add(FULLY_QUALIFIED_PATTERN
            .matcher(l)
            .replaceAll("$1$4")
            .replace(",", ", ")
            .replace(",  ", ", "));
      }

      if (indexOfGenImportsPlaceholder > -1) {
        modifiedLines.addAll(indexOfGenImportsPlaceholder, importStatements);
      }
      return Joiner.on('\n').join(modifiedLines) + '\n';
    }

    private String extractPackageName(String l) {
      return l.replaceAll("package ([a-z0-9_\\.]+)", "$1");
    }
  }

}
