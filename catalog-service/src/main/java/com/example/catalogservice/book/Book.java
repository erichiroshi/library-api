package com.example.catalogservice.book;

import java.util.HashSet;
import java.util.Set;

import com.example.catalogservice.author.Author;
import com.example.catalogservice.category.Category;
import com.example.catalogservice.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_book", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
public class Book extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, unique = true, length = 20)
	private String isbn;

	private Integer publicationYear;

	private Integer availableCopies;

	@ManyToOne
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;
	
	private String coverImageUrl;

    @ManyToMany
    @JoinTable(
        name = "tb_book_author",
        schema = "catalog",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private final Set<Author> authors = new HashSet<>();
}
