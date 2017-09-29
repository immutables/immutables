package org.immutables.mongo.fixture;

import com.google.common.base.Preconditions;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteError;

import java.util.List;

import static org.immutables.check.Checkers.check;
import static org.junit.Assert.fail;

/**
 * Collection of simple validation methods
 */
final class MongoAsserts {

  private MongoAsserts() {}

  /**
   * Ensures current exception has been generated due to a duplicate (primary) key.
   * Differentiates between Fongo and Mongo exceptions since the behaviour under these databases
   * is different.
   */
  public static void assertDuplicateKeyException(Throwable exception) {
    Preconditions.checkNotNull(exception, "exception");

    // unwrap, if necessary
    exception = exception instanceof MongoException ? exception : exception.getCause();

    // fongo throws directly DuplicateKeyException
    if (exception instanceof DuplicateKeyException) return;

    // MongoDB throws custom exception
    if (exception instanceof MongoCommandException) {
      String codeName = ((MongoCommandException) exception).getResponse().get("codeName").asString().getValue();
      int errorCode = ((MongoCommandException) exception).getErrorCode();

      check(codeName).is("DuplicateKey");
      check(errorCode).is(11000);

      // all good here (can return)
      return;
    }

    // for bulk writes as well
    if (exception instanceof MongoBulkWriteException) {
      List<BulkWriteError> errors = ((MongoBulkWriteException) exception).getWriteErrors();
      check(errors).hasSize(1);
      check(errors.get(0).getCode()).is(11000);
      check(errors.get(0).getMessage()).contains("duplicate key");
      return;
    }

    // all others exceptions
    fail("Should get duplicate key exception after " + exception);
  }


}
