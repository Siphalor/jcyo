package de.siphalor.jcyo.core.api;

import de.siphalor.jcyo.core.api.value.JcyoValue;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor
public class JcyoVariables {
	private final Map<String, JcyoValue> variables = new HashMap<>();

	public void set(String name, JcyoValue value) {
		variables.put(name.toUpperCase(Locale.ROOT), value);
	}

	public void setAll(Map<String, JcyoValue> map) {
		map.forEach(this::set);
	}

	public Optional<JcyoValue> get(String name) {
		return Optional.ofNullable(variables.get(name.toUpperCase(Locale.ROOT)));
	}
}
