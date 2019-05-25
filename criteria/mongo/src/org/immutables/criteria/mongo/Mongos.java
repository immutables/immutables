package org.immutables.criteria.mongo;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.immutables.criteria.expression.Expression;

/**
 * Util methods for mongo adapter.
 */
final class Mongos {

  private Mongos() {}

  /**
   * Convert existing expression to Bson
   */
  static <T> Bson toBson(CodecRegistry registry, Expression expression) {
    MongoVisitor visitor = new MongoVisitor(registry);
    return expression.accept(visitor).asDocument();
  }

}
