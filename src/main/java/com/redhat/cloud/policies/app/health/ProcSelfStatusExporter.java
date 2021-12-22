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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.util.Scanner;
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

    @Inject
    MeterRegistry registry;

    // @Gauge(name = "status.vmHwm", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
    Long vmHwm = registry.gauge("status.vmHwm", 0L);

    // @Gauge(name = "status.vmRss", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
    Long vmRss = registry.gauge("status.vmRss", 0L);

    // @Gauge(name = "status.rssAnon", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
    Long rssAnon = registry.gauge("status.rssAnon", 0L);

    // @Gauge(name = "status.rssFile", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
    Long rssFile = registry.gauge("status.rssFile", 0L);

    // @Gauge(name = "status.vmStk", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
    Long vmStk = registry.gauge("status.vmStk", 0L);

    // @Gauge(name = "status.vmLib", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
    Long vmLib = registry.gauge("status.vmLib", 0L);

    // @Gauge(name = "status.vmData", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
    Long vmData = registry.gauge("status.vmData", 0L);

    // @Gauge(name = "status.vmSize", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
    Long vmSize = registry.gauge("status.vmSize", 0L);

    // @Gauge(name = "status.threads", absolute = true, unit = MetricUnits.NONE, tags = "type=proc")
    Integer threads = registry.gauge("status.threads", 0);

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
                        vmHwm = Long.parseLong(parts[1]);
                        break;
                    case "VmRSS:":
                        vmRss = Long.parseLong(parts[1]);
                        break;
                    case "RssAnon:":
                        rssAnon = Long.parseLong(parts[1]);
                        break;
                    case "RssFile:":
                        rssFile = Long.parseLong(parts[1]);
                        break;
                    case "VmStk:":
                        vmStk = Long.parseLong(parts[1]);
                        break;
                    case "VmLib:":
                        vmLib = Long.parseLong(parts[1]);
                        break;
                    case "VmData:":
                        vmData = Long.parseLong(parts[1]);
                        break;
                    case "VmSize:":
                        vmSize = Long.parseLong(parts[1]);
                        break;
                    case "Threads:":
                        threads = Integer.parseInt(parts[1]);
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
        sb.append("vmHwm=").append(vmHwm / 1024);
        sb.append(", vmRss=").append(vmRss / 1024);
        sb.append(", rssAnon=").append(rssAnon / 1024);
        sb.append(", rssFile=").append(rssFile / 1024);
        sb.append(", vmStk=").append(vmStk / 1024);
        sb.append(", vmLib=").append(vmLib / 1024);
        sb.append(", vmData=").append(vmData / 1024);
        sb.append(", vmSize=").append(vmSize / 1024);
        sb.append(", threads=").append(threads);
        sb.append('}');
        return sb.toString();
    }
}
