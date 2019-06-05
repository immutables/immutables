package org.immutables.criteria;

import org.reactivestreams.Publisher;

/**
 * Access abstraction to a data-source.
 * @param <T>
 */
public interface Repository<T> {

    Finder<T> find(DocumentCriteria<T> criteria);

    /**
     * Allows to chain operations (like adding {@code offset} / {@code limit}) on some particular query.
     */
    interface Finder<T> {
       Publisher<T> fetch();
    }

}
