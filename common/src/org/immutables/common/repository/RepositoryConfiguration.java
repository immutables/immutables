package org.immutables.common.repository;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.mongodb.DB;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import static com.google.common.base.Preconditions.*;

@ThreadSafe
public final class RepositoryConfiguration {
  public final ListeningExecutorService executor;
  public final DB database;

  private RepositoryConfiguration(ListeningExecutorService executor, DB database) {
    this.executor = executor;
    this.database = database;
  }

  public static Builder builder() {
    return new Builder();
  }

  @NotThreadSafe
  public static class Builder {
    @Nullable
    private ListeningExecutorService executor;
    @Nullable
    private DB database;

    private Builder() {
    }

    public Builder executor(ListeningExecutorService executor) {
      this.executor = checkNotNull(executor);
      return this;
    }

    public Builder database(DB database) {
      this.database = checkNotNull(database);
      return this;
    }

    public RepositoryConfiguration build() {
      checkState(executor != null);
      checkState(database != null);
      return new RepositoryConfiguration(executor, database);
    }
  }
}
