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
package org.immutables.value.processor.meta;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

public final class TelescopicBuild {
  public final List<TelescopicStage> stages;
  public final List<ValueAttribute> finals;

  private TelescopicBuild(List<TelescopicStage> initializers, List<ValueAttribute> nonMandatory) {
    this.stages = initializers;
    this.finals = nonMandatory;
  }

  public TelescopicStage firstStage() {
    return stages.get(0);
  }

  static TelescopicBuild from(List<ValueAttribute> attributes) {
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
        next = new TelescopicStage(m, next);
        stages.addFirst(next);
      }
    }
    return new TelescopicBuild(stages, finals);
  }

  public final static class TelescopicStage {
    public final ValueAttribute attribute;
    public final @Nullable TelescopicStage next;

    TelescopicStage(ValueAttribute attribute, @Nullable TelescopicStage next) {
      this.attribute = attribute;
      this.next = next;
    }
  }
}
