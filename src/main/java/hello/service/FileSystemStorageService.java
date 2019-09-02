package hello.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import hello.config.StorageProperties;
import hello.exception.StorageException;
import hello.model.Word;
import hello.repository.WordRepository;

@Service
public class FileSystemStorageService implements StorageService {

	private final WordRepository wordRepository;

	private final Path rootLocation;

	Logger logger = LoggerFactory.getLogger(StorageService.class);
	@Autowired
	public FileSystemStorageService(WordRepository wordRepository, StorageProperties properties) {
		this.wordRepository = wordRepository;
		this.rootLocation = Paths.get(properties.getLocation());
	}

	@Override
	public void store(MultipartFile file) {
		String filename = StringUtils.cleanPath(file.getOriginalFilename());
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file " + filename);
			}
			if (filename.contains("..")) {
				// This is a security check
				throw new StorageException(
						"Cannot store file with relative path outside current directory " + filename);
			}
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, this.rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
			}
			ExecutorService executorService = Executors.newFixedThreadPool(1);
			executorService.submit(() -> {
				try (BufferedReader br = new BufferedReader(new FileReader(rootLocation.resolve(filename).toFile()))) {
					String st;
					Set<String> set = new HashSet<>();
					while ((st = br.readLine()) != null) {
						st = st.replaceAll("[^a-zA-Z0-9\\s]", "");
						String[] values = st.split(" ");
						for (int j = 0; j < values.length; j++) {
							if (values[j].trim().length() > 0) {
								set.add(values[j].trim());
							}

						}
					}
					set.forEach(item->{
						Word word = new Word();
						word.setValue(item);
						wordRepository.save(word);
					});
					
				} catch (IOException e) {
					throw new StorageException("Failed to store file " + filename, e);
				}

			});
			executorService.shutdown();
		} catch (

		IOException e) {
			throw new StorageException("Failed to store file " + filename, e);
		}
	}

	@Override
	public Boolean find(String value) {
		return !wordRepository.findByValue(value).isEmpty();
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(rootLocation.toFile());
	}

	@Override
	public void init() {
		try {
			Files.createDirectories(rootLocation);
		} catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}

}
