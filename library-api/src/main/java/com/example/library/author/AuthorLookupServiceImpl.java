package com.example.library.author;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthorLookupServiceImpl implements AuthorLookupService {

	private final AuthorRepository repository;

	@Override
	@Transactional(readOnly = true)
	public Set<Author> findAllById(Collection<Long> ids) {
		return new HashSet<>(repository.findAllById(ids));
	}

	@Override
	@Transactional(readOnly = true)
	public int countByIds(Collection<Long> ids) {
		return repository.findAllById(ids).size();
	}
}