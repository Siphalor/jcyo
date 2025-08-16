package de.siphalor.jcyo.core.impl;

public interface JavaImport {
	String[] path();
	default String importedName() {
		return path()[path().length - 1];
	}
	boolean staticImport();
}
