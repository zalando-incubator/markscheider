package org.zalando.markscheider

import com.codahale.metrics.{ ExponentiallyDecayingReservoir, Histogram, Reservoir }

class PrometheusSupportSpec extends UnitSpec {
  "The serializer regex" should "match some of the basic strings given" in {
    import PrometheusSupport.{ DefaultMetricName => regex }
    assert(regex.findFirstMatchIn("zmon.self.response.2xx.GET.org.zalando.markscheider.MetricsController.metrics").isDefined)
    "zmon.self.response.2xx.GET.org.zalando.markscheider.MetricsController.metrics" match {
      case regex(prefix, responseCode, httpMethod, method) =>
        assert(prefix == "zmon.self.response")
        assert(responseCode == "2xx")
        assert(httpMethod == "GET")
        assert(method == "org.zalando.markscheider.MetricsController.metrics")
    }
  }

  "The format" should "match an example" in {
    val histogram = new Histogram(new ExponentiallyDecayingReservoir())
    histogram.update(0)
    histogram.update(10)
    histogram.update(20)
    histogram.update(30)
    new PrometheusSupport {
      val retVal = PrometheusMetric.fromSnapshot("foo", Map("some" -> "label"), histogram.getSnapshot)
      val string = retVal.filterNot(_.name.contains("stddev")).mkString("\n")
      assert(string == """foo_mean{some="label"} 15.0
                         |foo_max{some="label"} 30.0
                         |foo_min{some="label"} 0.0
                         |foo_bucket{some="label",le="0.5"} 20.0
                         |foo_bucket{some="label",le="0.75"} 30.0
                         |foo_bucket{some="label",le="0.95"} 30.0
                         |foo_bucket{some="label",le="0.98"} 30.0
                         |foo_bucket{some="label",le="0.99"} 30.0
                         |foo_bucket{some="label",le="0.999"} 30.0
                         |foo_count{some="label"} 4.0""".stripMargin)
    }
  }
}
