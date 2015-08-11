package org.zalando.markscheider

import java.io.StringWriter

import play.api.{ Application, Logger, Play }
import play.api.mvc.{ Action, AnyContent, Controller, Result }
import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{ ObjectMapper, ObjectWriter }
import com.google.inject.Inject
import play.api.http.{ HeaderNames, MimeTypes }

class MetricsController @Inject() (plugin: MetricsPlugin) extends Controller {

  def serialize(mapper: ObjectMapper): Result = {
    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, plugin.registry)
    Ok(stringWriter.toString).as(MimeTypes.JSON).withHeaders(HeaderNames.CACHE_CONTROL -> "must-revalidate,no-cache,no-store")
  }

  def metrics: Action[AnyContent] = Action {
    if (plugin.enabled) {
      serialize(plugin.mapper)
    } else {
      InternalServerError("metrics plugin not enabled")
    }
  }

}
