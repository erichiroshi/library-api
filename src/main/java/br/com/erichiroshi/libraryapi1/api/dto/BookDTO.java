package br.com.erichiroshi.libraryapi1.api.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {

	private Long id;
	
	@NotEmpty
	private String author;
	
	@NotEmpty
	private String title;
	
	@NotEmpty
	private String isbn;

}
