package de.siphalor.jcyo.core.impl.token;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum JavaKeyword {
	UNDERSCORE("_"),
	ABSTRACT("abstract"),
	ASSERT("assert"),
	BOOLEAN("boolean"),
	BREAK("break"),
	BYTE("byte"),
	CASE("case"),
	CATCH("catch"),
	CHAR("char"),
	CLASS("class"),
	CONST("const"),
	CONTINUE("continue"),
	DEFAULT("default"),
	DO("do"),
	DOUBLE("double"),
	ELSE("else"),
	ENUM("enum"),
	EXTENDS("extends"),
	EXPORTS("exports"),
	FALSE("false"),
	FINAL("final"),
	FINALLY("finally"),
	FLOAT("float"),
	FOR("for"),
	GOTO("goto"),
	IF("if"),
	IMPLEMENTS("implements"),
	IMPORT("import"),
	INSTANCEOF("instanceof"),
	INT("int"),
	INTERFACE("interface"),
	LONG("long"),
	NATIVE("native"),
	NEW("new"),
	NON_SEALED("non-sealed"),
	NULL("null"),
	OPEN("open"),
	OPENS("opens"),
	MODULE("module"),
	PACKAGE("package"),
	PERMITS("permits"),
	PRIVATE("private"),
	PROTECTED("protected"),
	PROVIDES("provides"),
	PUBLIC("public"),
	RECORD("record"),
	REQUIRES("requires"),
	RETURN("return"),
	SEALED("sealed"),
	SHORT("short"),
	STATIC("static"),
	STRICTFP("strictfp"),
	SUPER("super"),
	SWITCH("switch"),
	SYNCHRONIZED("synchronized"),
	THIS("this"),
	THROW("throw"),
	THROWS("throws"),
	TO("to"),
	TRANSIENT("transient"),
	TRANSITIVE("transitive"),
	TRUE("true"),
	TRY("try"),
	VAR("var"),
	VOID("void"),
	VOLATILE("volatile"),
	WHILE("while"),
	WITH("with"),
	WHEN("when"),
	YIELD("yield"),
	;

	private static final Map<String, JavaKeyword> KEYWORDS = Arrays.stream(JavaKeyword.values())
			.collect(Collectors.toMap(JavaKeyword::text, Function.identity()));

	public static @Nullable JavaKeyword getKeyword(String text) {
		return KEYWORDS.get(text);
	}

	private final String text;
}
