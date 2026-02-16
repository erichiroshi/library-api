package com.example.library.shared.config.delaycachetest;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("dev")
@Component
public class DevArtificialDelayService implements ArtificialDelayService {

	@Override
	public void delay() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException _) {
			Thread.currentThread().interrupt();
		}
	}
}
