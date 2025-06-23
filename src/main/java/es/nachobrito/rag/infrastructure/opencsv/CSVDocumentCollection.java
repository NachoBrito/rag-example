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

package es.nachobrito.rag.infrastructure.opencsv;

import com.opencsv.CSVReader;
import es.nachobrito.rag.domain.document.Document;
import es.nachobrito.rag.domain.document.DocumentCollection;
import es.nachobrito.rag.domain.document.DocumentLoadException;
import es.nachobrito.rag.domain.document.StringDocument;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Stream;

/**
 * @author nacho
 */
public class CSVDocumentCollection implements DocumentCollection {
  private final String filePath;
  private final int idColumn;
  private final int documentColumn;
  private final int maxDocuments;

  private int documentsLoaded = 0;

  public CSVDocumentCollection(
      String filePath, int idColumn, int documentColumn, int maxDocuments) {
    this.filePath = filePath;
    this.idColumn = idColumn;
    this.documentColumn = documentColumn;
    this.maxDocuments = maxDocuments;
  }

  public CSVDocumentCollection(String filePath, int idColumn, int documentColumn) {
    this(filePath, idColumn, documentColumn, -1);
  }

  @Override
  public Stream<Document> stream() {
    try {
      CSVReader reader = new CSVReader(new FileReader(filePath));
      var iterator = reader.iterator();
      if (!iterator.hasNext()) {
        throw new DocumentLoadException("Document is empty!");
      }
      // discard first row
      iterator.next();
      return Stream.generate(() -> null)
          .takeWhile(
              x -> {
                if (maxDocuments > 0 && documentsLoaded >= maxDocuments) {
                  return false;
                }
                return iterator.hasNext();
              })
          .onClose(
              () -> {
                try {
                  reader.close();
                } catch (IOException e) {
                  throw new DocumentLoadException(e);
                }
              })
          .map(it -> iterator.next())
          .map(this::readDocument);

    } catch (IOException e) {
      throw new DocumentLoadException(e);
    }
  }

  private Document readDocument(String[] row) {
    if (row.length < documentColumn) {
      throw new DocumentLoadException("Invalid number of columns in CSV file!");
    }
    var doc = new StringDocument(row[idColumn], row[documentColumn]);
    documentsLoaded++;
    return doc;
  }
}
