package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.api.JcyoProcessingException;
import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.impl.stream.TokenBuffer;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.transform.GeneratedAndDisabledTokenRemover;
import de.siphalor.jcyo.core.impl.transform.JcyoCleaner;
import de.siphalor.jcyo.core.impl.transform.JcyoDirectiveApplier;
import de.siphalor.jcyo.core.impl.transform.UnusedImportDisabler;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;

public class JcyoProcessor {
	private final JcyoOptions options;
	private final Path baseDirectory;
	private final @Nullable Path cleanOutputDirectory;
	private final JcyoDirectiveApplier directiveApplier;
	private final UnusedImportDisabler unusedImportDisabler;

	public JcyoProcessor(
			JcyoVariables variables,
			JcyoOptions options,
			Path baseDirectory,
			@Nullable Path cleanOutputDirectory
	) {
		this.options = options;
		this.baseDirectory = baseDirectory.normalize().toAbsolutePath();
		this.cleanOutputDirectory = cleanOutputDirectory;
		this.directiveApplier = new JcyoDirectiveApplier(variables);
		this.unusedImportDisabler = new UnusedImportDisabler();
	}

	public void process(Path inputFile) throws JcyoProcessingException {
		Path absoluteInput = inputFile.normalize().toAbsolutePath();
		if (!absoluteInput.startsWith(baseDirectory)) {
			throw new IllegalArgumentException("Input files must be inside the base directory: " + baseDirectory);
		}
		File input = absoluteInput.toFile();
		File cleanOutput = getCleanOutputFileForInputPath(absoluteInput);

		processFile(input, cleanOutput);
	}

	private @Nullable File getCleanOutputFileForInputPath(Path inputPath) {
		if (cleanOutputDirectory == null) {
			return null;
		}
		return cleanOutputDirectory.resolve(baseDirectory.relativize(inputPath)).toFile();
	}

	void processFile(File input, @Nullable File cleanOutput) throws JcyoProcessingException {
		TokenStream processedTokenStream = getProcessedTokensStreamForFile(input);

		if (options.updateInput()) {
			if (cleanOutput == null) {
				writeToFile(input, processedTokenStream);
			} else {
				TokenBuffer copy = new TokenBuffer();
				writeToFile(input, copy.copying(processedTokenStream));
				writeToFile(cleanOutput, new JcyoCleaner(copy));
			}
		} else {
			assert cleanOutput != null;
			writeToFile(cleanOutput, processedTokenStream);
		}
	}

	TokenStream getProcessedTokensStreamForFile(File input) throws JcyoProcessingException {
		try (var lexer = new JcyoLexer(new BufferedReader(new FileReader(input)), options)) {

			TokenStream streamWithOldStuffRemoved = new GeneratedAndDisabledTokenRemover(lexer, options);
			TokenStream streamWithDirectivesApplied = directiveApplier.apply(streamWithOldStuffRemoved);
			return unusedImportDisabler.apply(streamWithDirectivesApplied);

		} catch (FileNotFoundException e) {
			throw new JcyoProcessingException("Failed to read input file: " + input, e);
		} catch (JcyoParseException e) {
			throw new JcyoProcessingException("Failed to parse input file: " + input, e);
		} catch (Exception e) {
			throw new JcyoProcessingException("Unexpected exception for input file: " + input, e);
		}
	}

	void writeToFile(File file, TokenStream tokenStream) throws JcyoProcessingException {
		file.getParentFile().mkdirs();
		try (var writer = new TokenWriter(new BufferedWriter(new FileWriter(file)), options)) {
			writer.writeAll(tokenStream);
		} catch (IOException e) {
			throw new JcyoProcessingException("Failed to write to file: " + file, e);
		}
	}
}
