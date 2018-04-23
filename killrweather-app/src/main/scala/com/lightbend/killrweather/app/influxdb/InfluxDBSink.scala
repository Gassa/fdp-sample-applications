package com.lightbend.killrweather.app.influxdb

import java.util.concurrent.TimeUnit

import com.lightbend.killrweather.WeatherClient.WeatherRecord
import com.lightbend.killrweather.grafana.GrafanaSetup
import com.lightbend.killrweather.settings.WeatherSettings
import com.lightbend.killrweather.utils.{DailyTemperature, MonthlyTemperature}
import org.influxdb.dto.{Point, Query}
import org.influxdb.{InfluxDB, InfluxDBFactory}
import collection.JavaConverters._

class InfluxDBSink(createWriter: () => InfluxDB) extends Serializable {

  lazy val influxDB = createWriter()

  def write(raw: WeatherRecord): Unit = {
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
    write(rawPoint.build())
  }

  def write(dailyTemp: DailyTemperature): Unit = {
    val dailyTempPoint = Point.measurement("daily_temp_weather").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    dailyTempPoint.addField("year", dailyTemp.year.toLong)
    dailyTempPoint.addField("month", dailyTemp.month.toLong)
    dailyTempPoint.addField("day", dailyTemp.day.toLong)
    dailyTempPoint.addField("high", dailyTemp.high)
    dailyTempPoint.addField("low", dailyTemp.low)
    dailyTempPoint.addField("mean", dailyTemp.mean)
    dailyTempPoint.addField("variance", dailyTemp.variance)
    dailyTempPoint.addField("stdev", dailyTemp.stdev)
    dailyTempPoint.tag("station", dailyTemp.wsid)
    write(dailyTempPoint.build())
  }

  def write(monthlyTemp: MonthlyTemperature): Unit = {
    val monthlyTempPoint = Point.measurement("monthly_temp_weather").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    monthlyTempPoint.addField("year", monthlyTemp.year.toLong)
    monthlyTempPoint.addField("month", monthlyTemp.month.toLong)
    monthlyTempPoint.addField("high", monthlyTemp.high)
    monthlyTempPoint.addField("low", monthlyTemp.low)
    monthlyTempPoint.addField("mean", monthlyTemp.mean)
    monthlyTempPoint.addField("variance", monthlyTemp.variance)
    monthlyTempPoint.addField("stdev", monthlyTemp.stdev)
    monthlyTempPoint.tag("station", monthlyTemp.wsid)
    write(monthlyTempPoint.build())
  }

  private def write(point: Point): Unit = {
    try {
      if (InfluxDBSink.useInfluxDB) influxDB.write(point)
      println(s"written to influx $point")  // TODO replace with a debug log statement.
    } catch { case t: Throwable => println(s"Exception writing to Influx $t") }
  }
}

object InfluxDBSink {

  val settings = WeatherSettings()
  import settings._

  //TODO: this access is wrong. We should not export config from this context
  def useInfluxDB = influxConfig.enabled

  // TODO the implementation is a bit messy.
  def apply(): InfluxDBSink =
    if (influxConfig.enabled) {
      make()
    }
    else makeNull()

  def make(): InfluxDBSink = {
    val f = () => {
      val influxDB = InfluxDBFactory.connect(influxConfig.url, influxConfig.user, influxConfig.password)
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

      sys.addShutdownHook {
        influxDB.flush()
        influxDB.close()
      }
      influxDB
    }
    try {
      val graphanaConfig = settings.graphanaConfig
      new GrafanaSetup(graphanaConfig.port.toString, graphanaConfig.server).setGrafana()
    } catch {
      case t: Throwable => println("Grafana not initialized")
    }
    new InfluxDBSink(f)
  }

  def makeNull(): InfluxDBSink =
    new InfluxDBSink(() => null.asInstanceOf[InfluxDB])
}
