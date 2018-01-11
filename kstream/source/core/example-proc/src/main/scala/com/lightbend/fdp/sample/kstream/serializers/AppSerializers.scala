/**
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.fdp.sample.kstream
package serializers

import models.LogRecord
import org.apache.kafka.common.serialization.Serdes
import com.lightbend.kafka.scala.iq.serializers._

trait AppSerializers extends Serializers {
  final val ts = new Tuple2Serializer[String, String]()
  final val ms = new ModelSerializer[LogRecord]()
  final val logRecordSerde = Serdes.serdeFrom(ms, ms)
  final val tuple2StringSerde = Serdes.serdeFrom(ts, ts)
}
