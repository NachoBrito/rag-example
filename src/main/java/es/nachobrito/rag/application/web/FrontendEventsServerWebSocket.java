/*
 *    Copyright 2025 Nacho Brito
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package es.nachobrito.rag.application.web;

import es.nachobrito.rag.application.web.model.ResponseTokens;
import es.nachobrito.rag.application.web.model.UserMessage;
import es.nachobrito.rag.domain.rag.RagQuery;
import es.nachobrito.rag.domain.rag.RagService;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.ServerWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author nacho
 */
@ServerWebSocket("/ws/events")
public class FrontendEventsServerWebSocket {

  private static final Logger LOG = LoggerFactory.getLogger(FrontendEventsServerWebSocket.class);

  private final WebSocketBroadcaster broadcaster;
  private final RagService ragService;

  public FrontendEventsServerWebSocket(WebSocketBroadcaster broadcaster, RagService ragService) {
    this.broadcaster = broadcaster;
    this.ragService = ragService;
  }

  //  @OnOpen
  //  public Publisher<String> onOpen(WebSocketSession session) {
  //    log("onOpen", session);
  //
  //    return broadcaster.broadcast("Joined!");
  //  }

  @OnMessage
  public void onMessage(UserMessage userMessage, WebSocketSession session) {
    log("onMessage: %s".formatted(userMessage), session);
    queryRagService(session, userMessage);
  }

  private void queryRagService(WebSocketSession session, UserMessage userMessage) {
    ragService.chat(
        RagQuery.of(userMessage.queryId(), userMessage.message()),
        tokens -> {
          session.sendAsync(ResponseTokens.of(tokens));
        });
  }

  private void log(String event, WebSocketSession session) {
    LOG.info("* WebSocket: {} received for session {}", event, session.getId());
  }
}
