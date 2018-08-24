package com.example.scanner;

import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@SpringBootApplication
public class ScannerApplication implements CommandLineRunner {
	
	private final String authorizationURI = "https://api.cognitive.microsoft.com/sts/v1.0/issueToken";
	private final String authorizationHeaderFormat = "Bearer %s";
	private final String endpointURI = "wss://speech.platform.bing.com/speech/recognition/conversation/cognitiveservices/v1?language=en-US";
	private final Logger logger = LoggerFactory.getLogger(ScannerApplication.class);
	
	private String token;
	private String connectionId;
	
	@Value("${subscriptionKey}")
	private String subscriptionKey;
	
	private String getToken() {
    	if (null == token) {
	    	RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);
			HttpEntity<String> entity = new HttpEntity<String>("", headers);
			ResponseEntity<String> auth = restTemplate.exchange(authorizationURI, HttpMethod.POST, entity, String.class);
			token = auth.getBody();
			logger.info("Access token: {}", token);
    	} 
    	return token;
    }
    
    private String getAuthorizationHeader() {
    	return String.format(authorizationHeaderFormat, getToken());
    }
    
    private String getConnectionId() {
    	if (null == connectionId) {
    		connectionId = UUID.randomUUID().toString();
    	}
    	return connectionId;
    }
    
    private WebSocketHttpHeaders getHeaders() {
    	WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    	headers.put("Authorization", Arrays.asList(getAuthorizationHeader()));
		headers.put("X-ConnectionId", Arrays.asList(getConnectionId()));
    	return headers;
    }
	
	public static void main(String[] args) {
		SpringApplication.run(ScannerApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		WebSocketClient client = new StandardWebSocketClient();
		WebSocketSession session = client.doHandshake(new SpeechWebSocketHandler(), getHeaders(), new URI(endpointURI)).get();
	}
}
