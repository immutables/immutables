/*
   Copyright 2014 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value.processor.meta;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import com.google.common.base.Functions;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.immutables.value.processor.encode.Instantiator;
import org.immutables.value.processor.encode.Instantiator.InstantiationCreator;
import org.immutables.value.processor.meta.Proto.Protoclass;

final class RecordComponentCollector {
	private final Protoclass protoclass;
	private final ValueType type;
	private final List<ValueAttribute> attributes = Lists.newArrayList();
	private final Styles styles;
	private final Reporter reporter;

	RecordComponentCollector(Protoclass protoclass, ValueType type) {
		this.protoclass = protoclass;
		this.styles = protoclass.styles();
		this.type = type;
		this.reporter = protoclass.report();
	}

	void collect() {
		TypeElement recordType = (TypeElement) protoclass.sourceElement();

		for (ExecutableElement accessor : recordComponentAssessors(recordType)) {
			TypeMirror returnType = accessor.getReturnType();

			Reporter reporter = report(accessor);

			ValueAttribute attribute = new ValueAttribute();
			attribute.isGenerateAbstract = true;
			attribute.reporter = reporter;
			attribute.returnType = returnType;

			attribute.element = accessor;
			String parameterName = accessor.getSimpleName().toString();
			attribute.names = styles.forAccessorWithRaw(parameterName, parameterName);

			attribute.constantDefault = DefaultAnnotations.extractConstantDefault(
					reporter, accessor, returnType);

			if (attribute.constantDefault != null) {
				attribute.isGenerateDefault = true;
			}

			attribute.containingType = type;
			attributes.add(attribute);
		}

		Instantiator encodingInstantiator = protoclass.encodingInstantiator();

		@Nullable InstantiationCreator instantiationCreator =
				encodingInstantiator.creatorFor(recordType);

		for (ValueAttribute attribute : attributes) {
			attribute.initAndValidate(instantiationCreator);
		}

		if (instantiationCreator != null) {
			type.additionalImports(instantiationCreator.imports);
		}

		type.attributes.addAll(attributes);
	}

	// we reflectively access newer annotation processing APIs by still compiling
	// to an older version.
	private List<ExecutableElement> recordComponentAssessors(TypeElement type) {
		type = CachingElements.getDelegate(type);
		List<ExecutableElement> accessors = new ArrayList<ExecutableElement>();

		try {
			List<?> components = (List<?>) type.getClass()
					.getMethod("getRecordComponents").invoke(type);

			for (Object c : components) {
				ExecutableElement accessor = (ExecutableElement) c.getClass().getMethod("getAccessor").invoke(c);
				accessors.add(accessor);
			}
		} catch (IllegalAccessException
						 | InvocationTargetException
						 | ClassCastException
						 | NoSuchMethodException e) {
			reporter.withElement(type)
					.error("Problem with `TypeElement.getRecordComponents.*.getAccessors`"
							+ " from record type mirror, compiler mismatch.\n"
							+ Throwables.getStackTraceAsString(e));
		}
		return accessors;
	}

	private static ImmutableList<String> extractThrowsClause(ExecutableElement factoryMethodElement) {
		return FluentIterable.from(factoryMethodElement.getThrownTypes())
				.transform(Functions.toStringFunction())
				.toList();
	}

	private Reporter report(Element type) {
		return Reporter.from(protoclass.processing()).withElement(type);
	}
}
