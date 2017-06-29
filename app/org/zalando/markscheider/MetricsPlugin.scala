package org.zalando.markscheider

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.codahale.metrics.json.MetricsModule
import com.codahale.metrics.jvm.{ GarbageCollectorMetricSet, MemoryUsageGaugeSet, ThreadStatesGaugeSet }
import com.codahale.metrics.logback.InstrumentedAppender
import com.codahale.metrics.{ MetricRegistry, SharedMetricRegistries }
import com.fasterxml.jackson.databind.ObjectMapper
import play.api._
import play.api.inject.{ ApplicationLifecycle, Module }
import javax.inject._

import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.language.implicitConversions

@Singleton
class MetricsPlugin @Inject() (
    configuration: Configuration,
    lifecycle:     ApplicationLifecycle,
    registries:    MetricRegistries,
    stopper:       RegistryStopper
) {

  val validUnits = Set("NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS")

  val mapper: ObjectMapper = new ObjectMapper()

  implicit def stringToTimeUnit(s: String): TimeUnit = TimeUnit.valueOf(s)

  val registry = registries.getOrCreate

  lifecycle.addStopHook(() => Future.successful(stopper.stop()))
  onStart()

  def onStart(): Any = {
      def setupJvmMetrics(registry: MetricRegistry): Unit = {
        val jvmMetricsEnabled = configuration.getOptional[Boolean]("org.zalando.markscheider.jvm").getOrElse(true)
        if (jvmMetricsEnabled) {
          registry.registerAll(new GarbageCollectorMetricSet())
          registry.registerAll(new MemoryUsageGaugeSet())
          registry.registerAll(new ThreadStatesGaugeSet())
        }
      }

      def setupLogbackMetrics(registry: MetricRegistry): Unit = {
        val logbackEnabled = configuration.getOptional[Boolean]("org.zalando.markscheider.logback").getOrElse(true)
        if (logbackEnabled) {
          val appender: InstrumentedAppender = new InstrumentedAppender(registry)

          val factory = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
          val rootLogger = factory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

          appender.setContext(rootLogger.getLoggerContext)
          appender.start()
          rootLogger.addAppender(appender)
        }
      }

      def setupReporting(conf: Configuration, registry: MetricRegistry): Unit =
        Map(
          "console" -> Reporter.console _,
          "csv" -> Reporter.csv _
        ).foreach {
            case (name, fun) =>
              conf.getOptional[Configuration](name).foreach {
                conf =>
                  if (conf.getOptional[Boolean]("enabled").getOrElse(false)) {
                    fun(conf, registry)()
                  }
              }
          }

    if (enabled) {
      val rateUnit = configuration.getOptional[String]("org.zalando.markscheider.rateUnit").filter(validUnits.contains).getOrElse("SECONDS")
      val durationUnit = configuration.getOptional[String]("org.zalando.markscheider.durationUnit").filter(validUnits.contains).getOrElse("MILLISECONDS")
      val showSamples = configuration.getOptional[Boolean]("org.zalando.markscheider.showSamples").getOrElse(false)

      setupJvmMetrics(registry)
      setupLogbackMetrics(registry)
      setupReporting(configuration.getOptional[Configuration]("org.zalando.markscheider.reporting").getOrElse(Configuration.empty), registry)

      val module = new MetricsModule(rateUnit, durationUnit, showSamples)
      mapper.registerModule(module)
    }
  }

  def enabled: Boolean = configuration.getOptional[Boolean]("org.zalando.markscheider.enabled").getOrElse(true)
}

class RegistryStopper @Inject() (configuration: Configuration) {
  def stop(): Unit = SharedMetricRegistries.remove(configuration.getOptional[String]("org.zalando.markscheider.name").getOrElse("default"))
}

class MetricRegistries @Inject() (configuration: Configuration) {
  def getOrCreate: MetricRegistry =
    SharedMetricRegistries.getOrCreate(configuration.getOptional[String]("org.zalando.markscheider.name").getOrElse("default"))
}

class PlayMetricsModule extends Module {
  def bindings(
    environment:   Environment,
    configuration: Configuration
  ): Seq[play.api.inject.Binding[_]] = Seq(
    bind[MetricsPlugin].toSelf.eagerly(),
    bind[RegistryStopper].toSelf.eagerly(),
    bind[MetricRegistries].toSelf.eagerly(),
    bind[MetricsFilter].toSelf.eagerly()
  )
}
