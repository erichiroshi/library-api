package br.com.erichiroshi.libraryapi1.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {

	private Long id;
	private String author;
	private String title;
	private String isbn;
}
