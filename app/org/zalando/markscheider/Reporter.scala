package org.zalando.markscheider

import java.io.File
import java.util.concurrent.TimeUnit

import com.codahale.metrics.{ ConsoleReporter, CsvReporter, MetricRegistry }
import play.api.{ Configuration, Logger }

object Reporter {
  def console(conf: Configuration, registry: MetricRegistry): () => Any = {
    for {
      unit <- conf.getString("unit")
      period <- conf.getInt("period")
      prefix <- conf.getString("prefix")
    } yield () => {
      Logger.info("Enabling ConsoleReporter")

      ConsoleReporter.forRegistry(registry)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .convertRatesTo(TimeUnit.SECONDS)
        .build().start(period, TimeUnit.valueOf(unit))
    }
  }.getOrElse(() => Unit)

  def csv(conf: Configuration, registry: MetricRegistry): () => Any = {
    for {
      outputDir <- conf.getString("output")
      unit <- conf.getString("unit")
      period <- conf.getInt("period")
      prefix <- conf.getString("prefix")
    } yield () => {
      Logger.info("Enabling CsvReporter")

      CsvReporter.forRegistry(registry)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .convertRatesTo(TimeUnit.SECONDS)
        .build(new File(outputDir)).start(period, TimeUnit.valueOf(unit))
    }
  }.getOrElse(() => Unit)

}
