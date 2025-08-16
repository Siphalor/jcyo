package de.siphalor.jcyo.core.api;

import de.siphalor.jcyo.core.impl.JcyoProcessor;
import lombok.Builder;
import lombok.extern.apachecommons.CommonsLog;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CommonsLog
public class Jcyo {
	private final Path baseDirectory;
	private final JcyoProcessor processor;

	@Builder
	private Jcyo(JcyoVariables variables, JcyoOptions options, Path baseDirectory, @Nullable Path cleanOutputDirectory) {
		this.baseDirectory = baseDirectory;
		this.processor = new JcyoProcessor(variables, options, baseDirectory, cleanOutputDirectory);
	}

	public void process(Path inputFile) throws JcyoProcessingException {
		processor.process(inputFile);
	}

	public void processAll() throws JcyoProcessingException {
		try {
			Files.walk(baseDirectory)
					.filter(path -> {
						var fileName = path.getFileName().toString();
						return fileName.endsWith(".java") && !fileName.contains("-");
					})
					.forEach(path -> {
						try {
							processor.process(path);
						} catch (JcyoProcessingException e) {
							log.error("Failed to process file: " + path, e);
						}
					});
		} catch (IOException e) {
			throw new JcyoProcessingException("Failed to walk input directory: " + baseDirectory, e);
		}
	}
}
