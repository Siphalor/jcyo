package de.siphalor.jcyo.core.impl.directive;

public sealed interface JcyoDirective permits ElseDirective, EndDirective, GeneratedDirective, IfDirective {
	String name();
	boolean isBlockBegin();
	boolean isBlockEnd();
	boolean ends(JcyoDirective blockBegin);
}
