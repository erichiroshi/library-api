package com.example.library.api.controllers;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.library.domain.exceptions.ResourceNotFoundException;
import com.example.library.domain.services.CategoryService;
import com.example.library.security.JwtAuthenticationFilter;

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
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/categories"))
               .andExpect(status().isOk());
    }
    
    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        when(service.findById(1L))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isNotFound());
    }

}
