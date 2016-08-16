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

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Parameterizable;
import org.immutables.value.processor.encode.Type.Parameterized;
import org.immutables.value.processor.encode.Type.Variable;
import org.immutables.value.processor.encode.Type.VariableResolver;
import org.immutables.value.processor.encode.Type.Wildcard;
import org.immutables.value.processor.meta.Reporter;
import org.immutables.value.processor.meta.Styles;

public final class Instantiator {
  private final Type.Factory typeFactory;
  private final Multimap<Type, TemplateAnchor> anchors = HashMultimap.create();

  Instantiator(Type.Factory typeFactory, Set<EncodingInfo> encodings) {
    this.typeFactory = typeFactory;

    for (EncodingInfo encoding : encodings) {
      for (EncodedElement e : encoding.element()) {
        if (e.isExpose()) {
          Type raw = getRaw(e.type());
          this.anchors.put(raw, new TemplateAnchor(encoding, e));
        }
      }
    }
  }

  public boolean isEmpty() {
    return anchors.isEmpty();
  }

  public @Nullable InstantiationCreator creatorFor(Parameterizable element) {
    return !isEmpty() ? new InstantiationCreator(element) : null;
  }

  private Type getRaw(Type type) {
    return type.accept(new Type.Transformer() {
      @Override
      public Type parameterized(Parameterized parameterized) {
        return parameterized.reference;
      }

      @Override
      public Type extendsWildcard(Wildcard.Extends wildcard) {
        return typeFactory.extendsWildcard(Type.OBJECT);
      }

      @Override
      public Type superWildcard(Wildcard.Super wildcard) {
        return typeFactory.extendsWildcard(Type.OBJECT);
      }

      @Override
      public Type variable(Variable variable) {
        return typeFactory.extendsWildcard(Type.OBJECT);
      }
    });
  }

  private static class TemplateAnchor {
    final Type.Template template;
    final EncodedElement exposedElement;
    final EncodingInfo encoding;

    TemplateAnchor(EncodingInfo encoding, EncodedElement exposedElement) {
      this.encoding = encoding;
      this.exposedElement = exposedElement;
      this.template = new Type.Template(exposedElement.type());
    }

    @Override
    public String toString() {
      return template.template + " (from " + encoding.name() + ")";
    }
  }

  public final class InstantiationCreator {
    public final Set<String> imports = new LinkedHashSet<>();
    private final TypeExtractor typeExtractor;

    InstantiationCreator(Parameterizable parameterizable) {
      this.typeExtractor = new TypeExtractor(typeFactory, parameterizable);
    }

    public @Nullable Instantiation tryInstantiateFor(
        Reporter reporter,
        String typeString,
        Styles.UsingName.AttributeNames names) {

      // we use parse/string here to reuse all cryptic logic to extract type strings
      // when resolving not-yet-generated types and othe complex cases
      // the alternative would be just to read TypeMirror directly
      Type type = typeExtractor.parser.parse(typeString);
      @Nullable List<TemplateAnchor> contenders = null;
      @Nullable TemplateAnchor winner = null;
      @Nullable VariableResolver winnerResolver = null;

      for (TemplateAnchor anchor : anchors.get(getRaw(type))) {
        Optional<VariableResolver> match = anchor.template.match(type);
        if (match.isPresent()) {
          if (winner == null) {
            winner = anchor;
            winnerResolver = match.get();
          } else {
            if (contenders == null) {
              contenders = new ArrayList<>();
            }
            contenders.add(anchor);
          }
        }
      }

      if (winner != null) {
        if (contenders != null) {
          reporter.warning(
              "Encoding conflict for attribute '%s', the winning match: %s. Other applicable: %s",
              names.raw, winner, contenders);
        }

        imports.addAll(winner.encoding.imports());
//        System.err.println("!!!!!ENC: " + type + " " + names.raw + ": " + winnerResolver);

        return new Instantiation(
            winner.encoding,
            winner.exposedElement,
            type,
            names,
            winnerResolver);
      }

      return null;
    }
  }
}
