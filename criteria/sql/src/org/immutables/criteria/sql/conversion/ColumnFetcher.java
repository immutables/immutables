package org.immutables.criteria.sql.conversion;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ColumnFetcher<T> {
    T apply(ResultSet row, int index) throws SQLException;
}
