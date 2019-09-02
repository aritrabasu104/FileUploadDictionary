package hello.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    Boolean find(String value);

    void store(MultipartFile file);

	void deleteAll();

	void init();

}
