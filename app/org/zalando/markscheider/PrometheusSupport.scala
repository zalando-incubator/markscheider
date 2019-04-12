package org.zalando.markscheider

import java.time.{ LocalDateTime, ZoneOffset }

import com.codahale.metrics.{ Snapshot, Timer }

trait PrometheusSupport {
  import PrometheusSupport.DefaultMetricName

  case class ExtractedName(name: String, labels: Map[String, String])
  object ExtractedName {
    def fromMetricsName(metricsName: String): Option[ExtractedName] = {
      metricsName match {
        case DefaultMetricName(prefix, responseCode, "ALL", "ALL") if responseCode.contains("xx") =>
          None
        case DefaultMetricName(prefix, responseCode, httpMethod, controller) => Option(
          ExtractedName(
            s"${prefix.replace(".", "_")}_http_response",
            Map("httpcode" -> responseCode, "httpmethod" -> httpMethod, "method" -> controller)
          )
        )
        case other => Option(ExtractedName(other.replace(".", "_"), Map.empty))
      }
    }
  }
  case class PrometheusMetric(name: String, value: Double, labels: Map[String, String] = Map.empty, timestamp: LocalDateTime = LocalDateTime.now) {
    require(!name.contains(" "))
    require(!labels.keys.exists(_.contains(" ")))

    private def labelsToString(labels: Map[String, String]) = labels.map{ case (key, value) => s"$key=$value" }.mkString(",")
    override def toString: String =
      s"$name{${labelsToString(labels)}} $value ${timestamp.toEpochSecond(ZoneOffset.UTC)}${timestamp.getNano}\n"
  }
  object PrometheusMetric {
    def fromSnapshot(name: String, labels: Map[String, String], snapshot: Snapshot, now: LocalDateTime = LocalDateTime.now): Seq[PrometheusMetric] = {
      Seq(
        PrometheusMetric(name + "_median", snapshot.getMedian, labels, now),
        PrometheusMetric(name + "_mean", snapshot.getMean, labels, now),
        PrometheusMetric(name + "_stddev", snapshot.getStdDev, labels, now),
        PrometheusMetric(name + "_max", snapshot.getMax, labels, now),
        PrometheusMetric(name + "_min", snapshot.getMin, labels, now),
        PrometheusMetric(name + "_p75", snapshot.get75thPercentile(), labels, now),
        PrometheusMetric(name + "_p95", snapshot.get95thPercentile(), labels, now),
        PrometheusMetric(name + "_p98", snapshot.get98thPercentile(), labels, now),
        PrometheusMetric(name + "_p99", snapshot.get99thPercentile(), labels, now),
        PrometheusMetric(name + "_p999", snapshot.get999thPercentile(), labels, now)
      )
    }

    def fromTimer(name: String, labels: Map[String, String], timer: Timer): Seq[PrometheusMetric] = {
      val now = LocalDateTime.now
      PrometheusMetric.fromSnapshot(name, labels, timer.getSnapshot) ++ Seq(
        PrometheusMetric(name + "_meanrate", timer.getMeanRate, labels, now),
        PrometheusMetric(name + "_1mrate", timer.getOneMinuteRate, labels, now),
        PrometheusMetric(name + "_5mrate", timer.getFiveMinuteRate, labels, now),
        PrometheusMetric(name + "_15mrate", timer.getFifteenMinuteRate, labels, now),
        PrometheusMetric(name + "_count", timer.getCount, labels, now)
      )
    }
  }
}

object PrometheusSupport {
  val DefaultMetricName = """(?:(?<prefix>.*)\.)?(?<responsecode>[0-9]xx|[0-9]{3})\.(?<httpmethod>[^\.]+)\.(?<method>.+)""".r
}
