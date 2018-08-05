/**
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.fdp.sample.kstream
package models

import java.time.OffsetDateTime

case class LogRecord(
  host: String, 
  clientId: String, 
  user: String, 
  timestamp: OffsetDateTime, 
  method: String,
  endpoint: String, 
  protocol: String, 
  httpReplyCode: Int, 
  payloadSize: Long
)
