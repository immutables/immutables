package nonimmutables.recurs;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;

@JsonSerialize
@Immutable
public interface AbstractSomething extends IHaveGetContentAndWithId<MySomethingContent, Something> {

  // this override is not needed with 2.2, hooray! \o/
  // @Override
  // MySomethingContent getContent();

  // without this override it generates the withId method that returns T instead of Something for
  // the Json class ;(
  // @Override
  // Something withId(int id);
}
