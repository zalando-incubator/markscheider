package org.zalando.markscheider

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry.name
import com.codahale.metrics._
import com.google.inject.Inject
import play.api.{ Configuration, Logger }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

class MetricsFilter @Inject() (registries: MetricRegistries, callTimer: CallTimer, configuration: Configuration) extends EssentialFilter {
  val registry = registries.getOrCreate
  val prefix = configuration.getString("org.zalando.markscheider.prefix.http").getOrElse("zmon.response")
  val selfPrefix = configuration.getString("org.zalando.markscheider.prefix.self-http").getOrElse("zmon.self.response")

  def requestsTimer: Timer = registry.timer(name(classOf[MetricsFilter], "requestTimer"))
  def activeRequests: Counter = registry.counter(name(classOf[MetricsFilter], "activeRequests"))

  def apply(next: EssentialAction): EssentialAction = new EssentialAction {

    def apply(rh: RequestHeader) = {
      val startTime = System.currentTimeMillis()
      val method = rh.tags.getOrElse(play.api.routing.Router.Tags.RouteActionMethod, "MetricsFilter")
      val controller = rh.tags.getOrElse(play.api.routing.Router.Tags.RouteController, getClass.getPackage.getName)

      val globalCtx = requestsTimer.time()

        def logCompleted(result: Result): Result = {
          val duration: Long = System.currentTimeMillis() - startTime

          registry.timer(s"$prefix.${result.header.status}.${rh.method}.$controller.$method")
            .update(duration, TimeUnit.MILLISECONDS)
          registry.timer(s"$prefix.${result.header.status / 100}xx.${rh.method}.$controller.$method")
            .update(duration, TimeUnit.MILLISECONDS)
          registry.timer(s"$prefix.${result.header.status / 100}xx.${rh.method}.ALL")
            .update(duration, TimeUnit.MILLISECONDS)
          registry.timer(s"$prefix.${result.header.status / 100}xx.ALL.ALL")
            .update(duration, TimeUnit.MILLISECONDS)

          val externalCallTimes = callTimer.readExternalCallTimes(rh.id).toMillis
          registry.timer(s"$selfPrefix.${result.header.status}.${rh.method}.$controller.$method")
            .update(duration - externalCallTimes, TimeUnit.MILLISECONDS)
          registry.timer(s"$selfPrefix.${result.header.status / 100}xx.${rh.method}.$controller.$method")
            .update(duration - externalCallTimes, TimeUnit.MILLISECONDS)
          registry.timer(s"$selfPrefix.${result.header.status / 100}xx.${rh.method}.ALL")
            .update(duration - externalCallTimes, TimeUnit.MILLISECONDS)
          registry.timer(s"$selfPrefix.${result.header.status / 100}xx.ALL.ALL")
            .update(duration - externalCallTimes, TimeUnit.MILLISECONDS)
          activeRequests.dec()
          globalCtx.stop()
          result
        }

      activeRequests.inc()
      next(rh).map(logCompleted)
    }
  }
}
