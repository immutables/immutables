package org.immutables.generate.silly;

import com.google.common.collect.Range;
import java.util.regex.Pattern;
import org.immutables.common.repository.internal.ConstraintSupport.ConstraintHost;
import org.immutables.common.repository.internal.RepositorySupport;
import org.immutables.generate.silly.repository.SillyStructureWithIdRepository;
import org.immutables.generate.silly.repository.SillyStructureWithIdRepository.Criteria;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class CriteriaBuilderTest {

  @Test
  public void ranges() {
    check(stringify(SillyStructureWithIdRepository.where().attr1AtLeast("1")))
        .is("{ 'attr1' : { '$gte' : '1'}}");

    check(stringify(where().attr1LessThan("ZZZ")))
        .is("{ 'attr1' : { '$lt' : 'ZZZ'}}");

    check(stringify(where().idAtLeast("5")))
        .is("{ '_id' : { '$gte' : '5'}}");

    check(stringify(where().idGreaterThan("0")))
        .is("{ '_id' : { '$gt' : '0'}}");

    check(stringify(where().idIn(Range.singleton("1"))))
        .is("{ '_id' : '1'}");

    check(stringify(where().idNotIn(Range.singleton("4"))))
        .is("{ '_id' : { '$ne' : '4'}}");

    check(stringify(where().idNotIn(Range.closedOpen("1", "3"))))
        .is("{ '_id' : { '$not' : { '$gte' : '1' , '$lt' : '3'}}}");

    check(stringify(where().idNotIn(Range.lessThan("0"))))
        .is("{ '_id' : { '$gte' : '0'}}");

    check(stringify(where().idNotIn(Range.atLeast("3"))))
        .is("{ '_id' : { '$lt' : '3'}}");
  }

  @Test
  public void equality() {
    check(stringify(where().id("1"))).is("{ '_id' : '1'}");
    check(stringify(where().idNot("2"))).is("{ '_id' : { '$ne' : '2'}}");
    check(stringify(where().int9NotIn(3, 4, 5))).is("{ 'int9' : { '$nin' : [ 3 , 4 , 5]}}");
    check(stringify(where().idMatches(Pattern.compile("^2")))).is("{ '_id' : { '$regex' : '^2'}}");
    check(stringify(where().idStartsWith("4"))).is("{ '_id' : { '$regex' : '^4'}}");
  }

  private Criteria where() {
    return SillyStructureWithIdRepository.where();
  }

  private String stringify(Criteria criteria) {
    RepositorySupport.ConstraintBuilder builder =
        ((ConstraintHost) criteria).accept(new RepositorySupport.ConstraintBuilder(""));
    return builder.asDbObject().toString().replace('"', '\'');
  }
}
