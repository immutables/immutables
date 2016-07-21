package org.immutables.value.processor.encode;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Parameterizable;
import org.immutables.value.processor.encode.Inflater.EncodingInfo;
import org.immutables.value.processor.encode.Type.Parameterized;
import org.immutables.value.processor.encode.Type.Variable;
import org.immutables.value.processor.encode.Type.VariableResolver;
import org.immutables.value.processor.encode.Type.Wildcard;
import org.immutables.value.processor.meta.Reporter;

public final class Instantiator {
  private final Type.Factory typeFactory;
  private final Multimap<Type, TemplateAnchor> anchors = HashMultimap.create();

  Instantiator(Type.Factory typeFactory, Set<EncodingInfo> encodings) {
    this.typeFactory = typeFactory;

    for (EncodingInfo encoding : encodings) {
      for (EncodedElement exposedElement : encoding.expose()) {
        Type raw = getRaw(exposedElement);
        anchors.put(raw, new TemplateAnchor(encoding, exposedElement));
      }
    }
  }

  private Type getRaw(EncodedElement exposedElement) {
    return exposedElement.type().accept(new Type.Transformer() {
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

  public final class InstantiationCollector {
    private final TypeExtractor typeExtractor;
    private final Set<String> imports = new LinkedHashSet<>();
    private final Map<String, Instantiation> instantiations = new LinkedHashMap<>();

    public InstantiationCollector(Parameterizable parameterizable) {
      this.typeExtractor = new TypeExtractor(typeFactory, parameterizable);
    }

    Optional<Instantiation> tryInstantiateFor(Reporter reporter, String name, String typeString) {
      // we use parse/string here to reuse all cryptic logic to extract type strings
      // when resolving not-yet-generated types and othe complex cases
      Type type = typeExtractor.parser.parse(typeString);
      @Nullable List<TemplateAnchor> contenders = null;
      @Nullable TemplateAnchor winner = null;
      @Nullable VariableResolver winnerResolver = null;

      for (TemplateAnchor anchor : anchors.get(type)) {
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
              name, winner, contenders);
        }

        Instantiation instantiation =
            new Instantiation(
                reporter,
                typeExtractor,
                winner.encoding,
                winner.exposedElement,
                type,
                name,
                winnerResolver);

        instantiations.put(name, instantiation);

        imports.addAll(winner.encoding.imports());
      }

      return Optional.absent();
    }
  }

  public class Instantiation {
    private final Reporter reporter;
    private final TypeExtractor typeExtractor;
    private final EncodingInfo encoding;
    private final EncodedElement exposedAs;
    private final VariableResolver winnerResolver;
    private final Type actualType;
    private final String name;

    Instantiation(
        Reporter reporter,
        TypeExtractor typeExtractor,
        EncodingInfo encoding,
        EncodedElement exposedElement,
        Type actualType,
        String name,
        VariableResolver resolver) {
      this.reporter = reporter;
      this.typeExtractor = typeExtractor;
      this.encoding = encoding;
      this.exposedAs = exposedElement;
      this.actualType = actualType;
      this.name = name;
      this.winnerResolver = resolver;
    }

    void generate() {
      Map<String, String> generatedNames = new HashMap<>(encoding.element().size());

      for (EncodedElement e : encoding.element()) {
        generatedNames.put(e.name(), e.naming().apply(name));
      }
    }

    @Override
    public String toString() {
      return actualType + "(by " + encoding.name() + ")";
    }
//
//    static class AccessTrackingFunction implements Function<String, String> {
//      AccessTrackingFunction
//      AccessTracker(Map<String, String>) {
//
//      }
//    }
  }
}
