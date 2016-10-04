package org.immutables.value.processor.meta;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;
import com.google.common.collect.Lists;
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
