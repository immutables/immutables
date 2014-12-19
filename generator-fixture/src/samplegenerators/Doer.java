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
package samplegenerators;

import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

@Generator.Template
@Generator.Import({
    TypeElement.class,
    PackageElement.class
})
public class Doer extends AbstractTemplate {

  public Set<? extends Element> elements() {
    return round().getElementsAnnotatedWith(Doit.class);
  }

  public List<Integer> range() {
    return ImmutableList.of(1, 2, 3, 4, 5);
  }
}
