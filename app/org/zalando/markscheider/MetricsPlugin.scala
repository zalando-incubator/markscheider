package org.zalando.markscheider

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import ch.qos.logback.classic
import com.codahale.metrics.json.MetricsModule
import com.codahale.metrics.jvm.{ GarbageCollectorMetricSet, MemoryUsageGaugeSet, ThreadStatesGaugeSet }
import com.codahale.metrics.logback.InstrumentedAppender
import com.codahale.metrics.{ MetricRegistry, SharedMetricRegistries }
import com.fasterxml.jackson.databind.ObjectMapper
import play.api._
import play.api.inject.{ ApplicationLifecycle, Module }
import javax.inject._
import scala.concurrent.Future
import scala.language.implicitConversions

@Singleton
class MetricsPlugin @Inject() (
    configuration: Configuration,
    lifecycle:     ApplicationLifecycle,
    registries:    MetricRegistries,
    stopper:       RegistryStopper
) {

  val validUnits = Some(Set("NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS"))

  val mapper: ObjectMapper = new ObjectMapper()

  implicit def stringToTimeUnit(s: String): TimeUnit = TimeUnit.valueOf(s)

  val registry = registries.getOrCreate

  lifecycle.addStopHook(() => Future.successful(stopper.stop()))
  onStart()

  def onStart(): Any = {
      def setupJvmMetrics(registry: MetricRegistry): Unit = {
        val jvmMetricsEnabled = configuration.getBoolean("org.zalando.markscheider.jvm").getOrElse(true)
        if (jvmMetricsEnabled) {
          registry.registerAll(new GarbageCollectorMetricSet())
          registry.registerAll(new MemoryUsageGaugeSet())
          registry.registerAll(new ThreadStatesGaugeSet())
        }
      }

      def setupLogbackMetrics(registry: MetricRegistry): Unit = {
        val logbackEnabled = configuration.getBoolean("org.zalando.markscheider.logback").getOrElse(true)
        if (logbackEnabled) {
          val appender: InstrumentedAppender = new InstrumentedAppender(registry)

          val logger: classic.Logger = Logger.logger.asInstanceOf[classic.Logger]
          appender.setContext(logger.getLoggerContext)
          appender.start()
          logger.addAppender(appender)
        }
      }

      def setupReporting(conf: Configuration, registry: MetricRegistry): Unit =
        Map(
          "console" -> Reporter.console _,
          "csv" -> Reporter.csv _
        ).foreach {
            case (name, fun) =>
              conf.getConfig(name).foreach {
                conf =>
                  if (conf.getBoolean("enabled").getOrElse(false)) {
                    fun(conf, registry)()
                  }
              }
          }

    if (enabled) {
      val rateUnit = configuration.getString("org.zalando.markscheider.rateUnit", validUnits).getOrElse("SECONDS")
      val durationUnit = configuration.getString("org.zalando.markscheider.durationUnit", validUnits).getOrElse("MILLISECONDS")
      val showSamples = configuration.getBoolean("org.zalando.markscheider.showSamples").getOrElse(false)

      setupJvmMetrics(registry)
      setupLogbackMetrics(registry)
      setupReporting(configuration.getConfig("org.zalando.markscheider.reporting").getOrElse(Configuration.empty), registry)

      val module = new MetricsModule(rateUnit, durationUnit, showSamples)
      mapper.registerModule(module)
    }
  }

  def enabled: Boolean = configuration.getBoolean("org.zalando.markscheider.enabled").getOrElse(true)
}

class RegistryStopper @Inject() (configuration: Configuration) {
  def stop(): Unit = SharedMetricRegistries.remove(configuration.getString("org.zalando.markscheider.name").getOrElse("default"))
}

class MetricRegistries @Inject() (configuration: Configuration) {
  def getOrCreate: MetricRegistry =
    SharedMetricRegistries.getOrCreate(configuration.getString("org.zalando.markscheider.name").getOrElse("default"))
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
