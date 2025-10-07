/*
   Copyright 2016-2025 Immutables Authors and Contributors

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
package org.immutables.value.processor.meta;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

public final class TelescopicBuild {
  private final ValueType type;
  public final List<TelescopicStage> stages;
  public final List<ValueAttribute> finals;

  public final String nameBuildFinal;
  public final String nameBuildFinalSimple;

  public final String nameBuildStart;
  public final String nameBuildStartSimple;

  private TelescopicBuild(ValueType type,
      List<TelescopicStage> initializers,
      List<ValueAttribute> nonMandatory) {
    this.type = type;
    this.stages = initializers;
    this.finals = nonMandatory;
    this.nameBuildFinal = toNameOfStage(type, "BuildFinal");
    this.nameBuildFinalSimple = nameBuildFinal.substring(nameBuildFinal.indexOf('.') + 1); // ok
    this.nameBuildStart = toNameOfStage(type, "BuildStart");
    this.nameBuildStartSimple = nameBuildStart.substring(nameBuildStart.indexOf('.') + 1); // ok
  }

  public TelescopicStage firstStage() {
    return stages.get(0);
  }

  static TelescopicBuild from(ValueType type, List<ValueAttribute> attributes) {
    List<ValueAttribute> mandatory = Lists.newArrayList();
    List<ValueAttribute> finals = Lists.newArrayList();

    for (ValueAttribute a : attributes) {
      if (a.isBuilderParameter) {
        continue;
      }
      if (a.isMandatory()) {
        mandatory.add(a);
      } else {
        finals.add(a);
      }
    }

    LinkedList<TelescopicStage> stages = Lists.newLinkedList();
    if (!mandatory.isEmpty()) {
      Collections.reverse(mandatory);
      @Nullable TelescopicStage next = null;
      for (ValueAttribute m : mandatory) {
        next = new TelescopicStage(type, m, next);
        stages.addFirst(next);
      }
    }
    return new TelescopicBuild(type, stages, finals);
  }

  public final static class TelescopicStage {
    public final ValueAttribute attribute;
    public final @Nullable TelescopicStage next;
    public final String nameBuildStage;
    public final String nameBuildStageSimple;

    TelescopicStage(ValueType type, ValueAttribute attribute, @Nullable TelescopicStage next) {
      this.attribute = attribute;
      this.next = next;
      this.nameBuildStage = toNameOfStage(type, toUpper(attribute.name()) + "BuildStage");
      this.nameBuildStageSimple = nameBuildStage.substring(nameBuildStage.indexOf('.') + 1); // ok
    }
  }

  private static String toUpper(String n) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, n);
  }

  private static String toNameOfStage(ValueType type, String suffix) {
    if (type.constitution.hasTopLevelBuilder()) {
      return type.typeBuilderImpl().simple() + "Stages." + suffix;
    }
    return (type.constitution.isOutsideBuilder() ? toUpper(type.name()) : "") + suffix;
  }
}
