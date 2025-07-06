package org.immutables.datatype;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static java.util.Objects.requireNonNull;

/**
 * Deals with generic types: parameters, arguments, parameterized {@link Type}s.
 */
public final class Types {
  private Types() {}

  public static Method getAccessor(Class<?> type, String name) {
    try {
      // this works only for public methods
      try {
        return type.getMethod(name);
      } catch (NoSuchMethodException noSuchMethod) {}

      for (Class<?> c : collectSupertypes(type)) {
        Method m = collectMatchingDeclared(c, name);
        if (m != null) return m;
      }
      throw new AssertionError("None found " + name);
      //throw new NoSuchMethodException(name);
    } catch (Exception e) {
      throw new AssertionError("There should be an access", e);
    }
  }

  public static Type getGenericAccessorType(Type type, String name) {
    Class<?> hostRawType = toRawType(type);
    Method accessor = getAccessor(hostRawType, name);
    Type returnType = accessor.getGenericReturnType();
    try {
      requireSpecific(returnType);
      // simples case, no need to resolve anything
      return returnType;
    } catch (IllegalArgumentException e) {
      // maybe not efficient to do this on each call, but we don't want to store
      // this map in between calls to getGenericAccessorType, and given that
      // most likely we will not have too many accessors to resolve, this might be ok
      Map<TypeVariable<?>, Type> variables = mapArgumentsInHierarchy(hostRawType);
      return resolveArguments(returnType, variables);
    }
  }

  private static Set<Class<?>> collectSupertypes(Class<?> type) {
    Deque<Class<?>> queue = new ArrayDeque<>();
    queue.add(type);
    Set<Class<?>> processed = new LinkedHashSet<>();

    while (!queue.isEmpty()) {
      Class<?> top = queue.pop();
      if (processed.add(top)) {
        Class<?> superclass = top.getSuperclass();
        if (superclass != null && superclass != Objects.class) {
          queue.add(superclass);
        }
        for (Class<?> implemented : top.getInterfaces()) {
          queue.add(implemented);
        }
      }
    }
    return processed;
  }

  private static Method collectMatchingDeclared(Class<?> type, String name) {
    for (Method m : type.getDeclaredMethods()) {
      if (m.getName().equals(name) && m.getParameterCount() == 0 && !m.isBridge()) {
        return m;
      }
    }
    return null;
  }

  /**
   * Specific types are such types which are either terminal, non-parameterized
   * types, or, if parameterized, requires all type arguments
   * also be specific types recursively.
   * I.e. we ban any type variables and existential types.
   */
  public static Type requireSpecific(Type type) {
    if (type instanceof Class<?>) {
      Class<?> c = (Class<?>) type;
      Type[] parameters = c.getTypeParameters();
      if (parameters.length != 0) throw new IllegalArgumentException(
          "a type cannot contain raw types with its arguments missing,"
              + "use fully instantiated type " + c);
    } else if (type instanceof ParameterizedType) {
      ParameterizedType p = (ParameterizedType) type;
      for (Type a : p.getActualTypeArguments()) {
        requireSpecific(a);
      }
    } else throw new IllegalArgumentException(
        "a type cannot contain non-specific types. But contained " + type);

    return rewrapParameterized(type);
  }

  public static Type[] requireSpecific(Type[] types) {
    types = types.clone();
    for (int i = 0; i < types.length; i++) {
      types[i] = requireSpecific(types[i]);
    }
    return types;
  }

  public static ParameterizedType newParameterized(Class<?> raw, Type... arguments) {
    if (arguments.length != raw.getTypeParameters().length) {
      throw new IllegalArgumentException("Arguments length and parameters mismatch");
    }
    return new AppliedType(raw, rewrapParameterized(arguments));
  }

  private static Type[] rewrapParameterized(Type[] arguments) {
    arguments = arguments.clone();
    for (int i = 0; i < arguments.length; i++) {
      arguments[i] = rewrapParameterized(arguments[i]);
    }
    return arguments;
  }

  /**
   * May be useful in assertions. Implemented as a check if we rewrap(possibly again)
   * we would get exactly the same instance
   */
  public static boolean isRewrapped(Type type) {
    return type == rewrapParameterized(type);
  }

  private static Type rewrapParameterized(Type argument) {
    if (argument instanceof ParameterizedType) {
      ParameterizedType p = (ParameterizedType) argument;
      if (p instanceof AppliedType) return p;
      return new AppliedType(
          (Class<?>) p.getRawType(),
          rewrapParameterized(p.getActualTypeArguments()));
    }
    return argument;
  }

  public static Type[] getArguments(Type type) {
    return type instanceof ParameterizedType ? ((ParameterizedType) type).getActualTypeArguments() : new Type[0];
  }

  public static Type getFirstArgument(Type type) {
    return ((ParameterizedType) type).getActualTypeArguments()[0];
  }

  public static Type getSecondArgument(Type type) {
    return ((ParameterizedType) type).getActualTypeArguments()[1];
  }

  public static Class<?> toRawType(Type type) {
    if (type instanceof Class<?>) return (Class<?>) type;
    if (type instanceof ParameterizedType) return (Class<?>) ((ParameterizedType) type).getRawType();
    if (type instanceof TypeVariable<?>) return toRawType(((TypeVariable<?>) type).getBounds()[0]);
    if (type instanceof WildcardType) return toRawType(((WildcardType) type).getUpperBounds()[0]);
    throw new IllegalArgumentException("No raw type for " + type);
  }

