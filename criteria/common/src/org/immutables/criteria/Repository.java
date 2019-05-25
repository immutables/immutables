package org.immutables.criteria;

import org.reactivestreams.Publisher;

/**
 * Access abstraction to a data-source.
 * @param <T>
 */
public interface Repository<T> {

    Publisher<T> query(DocumentCriteria<T> criteria);

}
