package org.zalando.markscheider

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
}