  public static Map<TypeVariable<?>, Type> mapArgumentsInHierarchy(Class<?> raw) {
    Map<TypeVariable<?>, Type> initial = Collections.emptyMap();
    TypeVariable<? extends Class<?>>[] parameters = raw.getTypeParameters();
    if (parameters.length > 0) {
      initial = new LinkedHashMap<>();
      for (TypeVariable<? extends Class<?>> p : parameters) {
        initial.put(p, p);
      }
    }
    return mapArgumentsInHierarchy(raw, initial);
  }

  public static Map<TypeVariable<?>, Type> mapArgumentsInHierarchy(
      Class<?> raw, Map<? extends TypeVariable<?>, ? extends Type> initial) {
    HierarchyTypeVariableMapper resolution = new HierarchyTypeVariableMapper();
    resolution.variables.putAll(initial);
    resolution.collect(raw);
    return resolution.variables;
  }

  public static Map<TypeVariable<?>, Type> mapArguments(Class<?> raw, Type type) {
    if (type instanceof ParameterizedType) {
      ParameterizedType p = (ParameterizedType) type;
      Type[] arguments = p.getActualTypeArguments();
      TypeVariable<?>[] parameters = raw.getTypeParameters();

      assert p.getRawType().equals(raw);
      assert parameters.length == arguments.length;

      Map<TypeVariable<?>, Type> map = new LinkedHashMap<>(4);
      for (int i = 0; i < arguments.length; i++) {
        map.put(parameters[i], arguments[i]);
      }

      return map; // frankly, no need to wrap in immutable map
    } else if (raw.equals(type)) {
      assert raw.getTypeParameters().length == 0;
      return Collections.emptyMap();
    } else throw new IllegalArgumentException("Unsupported type kind: " + type);
  }

  private static final class AppliedType implements ParameterizedType {
    final Class<?> raw;
    final Type[] arguments;

    private AppliedType(Class<?> raw, Type[] arguments) {
      this.raw = requireNonNull(raw);
      this.arguments = requireNonNull(arguments);
    }

    public Type[] getActualTypeArguments() {
      return arguments.clone();
    }

    public Type getRawType() {
      return raw;
    }

    public /*@Null*/ Type getOwnerType() {
      return null;
    }

    public Class<?> raw() {return raw;}

    public Type[] arguments() {return arguments;}

    @Override
    public String toString() {
      String a = Arrays.toString(arguments);// then cut off square brackets
      return raw.getName() + '<' + a.substring(0, a.length() - 1) + '>';
    }

    @Override
    public boolean equals(Object obj) {
      return obj == this || (obj instanceof AppliedType)
          && raw == ((AppliedType) obj).raw
          && Arrays.equals(arguments, ((AppliedType) obj).arguments);
    }

    @Override
    public int hashCode() {
      return Objects.hash((Object[]) arguments) * 31 + raw.hashCode();
    }
  }

  public static Type[] resolveArguments(Type[] types, Map<TypeVariable<?>, Type> variables) {
    types = types.clone();
    for (int i = 0; i < types.length; i++) {
      types[i] = resolveArguments(types[i], variables);
    }
    return types;
  }

  public static Type resolveArguments(Type type, Map<TypeVariable<?>, Type> variables) {
    if (type instanceof Class<?>) return type;

    if (type instanceof ParameterizedType) {
      ParameterizedType p = (ParameterizedType) type;
      // this array is always expected as a clone, never original
      Type[] arguments = p.getActualTypeArguments();
      for (int i = 0; i < arguments.length; i++) {
        arguments[i] = resolveArguments(arguments[i], variables);
      }
      return new AppliedType((Class<?>) p.getRawType(), arguments);
    }

    if (type instanceof TypeVariable<?>) {
      TypeVariable<?> v = (TypeVariable<?>) type;
      /*@Null*/
      Type substitution = variables.get(v);
      if (substitution == null) throw new IllegalArgumentException(
          String.format("Must have all variables substituted! Missing %s occurs in %s " +
              "where existing substitutions: %s", v, type.getTypeName(), variables));

      return substitution;
    }
    throw new IllegalArgumentException("Unsupported type: " + type);
    // TODO not sure if it's a good idea, need to see bigger picture
  }

  // This is limited only to extracting type resolution started with fully specific
  private static final class HierarchyTypeVariableMapper {
    final Map<TypeVariable<?>, Type> variables = new HashMap<>();
    private final Set<Class<?>> seen = new HashSet<>();

    void collect(Class<?> raw) {
      collect(raw, raw);
    }

    void collect(Class<?> raw, Type type) {
      if (raw == Object.class) return;

      if (seen.add(raw)) {
        if (raw != type) {
          Map<TypeVariable<?>, Type> arguments = mapArguments(raw, type);
          variables.putAll(arguments);
        }

        /*@Null*/
        Type superClass = raw.getGenericSuperclass();
        if (superClass != null) {
          collect(toRawType(superClass),
              resolveArguments(superClass, variables));
        }

        for (Type superInterface : raw.getGenericInterfaces()) {
          collect(toRawType(superInterface),
              resolveArguments(superInterface, variables));
        }
      } else if (assertionsEnabled) {
        // if assertions enabled we just recheck that resolved variables
        // table contains the same resolved arguments for the type definition
        for (Map.Entry<TypeVariable<?>, Type> e : mapArguments(raw, type).entrySet()) {
          TypeVariable<?> variable = e.getKey();
          Type argument = e.getValue();
          assert argument.equals(variables.get(variable));
        }
      }
    }
  }

  private static final boolean assertionsEnabled = Types.class.desiredAssertionStatus();
}
