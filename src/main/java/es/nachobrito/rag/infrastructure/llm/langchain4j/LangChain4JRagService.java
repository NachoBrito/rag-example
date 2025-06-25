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

import static java.util.stream.Collectors.joining;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.jlama.JlamaEmbeddingModel;
import dev.langchain4j.model.jlama.JlamaStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import es.nachobrito.rag.domain.document.Document;
import es.nachobrito.rag.domain.rag.RagQuery;
import es.nachobrito.rag.domain.rag.RagService;
import es.nachobrito.rag.domain.rag.RagTokens;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author nacho
 */
@Singleton
public class LangChain4JRagService implements RagService {

  // expect a more focused and deterministic answer
  public static final float TEMPERATURE = 0.2f;
  private static final String EMBEDDING_MODEL = "intfloat/e5-small-v2";
  private static final String CHAT_MODEL = "tjake/Llama-3.2-1B-Instruct-JQ4";
  private static final Logger log = LoggerFactory.getLogger(LangChain4JRagService.class);
  private final DocumentSplitter splitter = DocumentSplitters.recursive(200, 0);
  private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
  private final PromptTemplate promptTemplate =
      PromptTemplate.from(
          "Context information is below.:\n"
              + "------------------\n"
              + "{{information}}\n"
              + "------------------\n"
              + "Given the context information and not prior knowledge, answer the query.\n"
              + "Query: {{question}}\n"
              + "Answer:");
  private final String cacheFilePath;
  private final EmbeddingMatchMapper embeddingMatchMapper;

  private EmbeddingModel embeddingModel;
  private StreamingChatModel chatModel;

  public LangChain4JRagService(
      @Property(name = "remo.embeddings.cache") String cacheFilePath,
      EmbeddingMatchMapper embeddingMatchMapper) {
    this.cacheFilePath = cacheFilePath;
    this.embeddingMatchMapper = embeddingMatchMapper;
    embeddingStore = initializeEmbeddingStore();
  }

  private InMemoryEmbeddingStore<TextSegment> initializeEmbeddingStore() {
    if (Path.of(cacheFilePath).toFile().isFile()) {
      return InMemoryEmbeddingStore.fromFile(cacheFilePath);
    }
    return new InMemoryEmbeddingStore<>();
  }

  private EmbeddingModel getEmbeddingModel() {
    if (embeddingModel == null) {
      embeddingModel = JlamaEmbeddingModel.builder().modelName(EMBEDDING_MODEL).build();
    }
    return embeddingModel;
  }

  private StreamingChatModel getChatModel() {
    if (chatModel == null) {
      chatModel =
          JlamaStreamingChatModel.builder().modelName(CHAT_MODEL).temperature(TEMPERATURE).build();
    }
    return chatModel;
  }

  @Override
  public void ingest(Document document) {
    log.info("Ingesting document: {}", document.getId());
    var content = document.getTextContent();
    var metadata = new Metadata(document.getMetadata());
    var l4Document = dev.langchain4j.data.document.Document.from(content, metadata);
    var segments = splitter.split(l4Document);
    var embeddingModel = getEmbeddingModel();
    var embeddings = embeddingModel.embedAll(segments).content();
    embeddingStore.addAll(embeddings, segments);
  }

  @Override
  public void chat(RagQuery query, Consumer<RagTokens> ragTokensConsumer) {
    Prompt prompt = buildPrompt(query);
    var userMessage = prompt.text();
    log.info("Generated prompt\n{}", userMessage);
    getChatModel()
        .chat(
            userMessage,
            new StreamingChatResponseHandler() {
              @Override
              public void onPartialResponse(String s) {
                ragTokensConsumer.accept(RagTokens.partialResponse(query.uuid(), s));
              }

              @Override
              public void onCompleteResponse(ChatResponse chatResponse) {
                log.info("Response complete: {}", chatResponse.aiMessage().text());
              }

              @Override
              public void onError(Throwable throwable) {
                log.error(throwable.getMessage(), throwable);
              }
            });
  }

  @Override
  public void serialize() {
    embeddingStore.serializeToFile(cacheFilePath);
  }

  private Prompt buildPrompt(RagQuery query) {
    List<EmbeddingMatch<TextSegment>> relevantEmbeddings = getRelevantEmbeddings(query);

    String information =
        relevantEmbeddings.stream()
            .map(embeddingMatchMapper::map)
            .distinct()
            .collect(joining("\n\n"));

    Map<String, Object> promptInputs = new HashMap<>();
    promptInputs.put("question", query.text());
    promptInputs.put("information", information);

    return promptTemplate.apply(promptInputs);
  }

  private List<EmbeddingMatch<TextSegment>> getRelevantEmbeddings(RagQuery query) {
    Embedding questionEmbedding = getEmbeddingModel().embed(query.text()).content();
    EmbeddingSearchRequest embeddingSearchRequest =
        EmbeddingSearchRequest.builder()
            .queryEmbedding(questionEmbedding)
            .maxResults(2)
            .minScore(0.75)
            .build();
    return embeddingStore.search(embeddingSearchRequest).matches();
  }
}
