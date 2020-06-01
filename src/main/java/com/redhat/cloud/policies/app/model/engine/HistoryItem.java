/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.cloud.policies.app.model.engine;


import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A single trigger history item
 * @author hrupp
 */

@Schema(description = "A single history item for a fired trigger on a host")
public class HistoryItem {


  @Schema(description = "Fire time (since the epoch)")
  public long ctime;
  @Schema(description = "Host id")
  public String id;
  @Schema(description = "Host name")
  public String hostName;

  public HistoryItem(long ctime, String id, String hostName) {
    this.ctime = ctime;
    this.id = id;
    this.hostName = hostName;
  }
}
