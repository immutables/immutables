package org.immutables.criteria;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Criteria
public interface Friend {

  String nickName();

}
