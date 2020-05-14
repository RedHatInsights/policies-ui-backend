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

/**
 * @author hrupp
 */

/*
[{"eventType":"ALERT","tenantId":"Td02c1a05-cc5a-485a-9791-7731f7b4ce681","id":"test-manual2-trigger-1587105536098-eb8f33e6-242b-4ea1-95c2-77282e83d109","ctime":1587105536098,"dataSource":"_none_","dataId":"test-manual2-trigger","category":"ALERT","text":"test-manual2-trigger","trigger":{"tenantId":"Td02c1a05-cc5a-485a-9791-7731f7b4ce681","id":"test-manual2-trigger","name":"test-manual2-trigger","type":"STANDARD","eventType":"ALERT","eventCategory":"","eventText":"","severity":"MEDIUM","autoDisable":false,"autoEnable":false,"autoResolve":false,"autoResolveAlerts":false,"autoResolveMatch":"ALL","enabled":true,"firingMatch":"ALL","source":"_none_"},"dampening":{"tenantId":"Td02c1a05-cc5a-485a-9791-7731f7b4ce681","triggerId":"test-manual2-trigger","triggerMode":"FIRING","type":"STRICT","evalTrueSetting":1,"evalTotalSetting":1,"evalTimeSetting":0,"dampeningId":"Td02c1a05-cc5a-485a-9791-7731f7b4ce681-test-manual2-trigger-FIRING"},"evalSets":[[{"evalTimestamp":1587105536096,"dataTimestamp":4000,"type":"AVAILABILITY","displayString":"Avail: test-manual2-avail[DOWN] is NOT_UP","condition":{"tenantId":"Td02c1a05-cc5a-485a-9791-7731f7b4ce681","triggerId":"test-manual2-trigger","triggerMode":"FIRING","type":"AVAILABILITY","conditionSetSize":1,"conditionSetIndex":1,"conditionId":"Td02c1a05-cc5a-485a-9791-7731f7b4ce681-test-manual2-trigger-FIRING-1-1","displayString":"test-manual2-avail is NOT_UP","lastEvaluation":1587105536100,"dataId":"test-manual2-avail","operator":"NOT_UP"},"value":"DOWN"}]],"severity":"MEDIUM","status":"ACKNOWLEDGED","notes":[{"user":"testUser","ctime":1587105672981,"text":"testNotes"}],"lifecycle":[{"status":"OPEN","user":"system","stime":1587105536098},{"status":"ACKNOWLEDGED","user":"testUser","stime":1587105672981}]}]
 */
public class Alert {

  public long ctime;
  String eventType;
  public Trigger trigger;

}
