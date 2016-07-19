package org.immutables.encode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface EncodingMetadata {
	String name();
	String[] imports();
	String[] typeParams();
	Element[] elements();

	@Target({})
	@Retention(RetentionPolicy.CLASS)
	public @interface Element {
		String name();
		String type();
		String naming();
		String[] params();
		String[] typeParams();
		String[] tags();
		String code();
	}
}
