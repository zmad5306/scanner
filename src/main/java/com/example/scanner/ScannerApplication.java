package com.example.scanner;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
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
import org.springframework.web.socket.BinaryMessage;
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
	private final String chunkHeader = 
			"Path:audio\r\n" + 
			"X-Timestamp:%s\r\n" + 
			"Content-Type:audio/x-wav\r\n" + 
			"X-RequestId:%s\r\n";
	
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
    
    private String getMessageId() {
    	return UUID.randomUUID().toString().replace("-", "");
    }
    
    private WebSocketHttpHeaders getHeaders() {
    	WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    	headers.put("Authorization", Arrays.asList(getAuthorizationHeader()));
		headers.put("X-ConnectionId", Arrays.asList(getConnectionId()));
    	return headers;
    }
    
	private BinaryMessage createMessage(byte[] buffer) {
		String headers = String.format(chunkHeader, SpeechUtils.now(), getMessageId());
		Short hdrLength = Integer.valueOf(headers.length()).shortValue();
		
		byte[] headerLength = ByteBuffer.allocate(2).putShort(hdrLength).array();

		byte[] data = ArrayUtils.addAll(headerLength, headers.getBytes(Charset.forName("utf8")));
		data = ArrayUtils.addAll(data, buffer);
		return new BinaryMessage(data);
	}
	
	public static void main(String[] args) {
		SpringApplication.run(ScannerApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		WebSocketClient client = new StandardWebSocketClient();
		WebSocketSession session = client.doHandshake(new SpeechWebSocketHandler(), getHeaders(), new URI(endpointURI)).get(30, TimeUnit.SECONDS);
		
		InputStream in = Files.newInputStream(new File("C:\\Users\\Zach\\Desktop\\Repos\\scanner-convert\\samples\\future-of-flying.wav").toPath());
		byte[] audio = IOUtils.toByteArray(in);
		
		int chunk = 8192;
		for(int i=0;i<audio.length;i+=chunk){
			BinaryMessage message = createMessage(
					Arrays.copyOfRange(audio, i, Math.min(audio.length,i+chunk))
				);
			session.sendMessage(message);
		} 
		
	}

}
