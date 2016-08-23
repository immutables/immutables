/*
   Copyright 2016 Immutables Authors and Contributors

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
package org.immutables.value.processor.encode;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.immutables.value.processor.encode.Code.Term;
import org.immutables.value.processor.encode.Structurizer.Statement;

final class SourceMapper {
  private static final Statement EMPTY_STATEMENT = new Statement.Builder().build();

  final Map<String, Statement> definitions = new LinkedHashMap<>();
  final Function<String, Statement> get = Functions.forMap(definitions, EMPTY_STATEMENT);

  SourceMapper(CharSequence source) {
    List<Term> terms = Code.termsFrom(source.toString());
    mapDefinitions("", new Structurizer(terms).structurize());
  }

  private void mapDefinitions(String prefix, List<Statement> statements) {
    for (Statement statement : statements) {
      if (statement.isClass()) {
        mapDefinitions(prefix + statement.name().get() + ".", statement.definitions());
      } else if (statement.name().isPresent()) {
        String suffix = statement.block().isEmpty() ? "" : "()";
        definitions.put(prefix + statement.name().get() + suffix, statement);
      }
    }
  }

  List<Term> getExpression(String path) {
    return get.apply(path).expression();
  }

  List<Term> getBlock(String path) {
    return get.apply(path).block();
  }

  List<Term> getAnnotations(String path) {
    return get.apply(path).annotations();
  }
}
