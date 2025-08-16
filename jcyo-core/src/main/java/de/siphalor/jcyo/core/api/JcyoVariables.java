package de.siphalor.jcyo.core.api;

import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class JcyoVariables {
	private final Map<String, String> variables = new HashMap<>();

	public void set(String name, String value) {
		variables.put(name, value);
	}

	public void setAll(Map<String, String> map) {
		variables.putAll(map);
	}

	public @Nullable String get(String name) {
		return variables.get(name);
	}
}
