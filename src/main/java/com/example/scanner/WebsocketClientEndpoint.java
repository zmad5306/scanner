package com.example.scanner;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

@ClientEndpoint(configurator=WebsocketClientEndpoint.class)
public class WebsocketClientEndpoint extends ClientEndpointConfig.Configurator {
	
    Session userSession = null;
    private MessageHandler messageHandler;
    private final String authorizationURI = "https://api.cognitive.microsoft.com/sts/v1.0/issueToken";
	private final String authorizationHeaderFormat = "Bearer %s";
	private final Logger logger = LoggerFactory.getLogger(WebsocketClientEndpoint.class);
	private String token;
	private String connectionId;
    
    public WebsocketClientEndpoint() {
    	super();
    }

    public WebsocketClientEndpoint(URI endpointURI) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        System.out.println("opening websocket");
        this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println("closing websocket");
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message) {
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
    }

    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    public static interface MessageHandler {

        public void handleMessage(String message);
    }
    
    private String getToken() {
    	if (null == token) {
    		String subscriptionKey = System.getProperty("subscriptionKey");
    		Assert.notNull(subscriptionKey, "'subscriptionKey' must be provided as system property -DsubscriptionKey=<your key here>");
	    	RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);
			HttpEntity<String> entity = new HttpEntity<String>("", headers);
			ResponseEntity<String> auth = restTemplate.exchange(authorizationURI, HttpMethod.POST, entity, String.class);
			token = auth.getBody();
			logger.info("Access token: ", token);
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
    
    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
    	super.beforeRequest(headers);
		headers.put("Authorization", Arrays.asList(getAuthorizationHeader()));
		headers.put("X-ConnectionId", Arrays.asList(getConnectionId()));
    }

}
