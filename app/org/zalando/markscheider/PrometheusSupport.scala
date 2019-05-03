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
  case class PrometheusMetric(
      name:      String,
      value:     Double,
      labels:    Map[String, String]   = Map.empty,
      timestamp: Option[LocalDateTime] = Option(LocalDateTime.now)
  ) {
    require(!name.contains(" "))
    require(!labels.keys.exists(_.contains(" ")))

    lazy val timestampAsString: String = timestamp.map(stamp => s"${stamp.toEpochSecond(ZoneOffset.UTC)}${stamp.getNano}").getOrElse("")
    private def labelsToString(labels: Map[String, String]) = labels.map{ case (key, value) => s"""$key="$value"""" }.mkString(",")
    override def toString: String =
      s"$name{${labelsToString(labels)}} $value $timestampAsString\n".trim()
  }
  object PrometheusMetric {
    def fromSnapshot(name: String, labels: Map[String, String], snapshot: Snapshot): Seq[PrometheusMetric] = {
      Seq(
        PrometheusMetric(name + "_mean", snapshot.getMean, labels, None),
        PrometheusMetric(name + "_stddev", snapshot.getStdDev, labels, None),
        PrometheusMetric(name + "_max", snapshot.getMax, labels, None),
        PrometheusMetric(name + "_min", snapshot.getMin, labels, None),
        PrometheusMetric(name + "_bucket", snapshot.getMedian, labels + ("le" -> "0.5"), None),
        PrometheusMetric(name + "_bucket", snapshot.get75thPercentile(), labels + ("le" -> "0.75"), None),
        PrometheusMetric(name + "_bucket", snapshot.get95thPercentile(), labels + ("le" -> "0.95"), None),
        PrometheusMetric(name + "_bucket", snapshot.get98thPercentile(), labels + ("le" -> "0.98"), None),
        PrometheusMetric(name + "_bucket", snapshot.get99thPercentile(), labels + ("le" -> "0.99"), None),
        PrometheusMetric(name + "_bucket", snapshot.get999thPercentile(), labels + ("le" -> "0.999"), None),
        PrometheusMetric(name + "_count", snapshot.size(), labels, None)
      )
    }

    def fromTimer(name: String, labels: Map[String, String], timer: Timer): Seq[PrometheusMetric] = {
      val now = Option(LocalDateTime.now)
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
