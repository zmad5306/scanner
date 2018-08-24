package com.example.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

public class SpeechWebSocketHandler implements WebSocketHandler {
	
	private final Logger logger = LoggerFactory.getLogger(SpeechWebSocketHandler.class);

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		logger.info("connection established, session id: {}", session.getId());
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		logger.info("message recieved, session id: {}, length: {}, payload: {}", session.getId(), message.getPayloadLength(), message.getPayload());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		logger.error("transport error {} on session {}", exception.getMessage(), session.getId());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		logger.info("connection closed on session {} with status {} {}", session.getId(), closeStatus.getCode(), closeStatus.getReason());
	}

	@Override
	public boolean supportsPartialMessages() {
		return false;
	}

}
