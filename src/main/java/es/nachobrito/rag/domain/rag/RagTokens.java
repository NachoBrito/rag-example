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

package es.nachobrito.rag.domain.rag;

import java.util.UUID;

/**
 * @author nacho
 */
public record RagTokens(UUID queryUuid, String tokens, boolean isComplete) {
  public static RagTokens partialResponse(UUID queryUuid, String tokens) {
    return new RagTokens(queryUuid, tokens, false);
  }

  public static RagTokens completeResponse(UUID queryUuid) {
    return new RagTokens(queryUuid, "", true);
  }
}
