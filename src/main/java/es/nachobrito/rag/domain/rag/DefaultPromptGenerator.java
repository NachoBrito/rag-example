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

import jakarta.inject.Singleton;

/**
 * @author nacho
 */
@Singleton
public class DefaultPromptGenerator implements PromptGenerator {
  @Override
  public String generate(RagQuery query) {
    return
"""
%s
"""
        .formatted(query.text());
  }
}
