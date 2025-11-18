package org.immutables.value.processor.meta;

import com.google.common.base.Preconditions;

public enum AnnotationPick {
  /** Old, legacy behaviour, includes all auto-detection peculiarities */
  LEGACY,
  /** No auto annotation pick */
  NONE,
  /**
   * {@code javax.annotation.*} and {@code javax.validation.*} annotations.
   * Except for {@code javax.annotation.processing.Generated},
   * see {@link #JAVAX_AND_PROCESSING}
   */
  JAVAX,
  /** {@code javax.annotation.*} and {@code javax.validation.*} annotations. Same as {@link #JAVAX} and {@code javax.annotation.processing
   * .Generated}) */
  JAVAX_AND_PROCESSING,
  /** {@code jakarta.annotation.*} and {@code jakarta.validation.*} annotations. */
  JAKARTA;

  private static volatile AnnotationPick pick = LEGACY;

  /** Can be overridden to change the default */
  public static AnnotationPick pick() {
    return pick;
  }

  public static void overridePick(AnnotationPick pick) {
    Preconditions.checkNotNull(pick);
    AnnotationPick.pick = pick;
  }
}
