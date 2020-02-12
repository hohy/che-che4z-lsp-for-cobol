/*
 * Copyright (c) 2020 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Broadcom, Inc. - initial API and implementation
 */
package com.ca.lsp.cobol.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toList;
import static org.eclipse.lsp4j.TextDocumentSyncKind.Full;

/**
 * This class sets up the initial state of the services and applies other initialization activities,
 * such as set server capabilities and register file system watchers.
 */
@Singleton
public class MyLanguageServerImpl implements LanguageServer {
  /** Glob patterns to watch COPYBOOKS folder and copybook files */
  private static final List<String> WATCHER_PATTERNS =
      asList("**/COPYBOOKS/*.cpy", "**/COPYBOOKS/*.CPY", "**/COPYBOOKS");

  /**
   * The kind of events of interest, for watchers calculated as WatchKind.Create | WatchKind.Change
   * | WatchKind.Delete which is 7
   */
  private static final int WATCH_ALL_KIND = 7;

  private TextDocumentService textService;
  private CobolWorkspaceService workspaceService;
  private FileSystemService fileSystemService;
  private Provider<LanguageClient> clientProvider;

  @Inject
  MyLanguageServerImpl(
      FileSystemService fileSystemService,
      TextDocumentService textService,
      CobolWorkspaceService workspaceService,
      Provider<LanguageClient> clientProvider) {
    this.textService = textService;
    this.fileSystemService = fileSystemService;
    this.workspaceService = workspaceService;
    this.clientProvider = clientProvider;
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return textService;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return workspaceService;
  }

  /**
   * Initialized request sent from the client after the 'initialize' request resolved. It is used as
   * hook to dynamically register capabilities, e.g. file system watchers.
   *
   * @param params - InitializedParams sent by a client
   */
  @Override
  public void initialized(@Nullable InitializedParams params) {
    LanguageClient client = clientProvider.get();

    RegistrationParams registrationParams =
        new RegistrationParams(
            asList(
                new Registration(
                    "copybooksWatcher", "workspace/didChangeWatchedFiles", createWatcher()),
                new Registration("configurationChange", "workspace/didChangeConfiguration", null)));
    client.registerCapability(registrationParams);
  }

  @Override
  @Nonnull
  public CompletableFuture<InitializeResult> initialize(@Nonnull InitializeParams params) {
    ServerCapabilities capabilities = new ServerCapabilities();

    capabilities.setTextDocumentSync(Full);
    capabilities.setCompletionProvider(new CompletionOptions(true, emptyList()));
    capabilities.setDefinitionProvider(TRUE);
    capabilities.setReferencesProvider(TRUE);
    capabilities.setDocumentFormattingProvider(TRUE);
    capabilities.setDocumentHighlightProvider(TRUE);

    WorkspaceFoldersOptions workspaceFoldersOptions = new WorkspaceFoldersOptions();
    workspaceFoldersOptions.setSupported(TRUE);
    WorkspaceServerCapabilities workspaceServiceCapabilities =
        new WorkspaceServerCapabilities(workspaceFoldersOptions);
    capabilities.setWorkspace(workspaceServiceCapabilities);

    fileSystemService.setWorkspaceFolders(params.getWorkspaceFolders());
    return supplyAsync(() -> new InitializeResult(capabilities));
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    return supplyAsync(() -> TRUE);
  }

  @Override
  public void exit() {
    // not supported
  }

  @Nonnull
  private DidChangeWatchedFilesRegistrationOptions createWatcher() {
    return new DidChangeWatchedFilesRegistrationOptions(
        WATCHER_PATTERNS.stream()
            .map(it -> new FileSystemWatcher(it, WATCH_ALL_KIND))
            .collect(toList()));
  }
}
