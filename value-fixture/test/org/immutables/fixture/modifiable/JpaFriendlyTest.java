package org.immutables.fixture.modifiable;

import static org.immutables.check.Checkers.check;
import static org.immutables.matcher.ModifierMatcher.notFinalModifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.immutables.value.Value.Style;
import org.junit.jupiter.api.Test;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Test Modifiables class when {@link Style#jpaFriendlyModifiables} style is activated.
 */
public class JpaFriendlyTest {
  /**
   * Modifables class with {@link Style#jpaFriendlyModifiables} should be jpa compliante
   * 
   * @throws Exception If somethong goes wrong during when scanning the class
   */
  @Test
  public void modifiableAsJpaEntity() throws Exception {
    ImmutableSet<String> rwProperties = 
        ImmutableSet.of("primary", "id", "description", "names", "scopes", "options", "extra", "targets");

    FluentIterable<Field> fields = FluentIterable.of(ModifiableJpaFriendly.class.getDeclaredFields());

    check(fields.transform(Field::getName).toSet().containsAll(rwProperties));

    for (Field field : fields) {
      if (rwProperties.contains(field.getName())) {
        check(field).is(notFinalModifier());
        check(getReadMethod(field)).is(notFinalModifier());
        check(getWriteMethod(field)).notNull();
      }
    }

    ModifiableJpaFriendly entity = new ModifiableJpaFriendly();
    entity.setPrimary(true);
    entity.setDescription("description");
    entity.setId(1000);
    entity.setNames(ImmutableList.of("name"));
    entity.addScopes("profiles");
    entity.putOptions("foo", "bar");
    entity.setTargets(ImmutableSet.of("target1", "target2"));

    // This bean can become immutable.
    JpaFriendly immutableBean = entity.toImmutable();
    check(immutableBean.isPrimary());
    check(immutableBean.getDescription()).is("description");
    check(immutableBean.getId()).is(1000);
    check(immutableBean.getNames()).isOf("name");
    check(immutableBean.getScopes()).isOf("profiles");
    check(immutableBean.getOptions()).is(ImmutableMap.of("foo", "bar"));
    check(immutableBean.getTargets()).isOf("target1", "target2");
  }

  /**
   * Get field's corresponding getter method.
   * 
   * @param field The searched field
   * @return The corresponding getter method
   */
  private static Method getReadMethod(Field field) {
    Method searchedMethod = null;
    if (field.getType().equals(boolean.class)) {
      searchedMethod = getMethodQuietly(ModifiableJpaFriendly.class, booleanGetterMethodName(field));
    }
    if (searchedMethod == null) {
      searchedMethod = getMethodQuietly(ModifiableJpaFriendly.class, getterMethodName(field));
    }
    return searchedMethod;
  }

  /**
   * Build getter method name for boolean.
   * 
   * @param field The searched field
   * @return The getter method name
   */
  private static String booleanGetterMethodName(Field field) {
    return "is" + capitalize(field.getName());
  }

  /**
   * Build getter method name.
   * 
   * @param field The searched field
   * @return The getter method name
   */
  private static String getterMethodName(Field field) {
    return "get" + capitalize(field.getName());
  }

  /**
   * Get field's corresponding setter method.
   * 
   * @param field The searched field 
   * @return The corresponding setter method
   */
  private static Method getWriteMethod(Field field) {
    return getMethodQuietly(ModifiableJpaFriendly.class, setterMethodName(field), field.getType());
  }

  /**
   * Build setter method name
   * 
   * @param field The searched field
   * @return The setter method name
   */
  private static String setterMethodName(Field field) {
    return "set" + capitalize(field.getName());
  }

  /**
   * Capitalize a string
   * @param s A string
   * @return The string capitalized
   */
  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    } else {
      return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
  }

  /**
   * Get method in a clazz ignoning any problem
   * @param clazz Class of the searching method
   * @param name Method's name
   * @param parameterTypes Types of parameter's method
   * @return The method
   */
  private static Method getMethodQuietly(Class<?> clazz, String name, Class<?>... parameterTypes) {
    try {
      return clazz.getMethod(name, parameterTypes);
    } catch (Exception e) {
      return null;
    }
  }
}
