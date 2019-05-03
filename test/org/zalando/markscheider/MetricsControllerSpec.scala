package org.zalando.markscheider

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.{ HeaderNames, MimeTypes, Status }
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest

import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.test.Helpers._

class MetricsControllerSpec extends UnitSpec with GuiceOneAppPerSuite {

  val controller = app.injector.instanceOf(classOf[MetricsController])
  //matches lines such as 'http_requests_total{method="post",code="200"} 1027 1395066363000'
  val prometheusFormatRegex = """(?<metricname>[^{\s]+)(?<labels>\{[^}].*})?\s(?<timestamp>[0-9\.]+)\s(?<value>[0-9\.]+)?""".r

  "The metrics endpoint" should "answer with dropwizard JSON metrics without an accept header" in {
    val eventualResult = controller.metrics(FakeRequest())
    val result = Await.result(eventualResult, 5.seconds)
    assert(result.header.status == Status.OK)

    assert(result.body.contentType.contains("application/json"))
  }
  it should "answer with prometheus metrics without an accept header if that is configured" in {
    val configuredController = new MetricsController(
      app.injector.instanceOf(classOf[MetricsPlugin]),
      app.injector.instanceOf(classOf[Configuration]) ++ Configuration("org.zalando.markscheider.defaultFormat" -> "prometheus"),
      app.injector.instanceOf(classOf[ControllerComponents])
    )
    val eventualResult = configuredController.metrics(FakeRequest())
    val result = Await.result(eventualResult, 5.seconds)
    assert(result.header.status == Status.OK)

    assert(result.body.contentType.contains("text/plain; version=0.0.4"))
  }

  it should "answer with dropwizard JSON metrics with 'application/json' in the accept header" in {
    val eventualResult = controller.metrics(FakeRequest().withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON))
    val result = Await.result(eventualResult, 5.seconds)
    assert(result.header.status == Status.OK)

    assert(result.body.contentType.contains("application/json"))
  }
  it should "answer with prometheus metrics with 'text/plain' in the accept header" in {
    val eventualResult = controller.metrics(FakeRequest().withHeaders(HeaderNames.ACCEPT -> MimeTypes.TEXT))
    val result = Await.result(eventualResult, 5.seconds)
    assert(result.header.status == Status.OK)

    assert(result.body.contentType.contains("text/plain; version=0.0.4"))
    for { line <- contentAsString(eventualResult).lines if line.nonEmpty } {
      assert(prometheusFormatRegex.findFirstMatchIn(line).isDefined)
    }
  }
}
