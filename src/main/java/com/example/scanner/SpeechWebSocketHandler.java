package com.example.scanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

public class SpeechWebSocketHandler implements WebSocketHandler {
	
	private final Logger logger = LoggerFactory.getLogger(SpeechWebSocketHandler.class);
	private Map<String, List<String>> telemetry = new HashMap<>();
	
	public SpeechWebSocketHandler() {
		resetTelemetry();
	}
	
	private void resetTelemetry() {
		telemetry.clear();
		telemetry.put("speech.hypothesis", new ArrayList<>());
		telemetry.put("speech.endDetected", new ArrayList<>());
		telemetry.put("speech.phrase", new ArrayList<>());
		telemetry.put("turn.end", new ArrayList<>());
	}
	
	private final String telemAckMsg = 
			"Path:telemetry\r\n" +
			"X-Timestamp:%s\r\n" + 
			"Content-Type:application/json\r\n" +
			"\r\n%s";
	
	private final String telemAckPayload = "\"ReceivedMessages\": [\r\n" + 
			"    { \"speech.hypothesis\": %s },\r\n" + 
			"    { \"speech.endDetected\": %s },\r\n" + 
			"    { \"speech.phrase\": %s },\r\n" + 
			"    { \"turn.end\": %s }\r\n" + 
			"  ]";

	private void addTelemetry(String type) {
		telemetry.get(type).add(SpeechUtils.now());
	}
	
	private String buildTelmAck(String type) {
		String rtn = "";
		List<String> ts = telemetry.get(type);
		if (ts.size() > 1) {
			StringBuilder rtnBuilder = new StringBuilder();
			rtnBuilder.append("[ ");
			for (int i = 0; i < ts.size(); i++) {
				rtnBuilder.append("\"" + ts.get(i) + "\"");
				if (i + 1 < ts.size()) {
					rtnBuilder.append(", ");
				}
			}
			rtnBuilder.append(" ]");
			rtn = rtnBuilder.toString();
		} else {
			if (!ts.isEmpty()) {
				rtn = "\"" + ts.get(0) + "\"";
			}
		}
		return rtn;
	}
	
	private String buildTelmAckPayload() {
		return String.format(telemAckPayload, buildTelmAck("speech.hypothesis"), buildTelmAck("speech.endDetected"), buildTelmAck("speech.phrase"), buildTelmAck("turn.end"));
	}
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		logger.info("connection established, session id: {}", session.getId());
	}
	
	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		String payload = message.getPayload().toString();
		if (payload.contains("Path:turn.end")) {
			addTelemetry("turn.end");
			String ackPayload = String.format(telemAckMsg, SpeechUtils.now(), buildTelmAckPayload());
			logger.info("sending turn end ack {}", ackPayload);
			TextMessage ackMsg = new TextMessage(ackPayload);
			session.sendMessage(ackMsg);
			resetTelemetry();
		} else if (payload.contains("Path:speech.hypothesis")) {
			addTelemetry("speech.hypothesis");
		} else if (payload.contains("Path:speech.endDetected")) {
			addTelemetry("speech.endDetected");
		} else if (payload.contains("Path:speech.phrase")) {
			addTelemetry("speech.phrase");
			logger.info("text recieved, {}", payload);
		}
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
