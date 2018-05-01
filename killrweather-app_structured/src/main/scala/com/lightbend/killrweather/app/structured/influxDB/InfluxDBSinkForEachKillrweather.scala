package com.lightbend.killrweather.app.structured.influxDB

import java.util.concurrent.TimeUnit

import com.lightbend.killrweather.WeatherClient.WeatherRecord
import com.lightbend.killrweather.settings.WeatherSettings
import com.lightbend.killrweather.utils.{DailyWeatherData, MonthlyWeatherData}
import org.apache.spark.sql.ForeachWriter
import org.influxdb.{InfluxDB, InfluxDBFactory}
import org.influxdb.dto.{Point, Query}
import collection.JavaConverters._

class InfluxDBSinkForEachKillrweatherRaw extends ForeachWriter[WeatherRecord] {

  val settings = WeatherSettings()

  import settings._

  var influxDB: InfluxDB = null

  override def open(partitionId: Long, version: Long): Boolean = {
    //    influxDB = InfluxDBFactory.connect(s"$influxDBServer:$influxDBPort", influxDBUser, influxDBPass)

    influxDB = InfluxDBFactory.connect(influxConfig.url, influxConfig.user, influxConfig.password)
    val databasesQuery = new Query("SHOW DATABASES","")
    val database_exists = influxDB.query(databasesQuery).getResults.get(0).getSeries.get(0).getValues match {
      case databases if databases == null => false
      case databases =>
        val names = databases.asScala.map(_.get(0).toString())
        if(names.contains(influxTableConfig.database)) true else false
    }
    if(!database_exists){
      val databaseCreateQuery = new Query(s"""CREATE DATABASE "${influxTableConfig.database}"""","")
      influxDB.query(databaseCreateQuery)
      val dropRetentionQuery = new Query("""DROP RETENTION POLICY "autogen"""",influxTableConfig.database)
      influxDB.query(dropRetentionQuery)
      val createRetentionQuery = new Query(s"""CREATE RETENTION POLICY "${influxTableConfig.retentionPolicy}" DURATION 1d SHARD DURATION 30m REPLICATION 1  DEFAULT""",influxTableConfig.database)
      influxDB.query(createRetentionQuery)
    }

    influxDB.setDatabase(influxTableConfig.database)
    // Flush every 2000 Points, at least every 100ms
    influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS)
    // set retention policy
    influxDB.setRetentionPolicy(influxTableConfig.retentionPolicy)
    //    println(s"InfluxDB opening raw with connector $influxDB")
    true
  }

  override def process(point: WeatherRecord): Unit = {
    //    println(s"InfluxDB writing raw with connector $influxDB")
    influxDB.write(converRaw(point))
  }

  override def close(errorOrNull: Throwable): Unit = {
    //    println(s"InfluxDB closing raw with connector $influxDB")
    if (influxDB != null) {
      influxDB.flush()
      influxDB.close()
    }
  }

  def converRaw(raw: WeatherRecord): Point = {
    val rawPoint = Point.measurement("raw_weather").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    rawPoint.addField("year", raw.year.toLong)
    rawPoint.addField("month", raw.month.toLong)
    rawPoint.addField("day", raw.day.toLong)
    rawPoint.addField("hour", raw.hour.toLong)
    rawPoint.addField("temperature", raw.temperature)
    rawPoint.addField("dewpoint", raw.dewpoint)
    rawPoint.addField("pressure", raw.pressure)
    rawPoint.addField("windDirection", raw.windDirection.toLong)
    rawPoint.addField("windSpeed", raw.windSpeed)
    rawPoint.addField("skyConditions", raw.skyCondition.toLong)
    rawPoint.tag("station", raw.wsid)
    rawPoint.build()
  }
}

class InfluxDBSinkForEachKillrweatherDaily extends ForeachWriter[DailyWeatherData] {
  //TODO: Review propagation of config
  val settings = WeatherSettings()

  import settings._

