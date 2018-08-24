package com.example.scanner;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScannerApplication implements CommandLineRunner {
	
	private final String endpointURI = "wss://speech.platform.bing.com/speech/recognition/conversation/cognitiveservices/v1?language=en-US";
	private final Logger logger = LoggerFactory.getLogger(WebsocketClientEndpoint.class);

	public static void main(String[] args) {
		SpringApplication.run(ScannerApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(endpointURI));
		
		clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
            public void handleMessage(String message) {
                logger.info("message: ", message);
            }
        });
		
		
	}
}
