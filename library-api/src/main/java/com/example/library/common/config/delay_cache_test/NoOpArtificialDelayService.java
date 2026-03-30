package com.example.library.common.config.delay_cache_test;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!dev")
@Component
public class NoOpArtificialDelayService implements ArtificialDelayService {

	@Override
	public void delay() {
		// não faz nada
	}
}
