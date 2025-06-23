package org.immutables.fixture.j17;

import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.collect.Multimap;
import org.immutables.fixture.nullable.SkipNulls;
import org.immutables.value.Value;

@Value.Builder
public record RecordWitherTwo(
		int attr,
		@Nullable Void voids,
		@Nullable Integer limit,
		float floaty,
		double doubly,
		Double upperDoubly,
		String justString,
		RetentionPolicy policy,
		String[] arr,
		@Nullable int[] arrg,
		Map<String, Integer> map,
		Multimap<String, Object> multimap,
		List<Boolean> list,
		Set<String> set,
		Optional<String> optional,
		Optional<Object> optject,
		Optional<RetentionPolicy> oret,
		OptionalInt optionalInt,
		OptionalDouble optionalDouble,
		com.google.common.base.Optional<Integer> guoptional,
		com.google.common.base.Optional<RetentionPolicy> guret,
		com.google.common.base.Optional<Object> gubject,
		@Nullable List<@SkipNulls String> nullist
) implements WithRecordWitherTwo {}
