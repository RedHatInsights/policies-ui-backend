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

import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.Scanner;


/**
 * Exports the following from /proc/self/status. See proc(5)
 * VmHWM:    265580 kB
 * VmRSS:    233156 kB
 * RssAnon:          210444 kB
 * RssFile:           22712 kB
 * VmStk:       136 kB
 * VmLib:     24416 kB
 * VmData:  3529900 kB
 *
 * @author hrupp
 */
@ApplicationScoped
public class ProcSelfStatusExporter {

  private static final String PATHNAME = "/proc/self/status";
//  private static final String PATHNAME = "/tmp/foo";

  long vmHwm;
  long vmRss;
  long rssAnon;
  long rssFile;
  long vmStk;
  long vmLib;
  long vmData;
  long vmSize;


  @Scheduled(every = "10s")
  void gather() {

    File status = new File(PATHNAME);
    if (!status.exists() || !status.canRead()) {
      System.out.println("Can't read " + PATHNAME);
      return;
    }

    try (Scanner fr = new Scanner(status)) {
      while (fr.hasNextLine()) {
        String line = fr.nextLine();
        String[] parts = line.split("[ \t]+");

        switch (parts[0]) {
          case "VmHWM:":
            vmHwm = Long.parseLong(parts[1]) ;
            break;
          case "VmRSS:":
            vmRss = Long.parseLong(parts[1]) ;
            break;
          case "RssAnon:":
            rssAnon = Long.parseLong(parts[1]) ;
            break;
          case "RssFile:":
            rssFile = Long.parseLong(parts[1]) ;
            break;
          case "VmStk:":
            vmStk = Long.parseLong(parts[1]) ;
            break;
          case "VmLib:":
            vmLib = Long.parseLong(parts[1]) ;
            break;
          case "VmData:":
            vmData = Long.parseLong(parts[1]) ;
            break;
          case "VmSize:":
            vmSize = Long.parseLong(parts[1]) ;
            break;
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Gauge(name="status.vmHwm", absolute = true, unit = MetricUnits.KILOBYTES,tags = "type=proc" )
  public long getVmHwm() {
    return vmHwm;
  }

  @Gauge(name="status.vmRss", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
  public long getVmRss() {
    return vmRss;
  }

  @Gauge(name="status.rssAnon", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
  public long getRssAnon() {
    return rssAnon;
  }

  @Gauge(name="status.rssFile", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
  public long getRssFile() {
    return rssFile;
  }

  @Gauge(name="status.vmStk", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
  public long getVmStk() {
    return vmStk;
  }

  @Gauge(name="status.vmLib", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
  public long getVmLib() {
    return vmLib;
  }

  @Gauge(name="status.vmData", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
  public long getVmData() {
    return vmData;
  }

  @Gauge(name="status.vmSize", absolute = true, unit = MetricUnits.KILOBYTES, tags = "type=proc")
  public long getVmSize() {
    return vmSize;
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
    sb.append('}');
    return sb.toString();
  }
}
