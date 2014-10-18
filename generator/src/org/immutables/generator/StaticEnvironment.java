package org.immutables.generator;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MutableClassToInstanceMap;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

final class StaticEnvironment {
  private StaticEnvironment() {}

  private static ClassToInstanceMap<Completable> components;
  private static ProcessingEnvironment processing;
  private static RoundEnvironment round;
  private static Set<TypeElement> annotations;
  private static boolean initialized;

  interface Completable {
    void complete();
  }

  static <T extends Completable> T getInstance(Class<T> type, Supplier<T> supplier) {
    checkInitialized();
    @Nullable
    T instance = components.getInstance(type);
    if (instance == null) {
      instance = supplier.get();
      components.putInstance(type, instance);
    }
    return instance;
  }

  static ProcessingEnvironment processing() {
    checkInitialized();
    return processing;
  }

  static RoundEnvironment round() {
    checkInitialized();
    return round;
  }

  static Set<TypeElement> annotations() {
    checkInitialized();
    return annotations;
  }

  private static void checkInitialized() {
    Preconditions.checkState(initialized, "Static environment should be initialized");
  }

  static void shutdown() throws Exception {
    for (Completable component : components.values()) {
      component.complete();
    }
    components = null;
    processing = null;
    round = null;
    annotations = null;
    initialized = false;
  }

  static void init(
      Set<? extends TypeElement> annotations,
      RoundEnvironment round,
      ProcessingEnvironment processing) {
    StaticEnvironment.components = MutableClassToInstanceMap.create();
    StaticEnvironment.processing = processing;
    StaticEnvironment.round = round;
    StaticEnvironment.annotations = ImmutableSet.copyOf(annotations);
    StaticEnvironment.initialized = true;
  }

}
