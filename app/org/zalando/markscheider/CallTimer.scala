package org.zalando.markscheider

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

import com.google.common.cache.{ CacheBuilder, CacheLoader, LoadingCache }
import play.api.Configuration
import play.api.http.Status
import play.api.mvc.{ AnyContent, Request }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

/**
  * Object used to record and retrieve results for external system calls. There will be a timer that shows response times
  * without external services, as well as record the time used for this service.
  */
class CallTimer @Inject() (registries: MetricRegistries, configuration: Configuration) {
  val registry = registries.getOrCreate
  val externalHttpPrefix = configuration.getString("org.zalando.markscheider.prefix.external").getOrElse("zmon.external")
  val externalNonHttpPrefix = configuration.getString("org.zalando.markscheider.prefix.non-http-external").getOrElse("zmon.nonhttp-external")

  //does not compile with scala.Long oO. Why?
  private val cache: LoadingCache[java.lang.Long, AtomicLong] = CacheBuilder.newBuilder()
    .expireAfterWrite(2, TimeUnit.MINUTES)
    .build(new CacheLoader[java.lang.Long, AtomicLong] {
      override def load(k: java.lang.Long): AtomicLong = new AtomicLong()
    })

  /**
    * Helper function that measures the time needed for a given future to complete, taking this as an external call. Used only for HTTP calls returning
    * a numeric response code. Failed futures will not record anything, you need to take care to recover yourself beforehand.
    *
    * @param   requestId             The request id of the call, if any
    * @param   name                  the name of the external service
    * @param   httpMethod            the http method that the call uses
    * @param   responseCodeFunction  a function that can map from the data of the result of the call to an http result code
    * @param   call                  the call that should be performed
    * @tparam  T                     The type that the call returns
    *
    * @return  A Future that also measures the time needed to be completed
    */
  def measureExternalHttpCall[T](
    requestId:            Option[Long],
    name:                 String       = "external-service",
    httpMethod:           String       = "GET",
    responseCodeFunction: T => Int     = { data: T => Status.OK }
  )(call: Future[T])(implicit executionContext: ExecutionContext): Future[T] = {
    val startTime = System.currentTimeMillis()
    call.onFailure{ //at least, now failures are being recorded somewhere.
      case ex: Throwable =>
        val duration = System.currentTimeMillis() - startTime
        recordExternalNonHttpCall(requestId, duration.milliseconds, name, httpMethod, "FAILED")
    }
    call.map{ data =>
      val duration = System.currentTimeMillis() - startTime
      recordExternalHttpCall(requestId, duration.milliseconds, name, httpMethod, responseCodeFunction(data))
      data
    }
  }

  /** Used to record an external http call that has been performed without using the Future that created it. May be useful if either your call is synchronous.*/
  def recordExternalHttpCall(
    requestId:    Option[Long],
    duration:     Duration,
    name:         String       = "external-service",
    httpMethod:   String       = "GET",
    responseCode: Int          = Status.OK
  ): Unit = {
    requestId.map { requestId =>
      val externalRequestTimer = cache.get(requestId)
      externalRequestTimer.getAndAdd(duration.toMillis)
    }
    registry //normal response code handling
      .timer(s"$externalHttpPrefix.$responseCode.$httpMethod.$name")
      .update(duration.toMillis, TimeUnit.MILLISECONDS)
    registry //aggregating all 2xx, 3xx, 4xx error code groups per endpoint
      .timer(s"$externalHttpPrefix.${responseCode / 100}xx.$httpMethod.$name")
      .update(duration.toMillis, TimeUnit.MILLISECONDS)
    registry //aggregating all 2xx, 3xx, 4xx error code groups over all endpoints per http method
      .timer(s"$externalHttpPrefix.${responseCode / 100}xx.$httpMethod.ALL")
      .update(duration.toMillis, TimeUnit.MILLISECONDS)
    registry //aggregating all 2xx, 3xx, 4xx error code groups over all endpoints
      .timer(s"$externalHttpPrefix.${responseCode / 100}xx.ALL.ALL")
      .update(duration.toMillis, TimeUnit.MILLISECONDS)
  }

  /**
    * Helper function that measures the time needed for a given future to complete, taking this as an external call. Used only for non-HTTP calls. These calls
    * may have any response code type. Failed futures will not record anything, you need to take care to recover yourself beforehand.
    *
    * @param   requestId             The request id of the call, if any
    * @param   name                  the name of the external service
    * @param   method                the method that the call uses. Defaults to "READ", may be any string. Proposal: READ / WRITE / EXECUTE.
    * @param   responseCodeFunction  a function that can map from the data of the result of the call to an http result code
    * @param   call                  the call that should be performed
    * @tparam  T                     The type that the call returns
    *
    * @return  A Future that also measures the time needed to be completed
    */
  def measureExternalNonHttpCall[T](
    requestId:            Option[Long],
    name:                 String       = "external-service",
    method:               String       = "READ",
    responseCodeFunction: T => String  = { data: T => "OK" }
  )(call: Future[T])(implicit executionContext: ExecutionContext): Future[T] = {
    val startTime = System.currentTimeMillis()
    call.onFailure{ //at least, now failures are being recorded somewhere.
      case ex: Throwable =>
        val duration = System.currentTimeMillis() - startTime
        recordExternalNonHttpCall(requestId, duration.milliseconds, name, method, "FAILED")
    }
    call.map{ data =>
      val duration = System.currentTimeMillis() - startTime
      recordExternalNonHttpCall(requestId, duration.milliseconds, name, method, responseCodeFunction(data))
      data
    }
  }

  /** Used to record an external call that has been performed without using the Future that created it. May be useful if either your call is synchronous.*/
  def recordExternalNonHttpCall(
    requestId:    Option[Long],
    duration:     Duration,
    name:         String       = "external-service",
    method:       String       = "READ",
    responseCode: String       = "OK"
  ): Unit = {
    requestId.map { requestId =>
      val externalRequestTimer = cache.get(requestId)
      externalRequestTimer.getAndAdd(duration.toMillis)
    }
    registry
      .timer(s"$externalNonHttpPrefix.$responseCode.$method.$name")
      .update(duration.toMillis, TimeUnit.MILLISECONDS)
  }

  /** Reads aggregated external call times that have been recorded with above functions and a given request id.*/
  def readExternalCallTimes(requestId: Long): Duration = cache.get(requestId).get().milliseconds
}
