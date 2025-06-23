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

package es.nachobrito.rag.infrastructure.llm.langchain4j;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.jlama.JlamaEmbeddingModel;
import dev.langchain4j.model.jlama.JlamaStreamingChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import es.nachobrito.rag.domain.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author nacho
 */
class Assistant {
  private static final String EMBEDDING_MODEL = "intfloat/e5-small-v2";
  private static final String CHAT_MODEL = "tjake/Llama-3.2-1B-Instruct-JQ4";
  private static final Logger log = LoggerFactory.getLogger(Assistant.class);
  private final Bot bot;
  private final EmbeddingStoreIngestor ingestor;

  private Assistant(Bot bot, EmbeddingStoreIngestor ingestor) {
    this.bot = bot;
    this.ingestor = ingestor;
  }

  static Assistant make() {
    InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    var embeddingModel = getEmbeddingModel();
    var chatModel = getChatModel();

    var embeddingStoreContentRetriever =
        EmbeddingStoreContentRetriever.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build();

    var assistant =
        AiServices.builder(Bot.class)
            .streamingChatModel(chatModel)
            .chatMemory(
                MessageWindowChatMemory.withMaxMessages(
                    10)) // it should remember 10 latest messages
            .contentRetriever(
                embeddingStoreContentRetriever) // it should have access to our documents
            .build();

    var embeddingStoreIngestor =
        EmbeddingStoreIngestor.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build();

    return new Assistant(assistant, embeddingStoreIngestor);
  }

  static StreamingChatModel getChatModel() {
    log.info("Preparing chat model: {}", CHAT_MODEL);
    return JlamaStreamingChatModel.builder().modelName(CHAT_MODEL).build();
  }

  static EmbeddingModel getEmbeddingModel() {
    log.info("Preparing embedding model: {}", EMBEDDING_MODEL);
    return JlamaEmbeddingModel.builder().modelName(EMBEDDING_MODEL).build();
  }

  void ingest(Document document) {
    ingestor.ingest(dev.langchain4j.data.document.Document.from(document.toString()));
  }

  TokenStream chat(String message) {
    return bot.chat(message);
  }

  interface Bot {
    TokenStream chat(String message);
  }
}
