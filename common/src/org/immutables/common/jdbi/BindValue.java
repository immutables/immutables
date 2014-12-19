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
package org.immutables.common.jdbi;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Map.Entry;
import org.immutables.common.marshal.Marshaler;
import org.immutables.common.marshal.internal.MarshalingSupport;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

@Beta
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@BindingAnnotation(BindValue.BindValueFactory.class)
public @interface BindValue {

  String value() default "";

  public static class BindValueFactory implements BinderFactory {

    private static final ObjectCodec CODEC = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_OF_OBJECTS =
        new TypeReference<Map<String, Object>>() {};

    @Override
    public Binder build(final Annotation annotation) {
      return new Binder<BindValue, Object>() {
        @Override
        public void bind(SQLStatement<?> q, BindValue bind, Object arg) {
          Class<?> argumentType = arg.getClass();
          Preconditions.checkArgument(
              MarshalingSupport.hasAssociatedMarshaler(argumentType),
              "Bound value should have marshaler generated using @Json.Marshaled annotation");

          String prefix = prefix(bind);
          try {
            @SuppressWarnings("resource")
            TokenBuffer buffer = new TokenBuffer(CODEC, false);
            Marshaler<Object> marshaler = MarshalingSupport.getMarshalerFor(argumentType);
            marshaler.marshalInstance(buffer, arg);
            Map<String, Object> parameters = buffer.asParser().readValueAs(MAP_OF_OBJECTS);
            bindStatement(q, prefix, parameters);
          } catch (Exception exception) {
            UnableToCreateStatementException statementException = new UnableToCreateStatementException(
                String.format("Could not bind parameter %s as '%s'", arg, bind.value()),
                exception,
                q.getContext());

            MapperFactory.makeStackTraceUseful(statementException, exception);
            throw statementException;
          }
        }

        private void bindStatement(SQLStatement<?> q, String prefix, Map<String, Object> map) {
          for (Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            String name = prefix + entry.getKey();
            if (value == null) {
              q.bind(name, (Object) null);
            } else {
              q.dynamicBind(value.getClass(), name, value);
            }
          }
        }

        private String prefix(BindValue bind) {
          return !bind.value().isEmpty() ? (bind.value() + ".") : "";
        }

        @Override
        public String toString() {
          return BindValueFactory.this.toString() + ".build(" + annotation + ")";
        }
      };
    }

    @Override
    public String toString() {
      return BindValue.class.getSimpleName() + "." + BindValueFactory.class.getSimpleName();
    }
  }
}
