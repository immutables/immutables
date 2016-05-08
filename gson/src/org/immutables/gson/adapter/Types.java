/*
   Copyright 2016 Immutables Authors and Contributors

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
package org.immutables.gson.adapter;

import com.google.gson.internal.$Gson$Types;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * Used in the generated code to subdtitute type variables for arguments in generic types.
 */
public final class Types {
  private Types() {}

  @SuppressWarnings("unchecked") // safe unchecked: could be safe only if used in the manner from generated code
  public static <T> TypeToken<T> tokenFor(Class<?> sampleType, String fieldName, Type[] arguments, String... variables) {
    try {
      Type typeWithVariables = sampleType.getField(fieldName).getGenericType();
      Type typeWithArguments = substituteVariables(typeWithVariables, arguments, variables);
      return (TypeToken<T>) TypeToken.get(typeWithArguments);
    } catch (Exception ex) {
      throw new AssertionError("Cannot get generic type for " + sampleType.getSimpleName() + "." + fieldName, ex);
    }
  }

  public static Type substituteVariables(
      final Type parameterized,
      final Type[] arguments,
      final String[] variables) {

    class Substitution {
      Type process(Type type) {
        if (type instanceof ParameterizedType) {
          ParameterizedType p = (ParameterizedType) type;
          return newParameterized(
              p.getOwnerType(),
              p.getRawType(),
              processAll(p.getActualTypeArguments()));

        } else if (type instanceof GenericArrayType) {
          GenericArrayType g = (GenericArrayType) type;
          return newArray(process(g.getGenericComponentType()));

        } else if (type instanceof WildcardType) {
          WildcardType w = (WildcardType) type;
          return newWildcard(processAll(w.getUpperBounds()), processAll(w.getLowerBounds()));

        } else if (type instanceof TypeVariable<?>) {
          return substitute((TypeVariable<?>) type);
        }
        // Class<?> and any other stuff just pass tru
        return type;
      }

      Type[] processAll(Type[] types) {
        for (int i = 0; i < types.length; i++) {
          types[i] = process(types[i]);
        }
        return types;
      }

      Type substitute(TypeVariable<?> variable) {
        String name = variable.getName();
        int index = 0;
        for (String v : variables) {
          if (name.equals(v)) {
            return arguments[index];
          }
          index++;
        }
        throw new IllegalStateException("type variable " + name + " not substituted in " + parameterized);
      }

    }
    return new Substitution().process(parameterized);
  }

  static ParameterizedType newParameterized(Type ownerType, Type rawType, Type... typeArguments) {
    return $Gson$Types.newParameterizedTypeWithOwner(ownerType, rawType, typeArguments);
  }

  static GenericArrayType newArray(Type componentType) {
    return $Gson$Types.arrayOf(componentType);
  }

  static WildcardType newWildcard(Type[] upperBounds, Type[] lowerBounds) {
    if (lowerBounds.length == 0) {
      return $Gson$Types.supertypeOf(upperBounds[0]);
    }
    return $Gson$Types.subtypeOf(lowerBounds[0]);
  }
}
