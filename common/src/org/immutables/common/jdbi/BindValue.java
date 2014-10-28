package org.immutables.common.jdbi;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@BindingAnnotation(BindValue.BindValueFactory.class)
public @interface BindValue {

  String value() default "";

  public static class BindValueFactory implements BinderFactory {

    @Override
    public Binder build(Annotation annotation) {
      return new Binder<BindValue, Object>() {
        @Override
        public void bind(SQLStatement<?> q, BindValue bind, Object arg) {
          // q.dynamicBind(readMethod.getReturnType(), prefix + prop.getName(),
// readMethod.invoke(arg));
        }
      };
    }
  }
}
