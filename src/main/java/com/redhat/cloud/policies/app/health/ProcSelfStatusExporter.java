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
package com.redhat.cloud.policies.app.health;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Exports the following from /proc/self/status. See proc(5)
 * VmHWM:    265580 kB
 * VmRSS:    233156 kB
 * RssAnon:          210444 kB
 * RssFile:           22712 kB
 * VmStk:       136 kB
 * VmLib:     24416 kB
 * VmData:  3529900 kB
 * VmSize:  13529900 kB
 * Threads: 23
 */
@ApplicationScoped
public class ProcSelfStatusExporter {

    private final Logger log = Logger.getLogger(this.getClass().getSimpleName());

    private static final String PATHNAME = "/proc/self/status";

    private boolean hasWarned = false;

    private final AtomicLong vmHwm = new AtomicLong(0);
    private final AtomicLong vmRss = new AtomicLong(0);
    private final AtomicLong rssAnon = new AtomicLong(0);
    private final AtomicLong rssFile = new AtomicLong(0);
    private final AtomicLong vmStk = new AtomicLong(0);
    private final AtomicLong vmLib = new AtomicLong(0);
    private final AtomicLong vmData = new AtomicLong(0);
    private final AtomicLong vmSize = new AtomicLong(0);
    private final AtomicInteger threads = new AtomicInteger(0);

    @Inject
    MeterRegistry meterRegistry;

    @PostConstruct
    public void initCounters() {
        meterRegistry.gauge("status.vmHwm", vmHwm);
        meterRegistry.gauge("status.vmRss", vmRss);
        meterRegistry.gauge("status.rssAnon", rssAnon);
        meterRegistry.gauge("status.rssFile", rssFile);
        meterRegistry.gauge("status.vmStk", vmStk);
        meterRegistry.gauge("status.vmLib", vmLib);
        meterRegistry.gauge("status.vmData", vmData);
        meterRegistry.gauge("status.vmSize", vmSize);
        meterRegistry.gauge("status.threads", threads);
    }

    @Scheduled(every = "10s")
    void gather() {

        File status = new File(PATHNAME);
        if (!status.exists() || !status.canRead()) {
            if (!hasWarned) {
                log.warning("Can't read " + PATHNAME);
                hasWarned = true;
            }
            return;
        }

        try (Scanner fr = new Scanner(status)) {
            while (fr.hasNextLine()) {
                String line = fr.nextLine();
                String[] parts = line.split("[ \t]+");

                switch (parts[0]) {
                    case "VmHWM:":
                        vmHwm.set(Long.parseLong(parts[1]));
                        break;
                    case "VmRSS:":
                        vmRss.set(Long.parseLong(parts[1]));
                        break;
                    case "RssAnon:":
                        rssAnon.set(Long.parseLong(parts[1]));
                        break;
                    case "RssFile:":
                        rssFile.set(Long.parseLong(parts[1]));
                        break;
                    case "VmStk:":
                        vmStk.set(Long.parseLong(parts[1]));
                        break;
                    case "VmLib:":
                        vmLib.set(Long.parseLong(parts[1]));
                        break;
                    case "VmData:":
                        vmData.set(Long.parseLong(parts[1]));
                        break;
                    case "VmSize:":
                        vmSize.set(Long.parseLong(parts[1]));
                        break;
                    case "Threads:":
                        threads.set(Integer.parseInt(parts[1]));
                        break;
                    default:
                        // That file has more entries, but which we don't care about
                }
            }
        } catch (Exception e) {
            log.warning("Reading failed: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ProcSelfStatusExporter{");
        sb.append("vmHwm=").append(vmHwm.get() / 1024);
        sb.append(", vmRss=").append(vmRss.get() / 1024);
        sb.append(", rssAnon=").append(rssAnon.get() / 1024);
        sb.append(", rssFile=").append(rssFile.get() / 1024);
        sb.append(", vmStk=").append(vmStk.get() / 1024);
        sb.append(", vmLib=").append(vmLib.get() / 1024);
        sb.append(", vmData=").append(vmData.get() / 1024);
        sb.append(", vmSize=").append(vmSize.get() / 1024);
        sb.append(", threads=").append(threads);
        sb.append('}');
        return sb.toString();
    }
}
