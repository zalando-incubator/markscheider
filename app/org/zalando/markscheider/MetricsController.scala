package org.zalando.markscheider

import java.io.StringWriter

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{ ObjectMapper, ObjectWriter }
import com.google.inject.Inject
import play.api.Configuration
import play.api.http.{ HeaderNames, MimeTypes }
import play.api.mvc._

import scala.collection.JavaConverters._

class MetricsController @Inject() (
    plugin:        MetricsPlugin,
    configuration: Configuration,
    cc:            ControllerComponents
)
  extends AbstractController(cc) with PrometheusSupport {
  private def defaultFormat: String = configuration.getAndValidate[String]("org.zalando.markscheider.defaultFormat", Set("zmon", "prometheus"))

  private def serializePrometheus(registry: MetricRegistry): Result = {
    val histograms = (for {
      (metricsName, histogram) <- registry.getHistograms.asScala
      extractedName <- ExtractedName.fromMetricsName(metricsName)
    } yield {
      import extractedName.{ labels, name }

      PrometheusMetric.fromSnapshot(name, labels, histogram.getSnapshot)
    }).flatten

    val timers = (for {
      (metricsName, timer) <- registry.getTimers().asScala
      extractedName <- ExtractedName.fromMetricsName(metricsName)
    } yield {
      import extractedName.{ labels, name }

      PrometheusMetric.fromTimer(name, labels, timer)
    }).flatten

    val stringWriter = new StringWriter()
    for { entry <- histograms ++ timers } {
      stringWriter.write(entry.toString)
    }
    Ok(stringWriter.toString).withHeaders(HeaderNames.CACHE_CONTROL -> "must-revalidate,no-cache,no-store").as("text/plain; version=0.0.4")
  }

  private def serializeJson(mapper: ObjectMapper): Result = {
    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, plugin.registry)
    Ok(stringWriter.toString).withHeaders(HeaderNames.CACHE_CONTROL -> "must-revalidate,no-cache,no-store").as(MimeTypes.JSON)
  }

  val acceptsAnything = Accepting("")
  val acceptsText = Accepting(MimeTypes.TEXT)
  def metrics: Action[AnyContent] = Action { implicit request =>
    if (plugin.enabled) {
      render {
        case acceptsAnything() => defaultFormat match {
          case "zmon"       => serializeJson(plugin.mapper)
          case "prometheus" => serializePrometheus(plugin.registry)
        }
        case acceptsText()    => serializePrometheus(plugin.registry)
        case Accepts.Json | _ => serializeJson(plugin.mapper)
      }
    } else {
      InternalServerError("metrics plugin not enabled")
    }
  }
}
