package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.api.JcyoProcessingException;
import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.impl.stream.TokenBuffer;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.transform.*;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

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
		InputStream inputStream = null;
		try {
			MessageDigest digest = null;
			inputStream = new FileInputStream(input);
			if (options.updateInput()) {
				digest = MessageDigest.getInstance("SHA-1");
				inputStream = new DigestInputStream(inputStream, digest);
			}

			TokenStream processedTokenStream = getProcessedTokensStreamForFile(
					new InputStreamReader(inputStream, StandardCharsets.UTF_8)
			);

			if (options.updateInput()) {
				if (cleanOutput == null) {
					updateFileOnChanged(input, digest, processedTokenStream);
				} else {
					TokenBuffer copy = new TokenBuffer();
					updateFileOnChanged(input, digest, copy.copying(processedTokenStream));
					writeToFile(cleanOutput, new JcyoCleaner(copy));
				}
			} else {
				assert cleanOutput != null;
				writeToFile(cleanOutput, processedTokenStream);
			}
		} catch (IOException e) {
			throw new JcyoProcessingException("Failed to read input file: " + input, e);
		} catch (NoSuchAlgorithmException e) {
			throw new JcyoProcessingException("Failed to initialize SHA-1 digest", e);
		} finally {
			assert inputStream != null;
			try {
				inputStream.close();
			} catch (IOException ignored) {}
		}
	}

	TokenStream getProcessedTokensStreamForFile(Reader input) throws JcyoProcessingException {
		try (var lexer = new JcyoLexer(input, options)) {

			TokenStream streamWithOldStuffRemoved = new GeneratedAndDisabledTokenRemover(
					new JcyoUnpadder(lexer),
					options
			);
			TokenStream streamWithDirectivesApplied = directiveApplier.apply(streamWithOldStuffRemoved);
			return unusedImportDisabler.apply(streamWithDirectivesApplied);

		} catch (JcyoParseException e) {
			throw new JcyoProcessingException("Failed to parse input file: " + input, e);
		} catch (Exception e) {
			throw new JcyoProcessingException("Unexpected exception for input file: " + input, e);
		}
	}

	void updateFileOnChanged(File file, MessageDigest oldDigest, TokenStream tokenStream) throws JcyoProcessingException {
		File tempFile = createTempFileForOutput(file);

		MessageDigest newDigest;
		try {
			newDigest = MessageDigest.getInstance(oldDigest.getAlgorithm());
		} catch (NoSuchAlgorithmException e) {
			throw new JcyoProcessingException("Missing digest algorithm: " + oldDigest.getAlgorithm(), e);
		}

		try (var outputStream = new DigestOutputStream(new FileOutputStream(tempFile), newDigest)) {
			try (var writer = new TokenWriter(
					new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)),
					options
			)) {
				writer.writeAll(tokenStream);
			}
		} catch (IOException e) {
			throw new JcyoProcessingException("Failed to write to temporary output file: " + tempFile, e);
		}

		if (!Arrays.equals(newDigest.digest(), oldDigest.digest())) {
			if (!file.delete()) {
				throw new JcyoProcessingException("Failed to delete file: " + file);
			}
			try {
				Files.move(tempFile.toPath(), file.toPath());
			} catch (IOException e) {
				throw new JcyoProcessingException(
						"Failed to move temporary output file: " + tempFile + " to: " + file, e
				);
			}
		} else {
			tempFile.deleteOnExit();
		}
	}

	private File createTempFileForOutput(File file) throws JcyoProcessingException {
		try {
			return Files.createTempFile("jcyo-processor", file.getName()).toFile();
		} catch (IOException e) {
			throw new JcyoProcessingException("Failed to create temporary file", e);
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
