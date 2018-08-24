package com.example.scanner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
	
	
//	path:audio
//	x-timestamp:2018-08-24T18:23:25.664Z
//	content-type:audio/x-wav
//	x-requestid:0a26bf38e33e4f508fb8f9fbcff5a424
	
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
    
    private String buildNowHeader() {
    	TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());
		return nowAsISO;
    }
    
	private BinaryMessage createMessage(byte[] buffer) {
	  /* Headers:
	      [{'path':'audio'},
	      {'x-timestamp':Date.UTC(new Date().toISOString())},
	      {'content-type': 'audio/x-wav'},
	      {'x-requestid': connectionId}];
	  */
		String headers = String.format(chunkHeader, buildNowHeader(), getMessageId());
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
		
		InputStream in = Files.newInputStream(new File("C:\\Users\\Zach\\Desktop\\Repos\\scanner-convert\\samples\\broadcastify01.wav").toPath());
		byte[] buffer = new byte[8192];
		int len = 0;
		while((len = in.read(buffer)) != -1) {
			logger.trace("read {} bytes", len);
			BinaryMessage message = createMessage(buffer);
			session.sendMessage(message);
		}
	}


}
