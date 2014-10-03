package samplegenerators;

import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import org.immutables.modeling.AbstractTemplate;
import org.immutables.modeling.Generator;

@Generator.Template
@Generator.Import({
    TypeElement.class,
    PackageElement.class
})
public class Doer extends AbstractTemplate {

  public Set<? extends Element> elements() {
    return round().getElementsAnnotatedWith(Doit.class);
  }
}
