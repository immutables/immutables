package org.immutables.value.processor.meta;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

final class SuppressedWarnings {
  private static final String ALL = "all";
  private static final String IMMUTABLES = "immutables";
  private static final String GENERATED = "generated";

  final boolean all;
  final boolean immutables;
  final boolean generated;

  private SuppressedWarnings(
      boolean all,
      boolean immutables,
      boolean generated) {
    this.all = all;
    this.immutables = immutables;
    this.generated = generated;
  }

  static SuppressedWarnings forElement(Element element) {
    boolean all = false;
    boolean immutables = false;
    boolean generated = false;

    outer: for (Element e = element; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
      @Nullable SuppressWarnings suppressWarnings = e.getAnnotation(SuppressWarnings.class);
      if (suppressWarnings != null) {
        for (String w : suppressWarnings.value()) {
          switch (w) {
          case ALL:
            all = true;
            immutables = true;
            generated = true;
            break outer;
          case IMMUTABLES:
            immutables = true;
            break;
          case GENERATED:
            generated = true;
            break;
          default:
            break;
          }
        }
      }
    }
    return new SuppressedWarnings(all, immutables, generated);
  }
}
