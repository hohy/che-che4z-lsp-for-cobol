/*
 * Copyright (c) 2022 Broadcom.
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

import { SettingsService } from "../Settings";
import { CopybookURI } from "./CopybookURI";
import { Uri } from "vscode";
import * as vscode from "vscode";
import { searchLocalCopybooks } from "./LocalCopybooksService";
import { LocalFilesystemResourceService } from "../LocalFilesystemResourceService";

export async function searchCopybook(
  documentUri: string,
  copybookName: string,
  dialectType: string,
  storagePath: string,
): Promise<Uri | undefined> {
  // search local paths
  const localPathResults = await searchLocalCopybooks(
    documentUri,
    copybookName,
    dialectType,
  );
  if (localPathResults) {
    return localPathResults;
  }

  // search cached dataset results
  const dsnPaths = SettingsService.getDsnPath(documentUri, dialectType);
  const ussPaths = SettingsService.getUssPath(documentUri, dialectType);
  const cachedRemoteResults = await searchCachedRemoteCopybooks(
    dsnPaths.concat(ussPaths),
    copybookName,
    storagePath,
  );

  if (cachedRemoteResults) {
    return cachedRemoteResults;
  }
}

async function searchCachedRemoteCopybooks(
  locations: string[],
  copybookName: string,
  storagePath: string,
) {
  const profile = SettingsService.getProfileName()!;
  const searchDirectoryUris = locations.map((location) =>
    CopybookURI.createDatasetPath([profile], location, storagePath),
  );

  const results = await Promise.allSettled(
    searchDirectoryUris.map(async (directoryUri) =>
      LocalFilesystemResourceService.searchDirectory(
        directoryUri,
        copybookName,
        [""],
      ),
    ),
  );

  const validResults: vscode.Uri[] = [];
  results.forEach((result) => {
    if (result.status === "fulfilled" && result.value) {
      validResults.push(result.value);
    }
  });

  return validResults[0];
}
