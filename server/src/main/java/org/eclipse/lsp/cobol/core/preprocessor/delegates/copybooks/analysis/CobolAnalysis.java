/*
 * Copyright (c) 2021 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Broadcom, Inc. - initial API and implementation
 *
 */

package org.eclipse.lsp.cobol.core.preprocessor.delegates.copybooks.analysis;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp.cobol.core.messages.MessageService;
import org.eclipse.lsp.cobol.core.model.CopybookUsage;
import org.eclipse.lsp.cobol.core.model.ResultWithErrors;
import org.eclipse.lsp.cobol.core.model.SyntaxError;
import org.eclipse.lsp.cobol.core.preprocessor.TextPreprocessor;
import org.eclipse.lsp.cobol.core.preprocessor.delegates.copybooks.ReplacingService;
import org.eclipse.lsp.cobol.service.CopybookService;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.eclipse.lsp.cobol.core.model.ErrorSeverity.ERROR;

/**
 * This implementation of the {@link AbstractCopybookAnalysis} provides logic for plain COBOL logic
 * applying replacing if presents.
 */
class CobolAnalysis extends AbstractCopybookAnalysis {
  private static final int MAX_COPYBOOK_NAME_LENGTH_DATASET = 8;

  private final ReplacingService replacingService;

  CobolAnalysis(
      TextPreprocessor preprocessor,
      CopybookService copybookService,
      MessageService messageService,
      ReplacingService replacingService) {
    super(preprocessor, copybookService, messageService, MAX_COPYBOOK_NAME_LENGTH_DATASET);
    this.replacingService = replacingService;
  }

  @Override
  protected ResultWithErrors<String> handleReplacing(
      List<Pair<String, String>> copyReplacingClauses,
      Deque<List<Pair<String, String>>> recursiveReplaceStmtStack,
      Deque<CopybookUsage> copybookStack,
      CopybookMetaData metaData,
      String text) {
    // In a chain of copy statement, there could be only one replacing phrase
    List<SyntaxError> errors = new ArrayList<>();
    if (!copyReplacingClauses.isEmpty()) {
      recursiveReplaceStmtStack.add(new ArrayList<>(copyReplacingClauses));
      copyReplacingClauses.clear();
    }
    if (recursiveReplaceStmtStack.size() > 1 && !copybookStack.isEmpty())
      errors.add(
          addCopybookError(
              metaData.getName(),
              metaData.getNameLocality(),
              ERROR,
              "GrammarPreprocessorListener.copyBkNestedReplaceStmt",
              "Syntax error by checkRecursiveReplaceStatement: {}"));

    return new ResultWithErrors<>(
        recursiveReplaceStmtStack.stream()
            .reduce(text, replacingService::applyReplacing, (raw, res) -> res),
        errors);
  }
}
