package samplegenerators;

import java.util.Set;
import javax.lang.model.element.Element;
import org.immutables.modeling.AbstractTemplate;
import org.immutables.modeling.Generator;

@Generator.Template
public class Doer extends AbstractTemplate {

  public Set<? extends Element> elements() {
    return round().getElementsAnnotatedWith(Doit.class);
  }
}
