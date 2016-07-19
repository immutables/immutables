package org.immutables.value.processor.encode;

import org.immutables.mirror.Mirror;

interface Mirrors {
	@Mirror.Annotation("org.immutables.encode.Encoding")
	@interface Encoding {}

	@Mirror.Annotation("org.immutables.encode.Encoding.Init")
	@interface Init {}

	@Mirror.Annotation("org.immutables.encode.Encoding.Naming")
	@interface Naming {
		String value();
	}

	@Mirror.Annotation("org.immutables.encode.Encoding.Expose")
	@interface Expose {}

	@Mirror.Annotation("org.immutables.encode.Encoding.Impl")
	@interface Impl {}

	@Mirror.Annotation("org.immutables.encode.Encoding.Stringify")
	@interface Stringify {}

	@Mirror.Annotation("org.immutables.encode.Encoding.Copy")
	@interface Copy {}

	@Mirror.Annotation("org.immutables.encode.Encoding.Builder")
	@interface Builder {}

	@Mirror.Annotation("org.immutables.encode.Encoding.Build")
	@interface Build {}

	@Mirror.Annotation("org.immutables.encode.EncodingMetadata")
	public @interface EncMetadata {
		String name();
		String[] imports();
		String[] typeParams();
		EncElement[] elements();
	}

	@Mirror.Annotation("org.immutables.encode.EncodingMetadata.Element")
	public @interface EncElement {
		String name();
		String type();
		String naming();
		String[] params();
		String[] typeParams();
		String[] tags();
		String code();
	}
}
