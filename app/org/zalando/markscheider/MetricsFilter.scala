package org.zalando.markscheider

import java.util.concurrent.TimeUnit

import akka.stream.Materializer
import com.codahale.metrics.MetricRegistry.name
import com.codahale.metrics._
import com.google.inject.Inject
import play.api.Configuration
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent.{ ExecutionContext, Future }

class MetricsFilter @Inject() (
    registries: MetricRegistries,
    callTimer: CallTimer,
    configuration: Configuration
)(implicit val mat: Materializer, val ec: ExecutionContext)
    extends Filter {

  val registry = registries.getOrCreate
  val prefix = configuration.getOptional[String]("org.zalando.markscheider.prefix.http").getOrElse("zmon.response")
  val selfPrefix = configuration.getOptional[String]("org.zalando.markscheider.prefix.self-http").getOrElse("zmon.self.response")
  val recordExternalCallTimes =
    configuration.getOptional[Boolean]("org.zalando.markscheider.recordExternalCallTimes").getOrElse(true)

  def requestsTimer: Timer = registry.timer(name(classOf[MetricsFilter], "requestTimer"))
  def activeRequests: Counter = registry.counter(name(classOf[MetricsFilter], "activeRequests"))

  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis()
    val method = rh.attrs.get(Router.Attrs.HandlerDef).map(_.method).getOrElse("MetricsFilter")
    val controller = rh.attrs.get(Router.Attrs.HandlerDef).map(_.controller).getOrElse(getClass.getPackage.getName)

    val globalCtx = requestsTimer.time()

    def logCompleted(result: Result): Result = {
      val duration: Long = System.currentTimeMillis() - startTime

      registry.timer(s"$prefix.${result.header.status}.${rh.method}.$controller.$method").update(duration, TimeUnit.MILLISECONDS)
      registry
        .timer(s"$prefix.${result.header.status / 100}xx.${rh.method}.$controller.$method")
        .update(duration, TimeUnit.MILLISECONDS)
      registry.timer(s"$prefix.${result.header.status / 100}xx.${rh.method}.ALL").update(duration, TimeUnit.MILLISECONDS)
      registry.timer(s"$prefix.${result.header.status / 100}xx.ALL.ALL").update(duration, TimeUnit.MILLISECONDS)

      if (recordExternalCallTimes) {
        val externalCallTimes = callTimer.readExternalCallTimes(rh.id).toMillis
        registry
          .timer(s"$selfPrefix.${result.header.status}.${rh.method}.$controller.$method")
          .update(duration - externalCallTimes, TimeUnit.MILLISECONDS)
        registry
          .timer(s"$selfPrefix.${result.header.status / 100}xx.${rh.method}.$controller.$method")
          .update(duration - externalCallTimes, TimeUnit.MILLISECONDS)
        registry
          .timer(s"$selfPrefix.${result.header.status / 100}xx.${rh.method}.ALL")
          .update(duration - externalCallTimes, TimeUnit.MILLISECONDS)
        registry
          .timer(s"$selfPrefix.${result.header.status / 100}xx.ALL.ALL")
          .update(duration - externalCallTimes, TimeUnit.MILLISECONDS)
      }

      activeRequests.dec()
      globalCtx.stop()
      result
    }

    activeRequests.inc()

    next(rh).transform(logCompleted, exception => {
      logCompleted(Results.InternalServerError)
      exception
    })
  }
}
