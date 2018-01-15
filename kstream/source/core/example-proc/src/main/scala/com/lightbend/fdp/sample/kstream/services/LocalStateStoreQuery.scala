/**
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.fdp.sample.kstream
package services

import org.apache.kafka.streams.KafkaStreams

import scala.collection.JavaConverters._
import scala.concurrent.{Future, ExecutionContext}
import akka.actor.ActorSystem

import processor.{ BFStore, ReadableBFStore, BFStoreType }
import com.twitter.algebird.Hash128

import com.lightbend.kafka.scala.iq.services.LocalStateStoreQuery

class AppStateStoreQuery[K, V] extends LocalStateStoreQuery[K, V] {

  def queryBFStore(streams: KafkaStreams, store: String, value: K)
    (implicit ex: ExecutionContext, mk: Hash128[K], as: ActorSystem): Future[Boolean] = {

    val q = new BFStoreType[K]()(mk)
    retry(streams.store(store, q), DelayBetweenRetries, MaxRetryCount)(ex, as.scheduler).map(_.read(value))
  }
}