  var influxDB: InfluxDB = null
  //TODO: Review DRY
  override def open(partitionId: Long, version: Long): Boolean = {
    influxDB = InfluxDBFactory.connect(influxConfig.url, influxConfig.user, influxConfig.password)
    val databasesQuery = new Query("SHOW DATABASES","")
    val database_exists = influxDB.query(databasesQuery).getResults.get(0).getSeries.get(0).getValues match {
      case databases if databases == null => false
      case databases =>
        val names = databases.asScala.map(_.get(0).toString())
        if(names.contains(influxTableConfig.database)) true else false
    }
    if(!database_exists){
      val databaseCreateQuery = new Query(s"""CREATE DATABASE "${influxTableConfig.database}"""","")
      influxDB.query(databaseCreateQuery)
      val dropRetentionQuery = new Query("""DROP RETENTION POLICY "autogen"""",influxTableConfig.database)
      influxDB.query(dropRetentionQuery)
      val createRetentionQuery = new Query(s"""CREATE RETENTION POLICY "${influxTableConfig.retentionPolicy}" DURATION 1d SHARD DURATION 30m REPLICATION 1  DEFAULT""",influxTableConfig.database)
      influxDB.query(createRetentionQuery)
    }

    influxDB.setDatabase(influxTableConfig.database)
    // Flush every 2000 Points, at least every 100ms
    influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS)
    // set retention policy
    influxDB.setRetentionPolicy(influxTableConfig.retentionPolicy)
    true
  }

  override def process(point: DailyWeatherData): Unit = {
    influxDB.write(converDaily(point))
  }

  override def close(errorOrNull: Throwable): Unit = {
    if (influxDB != null) {
      influxDB.flush()
      influxDB.close()
    }
  }

  def converDaily(dailyTemp: DailyWeatherData): Point = {
    val dailyTempPoint = Point.measurement("daily_temp_weather").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    dailyTempPoint.addField("year", dailyTemp.year.toLong)
    dailyTempPoint.addField("month", dailyTemp.month.toLong)
    dailyTempPoint.addField("day", dailyTemp.day.toLong)
    dailyTempPoint.addField("high", dailyTemp.highTemp)
    dailyTempPoint.addField("low", dailyTemp.lowTemp)
    dailyTempPoint.addField("mean", dailyTemp.meanTemp)
    dailyTempPoint.addField("variance", dailyTemp.varianceTemp)
    dailyTempPoint.addField("stdev", dailyTemp.stdevTemp)
    dailyTempPoint.tag("station", dailyTemp.wsid)
    dailyTempPoint.build()
  }
}

class InfluxDBSinkForEachKillrweatherMonthly extends ForeachWriter[MonthlyWeatherData] {

  val settings = WeatherSettings()

  import settings._

  var influxDB: InfluxDB = null
  // TODO: Really cleanup repetition here!
  override def open(partitionId: Long, version: Long): Boolean = {
    influxDB = InfluxDBFactory.connect(influxConfig.url, influxConfig.user, influxConfig.password)
    val databasesQuery = new Query("SHOW DATABASES","")
    val database_exists = influxDB.query(databasesQuery).getResults.get(0).getSeries.get(0).getValues match {
      case databases if databases == null => false
      case databases =>
        val names = databases.asScala.map(_.get(0).toString())
        if(names.contains(influxTableConfig.database)) true else false
    }
    if(!database_exists){
      val databaseCreateQuery = new Query(s"""CREATE DATABASE "${influxTableConfig.database}"""","")
      influxDB.query(databaseCreateQuery)
      val dropRetentionQuery = new Query(s"""DROP RETENTION POLICY "autogen" ON "${influxTableConfig.database}" """,influxTableConfig.database)
      influxDB.query(dropRetentionQuery)
      val createRetentionQuery = new Query(s"""CREATE RETENTION POLICY "${influxTableConfig.retentionPolicy}" ON "${influxTableConfig.database}" DURATION 1d SHARD DURATION 30m REPLICATION 1  DEFAULT""",influxTableConfig.database)
      influxDB.query(createRetentionQuery)
    }

    influxDB.setDatabase(influxTableConfig.database)
    // Flush every 2000 Points, at least every 100ms
    influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS)
    // set retention policy
    influxDB.setRetentionPolicy(influxTableConfig.retentionPolicy)
    true
  }

  override def process(point: MonthlyWeatherData): Unit = {
    influxDB.write(convertMonthly(point))
  }

  override def close(errorOrNull: Throwable): Unit = {
    if (influxDB != null) {
      influxDB.flush()
      influxDB.close()
    }
  }

  def convertMonthly(monthlyTemp: MonthlyWeatherData): Point = {
    val monthlyTempPoint = Point.measurement("monthly_temp_weather").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    monthlyTempPoint.addField("year", monthlyTemp.year.toLong)
    monthlyTempPoint.addField("month", monthlyTemp.month.toLong)
    monthlyTempPoint.addField("high", monthlyTemp.highTemp)
    monthlyTempPoint.addField("low", monthlyTemp.lowTemp)
    monthlyTempPoint.addField("mean", monthlyTemp.meanTemp)
    monthlyTempPoint.addField("variance", monthlyTemp.varianceTemp)
    monthlyTempPoint.addField("stdev", monthlyTemp.stdevTemp)
    monthlyTempPoint.tag("station", monthlyTemp.wsid)
    monthlyTempPoint.build()
  }
}

