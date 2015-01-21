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

/**
 * This design is flawed and should be removed. Last time it was not done "right"
 * because of lack of some form of dependency injection in generators and infrastructure.
 */
final class StaticEnvironment {
  private StaticEnvironment() {}

  private static class EnvironmentState {
    private ClassToInstanceMap<Completable> components;
    private ProcessingEnvironment processing;
    private RoundEnvironment round;
    private Set<TypeElement> annotations;
    private boolean initialized;

    void shutdown() {
      for (Completable component : components.values()) {
        component.complete();
      }
      components = null;
      processing = null;
      round = null;
      annotations = null;
      initialized = false;

      state.remove();
    }

    void init(Set<? extends TypeElement> annotations, RoundEnvironment round, ProcessingEnvironment processing) {
      this.components = MutableClassToInstanceMap.create();
      this.processing = processing;
      this.round = round;
      this.annotations = ImmutableSet.copyOf(annotations);
      this.initialized = true;
    }
  }

  private static final ThreadLocal<EnvironmentState> state = new ThreadLocal<EnvironmentState>() {
    @Override
    protected EnvironmentState initialValue() {
      return new EnvironmentState();
    }
  };

  private static EnvironmentState state() {
    EnvironmentState s = state.get();
    Preconditions.checkState(s.initialized, "Static environment should be initialized");
    return s;
  }

  interface Completable {
    void complete();
  }

  static <T extends Completable> T getInstance(Class<T> type, Supplier<T> supplier) {
    ClassToInstanceMap<Completable> components = state().components;
    @Nullable T instance = components.getInstance(type);
    if (instance == null) {
      instance = supplier.get();
      components.putInstance(type, instance);
    }
    return instance;
  }

  static ProcessingEnvironment processing() {
    return state().processing;
  }

  static RoundEnvironment round() {
    return state().round;
  }

  static Set<TypeElement> annotations() {
    return state().annotations;
  }

  static void shutdown() throws Exception {
    state().shutdown();
  }

  static void init(
      Set<? extends TypeElement> annotations,
      RoundEnvironment round,
      ProcessingEnvironment processing) {
    state.get().init(annotations, round, processing);
  }

}
