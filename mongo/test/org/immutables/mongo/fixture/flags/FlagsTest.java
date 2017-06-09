package org.immutables.mongo.fixture.flags;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.immutables.mongo.fixture.MongoContext;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;

import static org.immutables.check.Checkers.check;

/**
 * Ensures read / write / index methods are present / absent from generated repositories after some flags have been
 * set using {@link org.immutables.mongo.Mongo.Repository} annotation.
 */
public class FlagsTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  @Test
  public void standard() throws Exception {
    StandardRepository repo = new StandardRepository(context.setup());

    repo.index().withName().ensure().getUnchecked();

    repo.findAll().deleteAll().getUnchecked();
    repo.findAll().deleteFirst().getUnchecked();
    repo.findAll().andModifyFirst().setName("foo").update();
    repo.findAll().andReplaceFirst(ImmutableStandard.builder().id("id1").build()).upsert().getUnchecked();

  }

  @Test
  public void readonly() throws Exception {
    ReadonlyRepository repo = new ReadonlyRepository(context.setup());

    // index method should be present. Make it compile time check
    repo.index().withName().ensure().getUnchecked();

    // ensure write methods are missing
    check(methodsByName(ReadonlyRepository.class, "andModifyFirst")).isEmpty();
    check(methodsByName(ReadonlyRepository.class, "andReplaceFirst")).isEmpty();
    check(methodsByName(ReadonlyRepository.class, "update")).isEmpty();
    check(methodsByName(ReadonlyRepository.class, "upsert")).isEmpty();
    check(methodsByName(ReadonlyRepository.class, "insert")).isEmpty();

    check(methodsByName(ReadonlyRepository.Finder.class, "deleteFirst")).isEmpty();
    check(methodsByName(ReadonlyRepository.Finder.class, "deleteAll")).isEmpty();
    check(methodsByName(ReadonlyRepository.Finder.class, "delete")).isEmpty();
    check(methodsByName(ReadonlyRepository.Finder.class, "andModifyFirst")).isEmpty();
    check(methodsByName(ReadonlyRepository.Finder.class, "andReplaceFirst")).isEmpty();
  }

  @Test
  public void noIndex() throws Exception {
    NoIndexRepository repo = new NoIndexRepository(context.setup());

    repo.find(repo.criteria().id("foo")).fetchAll().getUnchecked();
    check(methodsByName(NoIndexRepository.class, "index")).isEmpty();
  }

  /**
   * Gets all (public) methods by name using reflection
   */
  private static Set<Method> methodsByName(Class<?> type, final String name) {
    return FluentIterable
            .from(Arrays.asList(type.getDeclaredMethods()))
            .append(Arrays.asList(type.getMethods()))
            .filter(new Predicate<Method>() {
              @Override
              public boolean apply(Method method) {
                return Modifier.isPublic(method.getModifiers()) && method.getName().equals(name);
              }
            })
            .toSet();
  }

}
