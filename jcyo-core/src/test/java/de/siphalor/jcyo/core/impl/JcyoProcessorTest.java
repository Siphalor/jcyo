package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.api.value.JcyoBoolean;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JcyoProcessorTest {
	@TempDir
	Path inputDir;

	@TempDir
	Path cleanOutputDir;

	@Test
	@SneakyThrows
	void processSimple() {
		JcyoVariables variables = new JcyoVariables();
		variables.set("Test", new JcyoBoolean(false));
		JcyoProcessor processor = new JcyoProcessor(
				variables,
				JcyoOptions.builder().updateInput(true).build(),
				inputDir,
				cleanOutputDir
		);

		File input = inputDir.resolve("Test.java").toFile();
		try (var writer = new BufferedWriter(new FileWriter(input))) {
			writer.write("""
					package de.siphalor.jcyo.test;
					
					import de.siphalor.jcyo.test.something.Utils;
					//- import de.siphalor.jcyo.test.something.Helper;
					
					class Test {
						public void test() {
							//# if !test
							//- Helper.test();
							//# else
							Utils.test();
							//# end
						}
					}
					""");
		}

		processor.process(input.toPath());

		assertThat(input).isFile().content().isEqualTo("""
				package de.siphalor.jcyo.test;
			
				//- import de.siphalor.jcyo.test.something.Utils;
				import de.siphalor.jcyo.test.something.Helper;
			
				class Test {
					public void test() {
						//# if !test
						Helper.test();
						//# else
						//- Utils.test();
						//# end
					}
				}
				""");

		assertThat(cleanOutputDir.resolve("Test.java")).isRegularFile().content().isEqualTo("""
				package de.siphalor.jcyo.test;
			
				import de.siphalor.jcyo.test.something.Helper;
			
				class Test {
					public void test() {
						Helper.test();
					}
				}
				""");
	}
}
