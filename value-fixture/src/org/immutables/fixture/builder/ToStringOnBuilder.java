package org.immutables.fixture.builder;

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

@Value.Style(builderToString = true)
@Value.Immutable
public interface ToStringOnBuilder {
	String a();
	int attr();
	@Nullable Void voids();
	@Nullable Integer limit();
	String[] arr();
	@Nullable int[] arrg();
	boolean[] arrb();
	Map<String, Integer> map();
	Multimap<String, Object> multimap();
	List<Boolean> list();
	Set<String> set();
	Optional<String> optional();
	Optional<Object> opject();
	Optional<RetentionPolicy> oret();
	OptionalInt optionalInt();
	OptionalDouble optionalDouble();
	@Nullable List<@SkipNulls String> nullist();
}
