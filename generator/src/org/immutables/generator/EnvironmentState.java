package org.immutables.generator;

import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MutableClassToInstanceMap;

/**
 * Next iteration of the same flawed design in hopes to untangle it at least a bit.
 */
public class EnvironmentState {
  static final ThreadLocal<EnvironmentState> currentState = new ThreadLocal<EnvironmentState>();

  private static EnvironmentState state() {
    return Preconditions.checkNotNull(currentState.get(), "Static environment should be initialized");
  }

  public static <T extends Runnable> T getPerRound(Class<T> type, Supplier<T> supplier) {
    EnvironmentState state = state();
    @Nullable T instance = state.afterRound.getInstance(type);
    if (instance == null) {
      state.afterRound.putInstance(type, instance = supplier.get());
    }
    return instance;
  }

  public static <T extends Runnable> T getPerProcessing(Class<T> type, Supplier<T> supplier) {
    EnvironmentState state = state();
    @Nullable T instance = state.afterProcessing.getInstance(type);
    if (instance == null) {
      state.afterProcessing.putInstance(type, instance = supplier.get());
    }
    return instance;
  }

  public static ProcessingEnvironment processing() {
    return state().processing;
  }

  public static RoundEnvironment round() {
    return state().round;
  }

  static Set<TypeElement> annotations() {
    return state().annotations;
  }

  private ProcessingEnvironment processing;
  private RoundEnvironment round;
  private Set<TypeElement> annotations;

  private final ClassToInstanceMap<Runnable> afterProcessing = MutableClassToInstanceMap.create();
  private final ClassToInstanceMap<Runnable> afterRound = MutableClassToInstanceMap.create();

  void initProcessing(ProcessingEnvironment processing) {
    this.processing = processing;
    currentState.set(this);
  }

  void initRound(Set<? extends TypeElement> annotations, RoundEnvironment round) {
    this.round = round;
    this.annotations = ImmutableSet.copyOf(annotations);
  }

  void completeRound() {
    for (Runnable r : afterRound.values()) {
      r.run();
    }
    afterRound.clear();
    annotations = null;
    round = null;
  }

  void completeProcessing() {
    for (Runnable r : afterProcessing.values()) {
      r.run();
    }
    afterProcessing.clear();
    currentState.remove();
  }
}
