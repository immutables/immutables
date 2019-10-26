package org.immutables.criteria.sql;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.DefaultResult;
import org.immutables.criteria.backend.StandardOperations.Delete;
import org.immutables.criteria.backend.StandardOperations.Insert;
import org.immutables.criteria.backend.StandardOperations.Select;
import org.immutables.criteria.backend.StandardOperations.Update;
import org.immutables.criteria.backend.StandardOperations.UpdateByQuery;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public final class SqlBackend implements Backend {

  @Override
  public Session open(Class<?> entityType) {
    return new Session(entityType);
  }

  private static class Session implements Backend.Session {

    private final Class<?> type;

    Session(Class<?> type) {
      this.type = type;
    }

    @Override
    public Class<?> entityType() {
      return type;
    }

    @Override
    public Result execute(Operation operation) {
      Publisher<?> publisher = new Publisher<Object>() {
        @Override
        public void subscribe(Subscriber<? super Object> s) {
          System.err.println("not subscribed");
        }
      };
      if (operation instanceof Insert) {
        Insert insert = (Insert) operation;

      } else if (operation instanceof Delete) {
        Delete delete = (Delete) operation;

      } else if (operation instanceof Select) {
        Select select = (Select) operation;
        System.out.println(select);

      } else if (operation instanceof Update) {
        Update update = (Update) operation;

      } else if (operation instanceof UpdateByQuery) {
        UpdateByQuery update = (UpdateByQuery) operation;

      } else {
        throw new UnsupportedOperationException();
      }
      return DefaultResult.of(publisher);
    }
  }
}
