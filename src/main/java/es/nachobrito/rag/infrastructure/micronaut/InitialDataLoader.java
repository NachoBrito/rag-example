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

package es.nachobrito.rag.infrastructure.micronaut;

import es.nachobrito.rag.domain.document.DocumentLoadException;
import es.nachobrito.rag.domain.rag.RagService;
import es.nachobrito.rag.infrastructure.opencsv.CSVFaqDocumentCollection;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author nacho
 */
@Singleton
@Requires(property = "remo.load-data", value = "true")
public class InitialDataLoader implements ApplicationEventListener<StartupEvent> {
  private final Logger logger = LoggerFactory.getLogger(InitialDataLoader.class);

  private final ResourceResolver resourceResolver;
  private final String dataFilePath;
  private final RagService ragService;

  public InitialDataLoader(
      ResourceResolver resourceResolver,
      @Property(name = "remo.data.file") String dataFilePath,
      RagService ragService) {
    this.resourceResolver = resourceResolver;
    this.dataFilePath = dataFilePath;
    this.ragService = ragService;
  }

  @Async(TaskExecutors.BLOCKING)
  @Override
  public void onApplicationEvent(StartupEvent event) {
    loadData();
    ragService.serialize();
  }

  private void loadData() {
    logger.info("Loading data from {}", dataFilePath);
    var url =
        resourceResolver
            .getResource(dataFilePath)
            .orElseThrow(
                () ->
                    new DocumentLoadException(
                        "Could not load data from %s".formatted(dataFilePath)));
    var documentCollection = new CSVFaqDocumentCollection(url.getPath(), 0, 1);

    ragService.ingest(documentCollection);
  }
}
