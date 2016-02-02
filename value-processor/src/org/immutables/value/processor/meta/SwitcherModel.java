/*
   Copyright 2015 Immutables Authors and Contributors

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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.immutables.generator.Naming;
import org.immutables.generator.Naming.Preference;

public final class SwitcherModel {
  private final String defaultName;
  private final Naming switcherNaming;

  public final ImmutableList<SwitchOption> options;

  private final TypeElement containedTypeElement;

  SwitcherModel(SwitchMirror mirror, String attributeName, TypeElement containedTypeElement) {
    this.switcherNaming = Naming.from(attributeName).requireNonConstant(Preference.SUFFIX);
    this.containedTypeElement = Preconditions.checkNotNull(containedTypeElement);
    this.defaultName = mirror.defaultName();
    this.options = constructOptions();
  }

  private ImmutableList<SwitchOption> constructOptions() {
    ImmutableList.Builder<SwitchOption> builder = ImmutableList.builder();

    for (Element v : containedTypeElement.getEnclosedElements()) {
      if (v.getKind() == ElementKind.ENUM_CONSTANT) {
        String name = v.getSimpleName().toString();
        builder.add(new SwitchOption(name, defaultName.equals(name)));
      }
    }

    return builder.build();
  }

  public boolean hasDefault() {
    return !defaultName.isEmpty();
  }

  public final class SwitchOption {
    public final boolean isDefault;
    public final String constantName;
    public final String switcherName;

    public SwitchOption(String constantName, boolean isDefault) {
      this.constantName = constantName;
      this.switcherName = deriveSwitcherName(constantName);
      this.isDefault = isDefault;
    }

    private String deriveSwitcherName(String constantName) {
      return switcherNaming.apply(
          CaseFormat.UPPER_UNDERSCORE.to(
              CaseFormat.LOWER_CAMEL, constantName));
    }
  }
}
