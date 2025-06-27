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

package es.nachobrito.rag.experiment;

import static java.lang.System.out;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.jlama.JlamaEmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * @author nacho
 */
public class EmbeddingModelComparisonTest {

  @Test
  void expectSimilarityValues() {
    var models =
        Map.of(
            "intfloat/e5-small-v2",
            JlamaEmbeddingModel.builder().modelName("intfloat/e5-small-v2").build(),
            "answerdotai/answerai-colbert-small-v1",
            JlamaEmbeddingModel.builder()
                .modelName("answerdotai/answerai-colbert-small-v1")
                .build());
    var query = "Is it ok to water my tomatoes every day?";
    var candidates =
        Set.of(
            "What is the best time to plant rice?",
            "How often should I water tomato plants?",
            "What fertilizer is good for wheat crops?",
            "How can I control pests on my cotton farm?",
            "Which crop is best for sandy soil?");

    out.printf("Query: %s\n", query);
    models.forEach(
        (modelName, model) -> {
          out.printf("-> Candidates sorted with '%s':\n", modelName);
          sortCandidates(candidates, query, model)
              .forEach(
                  (candidate, similarity) -> {
                    out.printf("%s (%f)\n", candidate, similarity);
                  });
          out.println("======================================");
        });
    // var sorted = sortCandidates(candidates, query, model);
  }

  private static LinkedHashMap<String, Double> sortCandidates(
      Set<String> candidates, String query, EmbeddingModel model) {
    var queryEmbedding = model.embed(query);
    return candidates.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                it ->
                    CosineSimilarity.between(queryEmbedding.content(), model.embed(it).content())))
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, LinkedHashMap::new));
  }
}
