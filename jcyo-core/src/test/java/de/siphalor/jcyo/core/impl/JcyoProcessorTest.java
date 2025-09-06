package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.api.value.JcyoString;
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
	void processWithoutDirectives() {
		String content = """
				package de.siphalor.coat.util;
				
				public interface TickableElement {
					/**
					 * Called on every render tick.
					 */
					void tick();
				}
				""";
		File input = inputDir.resolve("Test.java").toFile();
		createInputFile(input, content);

		var processor = new JcyoProcessor(
				new JcyoVariables(),
				JcyoOptions.builder().build(),
				inputDir,
				cleanOutputDir
		);

		processor.process(input.toPath());

		assertThat(input).isFile().content().isEqualTo(content);
	}

	@Test
	@SneakyThrows
	void processSimple() {
		JcyoVariables variables = new JcyoVariables();
		variables.set("Test", new JcyoString("blub"));
		variables.set("int_type", new JcyoString("long"));
		JcyoProcessor processor = new JcyoProcessor(
				variables,
				JcyoOptions.builder().updateInput(true).build(),
				inputDir,
				cleanOutputDir
		);

		File input = inputDir.resolve("Test.java").toFile();
		createInputFile(
				input, """
				package de.siphalor.jcyo.test;
				
				import de.siphalor.jcyo.test.something.Utils;
				//- import de.siphalor.jcyo.test.something.Helper;
				
				class Test {
					public void test(/*# if int_type == "long" *//*- long *//*# else */int/*# end */ test) {
						//# if test == "blub"
						//- Helper.test();
						//# else
						Utils.test();
						//# end
					}
				}
				""");

		processor.process(input.toPath());

		assertThat(input).isFile().content().isEqualTo("""
				package de.siphalor.jcyo.test;
			
				//- import de.siphalor.jcyo.test.something.Utils;
				import de.siphalor.jcyo.test.something.Helper;
			
				class Test {
					public void test(/*# if int_type == "long" */long/*# else *//*- int *//*# end */ test) {
						//# if test == "blub"
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
					public void test(long test) {
						Helper.test();
					}
				}
				""");
	}

	@Test
	@SneakyThrows
	void processComments() {
		JcyoProcessor processor = new JcyoProcessor(
				new JcyoVariables(),
				JcyoOptions.builder().updateInput(true).build(),
				inputDir,
				cleanOutputDir
		);

		File input = inputDir.resolve("Test.java").toFile();
		createInputFile(
				input, """
				package de.siphalor.jcyo.test;
				
				class Test {
					public void test() {
						/*# if false */ /* hi
						*/
						// ho
						/// A javadoc
						/** another javadoc */
						/* should stay */
						/*# end */
						//# if false
						// Test
						/// Javadoc
						/* test */ return;
						/**
						 * What nice
						 * multiline
						 * javadoc
						 */
						//hi
						//# end
					}
				}
				""");

		processor.process(input.toPath());

		assertThat(input).isFile().content().isEqualTo("""
				package de.siphalor.jcyo.test;
			
				class Test {
					public void test() {
						/*# if false *//*-  /* hi
						*//*-
						// ho
						/// A javadoc
						/** another javadoc *//*-
						/* should stay *//*-
						 *//*# end */
						//# if false
						//- // Test
						//- /// Javadoc
						//- /* test */ return;
						//- /**
						//-  * What nice
						//-  * multiline
						//-  * javadoc
						//-  */
						//- //hi
						//# end
					}
				}
				""");

		assertThat(cleanOutputDir.resolve("Test.java")).isRegularFile().content().isEqualTo("""
				package de.siphalor.jcyo.test;
			
				class Test {
					public void test() {
				\t\t
					}
				}
				""");
	}

	@Test
	@SneakyThrows
	void processStableLineComments() {
		JcyoProcessor processor = new JcyoProcessor(
				new JcyoVariables(),
				JcyoOptions.builder().updateInput(true).build(),
				inputDir,
				null
		);

		File input = inputDir.resolve("Test.java").toFile();
		createInputFile(
				input, """
				\t//# if false
				\t//- // Hello World!
				\t//- while (true) {
				\t//- \tSystem.out.println("Hello World!");
				\t//- \t// Bye World!
				\t//- }
				\t//# end
				"""
		);

		processor.process(input.toPath());

		assertThat(input).isFile().content().isEqualTo("""
				\t//# if false
				\t//- // Hello World!
				\t//- while (true) {
				\t//- \tSystem.out.println("Hello World!");
				\t//- \t// Bye World!
				\t//- }
				\t//# end
				""");
	}

	@SneakyThrows
	private void createInputFile(File file, String content) {
		try (var writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(content);
		}
	}
}
