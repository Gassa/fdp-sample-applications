package com.lightbend.fdp.sample.kstream
package services

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.{ ReadOnlyKeyValueStore, QueryableStoreTypes, QueryableStoreType, ReadOnlyWindowStore }

import scala.collection.JavaConverters._
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{ Try, Success, Failure }

class LocalStateStoreQuery[K, V] {
  def queryStateStore(streams: KafkaStreams, store: String, key: K)
    (implicit ex: ExecutionContext): Future[V] = Future {
    Try {
      val q: QueryableStoreType[ReadOnlyKeyValueStore[K, V]] = QueryableStoreTypes.keyValueStore()
      val localStore: ReadOnlyKeyValueStore[K, V] = streams.store(store, q)
      localStore.get(key)
    } match {
      case Success(s)  => s
      case Failure(ex) => throw ex
    }
  }

  def queryWindowedStateStore(streams: KafkaStreams, store: String, key: K, fromTime: Long, toTime: Long)
    (implicit ex: ExecutionContext): Future[List[(Long, V)]] = Future {
    Try {
      val q: QueryableStoreType[ReadOnlyWindowStore[K, V]] = QueryableStoreTypes.windowStore()
      val localStore: ReadOnlyWindowStore[K, V] = streams.store(store, q)
      localStore.fetch(key, fromTime, toTime).asScala.toList.map(kv => (Long2long(kv.key), kv.value))
    } match {
      case Success(s)  => s
      case Failure(ex) => throw ex
    }
  }
}
