package com.example.library.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.library.category.application.CategoryService;
import com.example.library.category.application.dto.CategoryResponseDTO;
import com.example.library.category.application.dto.PageResponseDTO;
import com.example.library.category.exception.CategoryNotFoundException;
import com.example.library.category.web.CategoryController;
import com.example.library.security.filter.JwtAuthenticationFilter;

@WebMvcTest(
	    controllers = CategoryController.class,
	    excludeFilters = @ComponentScan.Filter(
	        type = FilterType.ASSIGNABLE_TYPE,
	        classes = JwtAuthenticationFilter.class
	    )
	)
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService service;
    
    @MockitoBean
    private CacheManager cacheManager;
    
    @Test
	void shouldReturnOk() throws Exception {
		PageResponseDTO<CategoryResponseDTO> response = new PageResponseDTO<>(List.of(), 0, 10, 0, 0);

		when(service.findAll(any(Pageable.class))).thenReturn(response);


        mockMvc.perform(get("/api/categories"))
               .andExpect(status().isOk());
    }
    
    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        when(service.findById(1L))
                .thenThrow(new CategoryNotFoundException(1L));

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isNotFound());
    }

}
