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
