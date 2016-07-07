package org.immutables.value.processor.encode;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.processor.encode.Type.Array;
import org.immutables.value.processor.encode.Type.Defined;
import org.immutables.value.processor.encode.Type.Nonprimitive;
import org.immutables.value.processor.encode.Type.Primitive;
import org.immutables.value.processor.encode.Type.Reference;
import org.immutables.value.processor.encode.Type.Variable;
import org.immutables.value.processor.encode.Type.Wildcard;

class DefaultTypeFactory implements Type.Factory {
	private static final Map<String, Type> PREDEFINED_TYPES;
	static {
		ImmutableMap.Builder<String, Type> predefined =
				ImmutableMap.<String, Type>builder().put(Type.OBJECT.name, Type.OBJECT);
		for (Primitive p : Primitive.values()) {
			predefined.put(p.typename, p);
		}
		PREDEFINED_TYPES = predefined.build();
	}
	
	@Override
	public Type named(String name) {
		@Nullable Type type = PREDEFINED_TYPES.get(name);
		if (type != null) return type;
		return defined(name);
	}

	private Type defined(String name) {
		return null;
	}

	@Override
	public Type.Parameterized parameterized(Reference raw, Iterable<? extends Nonprimitive> arguments) {
		return null;
	}

	@Override
	public Array array(Type element) {
		return null;
	}

	@Override
	public Wildcard.Super superWildcard(Defined lowerBound) {
		return null;
	}

	@Override
	public Wildcard.Extends extendsWildcard(Defined upperBound) {
		return null;
	}

	@Override
	public Variable variable(String name) {
		return null; // TODO auto
	}

	@Override
	public Reference unresolved(String name) {
		return null; // TODO auto
	}

}
