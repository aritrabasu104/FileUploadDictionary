package hello.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import hello.model.Word;

public interface WordRepository extends CrudRepository<Word, Long> {
	List<Word> findByValue(String value);
}
