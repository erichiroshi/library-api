package com.example.library.author;

import java.util.Collection;
import java.util.Set;

public interface AuthorPort {
	Set<Author> findAllById(Collection<Long> ids);

	int countByIds(Collection<Long> ids);
}