package com.lightbend.fdp.sample.kstream
package serializers

import org.apache.kafka.common.serialization.{ Deserializer, Serde, Serdes, Serializer }

import java.util.Collections
import java.util.Map

class SpecificAvroSerde[T <: org.apache.avro.specific.SpecificRecord] extends Serde[T] {

  val inner: Serde[T] = Serdes.serdeFrom(new SpecificAvroSerializer[T](), new SpecificAvroDeserializer[T]()) 

  override def serializer(): Serializer[T] = inner.serializer()

  override def deserializer(): Deserializer[T] = inner.deserializer()

  override def configure(configs: Map[String, _], isKey: Boolean): Unit = {
    inner.serializer().configure(configs, isKey)
    inner.deserializer().configure(configs, isKey)
  }

  override def close(): Unit = {
    inner.serializer().close()
    inner.deserializer().close()
  }
}
