package org.immutables.criteria.sql.compiler;

import org.immutables.criteria.backend.JavaBeanNaming;
import org.immutables.criteria.expression.Path;

public class SQLPathNaming extends JavaBeanNaming {
    @Override
    public String name(Path path) {
        String name = super.name(path);
        return "`" + name.replaceAll("\\.", "`.`") + "`";
    }
}
