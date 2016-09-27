[![Build Status](https://travis-ci.org/zalando-incubator/markscheider.svg?branch=master)](https://travis-ci.org/zalando-incubator/markscheider)
[![Coverage Status](https://coveralls.io/repos/github/zalando-incubator/markscheider/badge.svg?branch=master)](https://coveralls.io/github/markscheider/markscheider?branch=master)
[![Apache licensed](https://img.shields.io/badge/license-Apache-green.svg)](https://raw.githubusercontent.com/zalando-incubator/markscheider/master/LICENSE)

# Markscheider

This play module provides some support for @codahale [Metrics](https://dropwizard.github.io/metrics/3.1.0/) library in a Play2 application (Scala).
It allows you to get insights about the state of your application, for example how response times behave, with which HTTP codes your service responds,
how many log entries are written.

The name stems from the german mining term [Markscheider](https://de.wikipedia.org/wiki/Markscheider), which was a land surveyor who was responsible
for mapping of the mine.

## Getting started

Add metrics-play dependency in your `build.sbt`:

```scala
libraryDependencies += Seq(
  "org.zalando" %% "markscheider" % "2.5.2"
)
```

To enable the plugin, add to `conf/application.conf`:

     play.modules.enabled += "org.zalando.markscheider.PlayMetricsModule"

Then add a class `Filters` as described in [The Play docs](https://www.playframework.com/documentation/2.5.x/ScalaHttpFilters) to
include the filter in your filter chain.

```tut:silent
import javax.inject.Inject
import play.api.http.DefaultHttpFilters
import org.zalando.markscheider.MetricsFilter

class Filters @Inject() (
  metricsFilter: MetricsFilter
) extends DefaultHttpFilters(metricsFilter)
```

You may want to have an endpoint that delivers the metrics, which you can add via

    GET        /metrics                        org.zalando.markscheider.MetricsController.metrics

in your `routes` file.

The metrics are created in a way that is compatible with [ZMON](https://github.com/zalando/zmon).

After that, you can get metrics information in your service at `/metrics`. By default, that endpoint is unsecured (no authentication in place).
For an example output of the endpoint, see `example-output.json`.


#### Configuration
The basic configuration is supported through the default configuration file, see `conf/reference.conf`. You can override
the settings in your `application.conf`. Otherwise the default settings are used.

_Note_: the namespace for the configuration is `org.zalando.markscheider`.

## Credits
This lib was adapted from https://github.com/fr3akX/metrics-play, which contains code from other sources as well.

## Contact
Feel free to drop me a line at lena.brueder@zalando.de.

## License
This code is released under the Apache Public License 2.0.
