# Model serving Akka Streams and Kafka Streams

# Overall Architecture

A high-level view of the overall model serving architecture (
similar to [dynamically controlled stream](https://data-artisans.com/blog/bettercloud-dynamic-alerting-apache-flink)) 
![Overall architecture of model serving](images/overallModelServing.png)


This architecture assumes two data streams - one containing data that needs to be scored, and one containing the model updates. The streaming engine contains the current model used for the actual scoring in memory. The results of scoring can be either delivered to the customer or used by the streaming engine internally as a new stream - input for additional calculations. If there is no model currently defined, the input data is dropped. When the new model is received, it is instantiated in memory, and when instantiation is complete, scoring is switched to a new model. The model stream can either contain the binary blob of the data itself or the reference to the model data stored externally (pass by reference) in a database or a filesystem, like HDFS or S3. 
Such approach effectively using model scoring as a new type of functional transformation, that can be used by any other stream functional transformations.
Although the overall architecture above is showing a single model, a single streaming engine could score multiple models simultaneously. 

# Akka Streams

Akka implementation is based on the usage of a custom stage, which is a fully type-safe way to encapsulate required functionality. 
The stage implements stream processor functionality from the overall architecture diagram. 
With such component in place, the overall implementation is going to look as follows:


![Akka streams model serving](images/Akkajoin.png)


# Kafka Streams

Kafka Streams implementation leverages custom store containing current execution state.
With this store in place, the implementation of the model serving using Kafka 
Streams becomes very simple, it’s basically two independent streams coordinated via a shared store. 


![Kafka streams model serving](images/kafkastreamsJoin.png)


# Queryable state

Kafka Streams  recently [introduced](https://docs.confluent.io/current/streams/developer-guide.html#id8) queryable state, which is 
a nice approach to execution monitoring.
This feature allows treating the stream processing layer as a 
lightweight embedded database and, more concretely, to directly query the latest state of your stream processing application, without needing to materialize that state to external databases or external storage first.


![Queriable state](images/queryablestate.png)

Both Akka Streams and Kafka streams implementation support queryable state

# Scaling

Both Akka and Kafka Streams implementations are in JVM implementations.
Given that the source of streams is Kafka, they both can be deployed as a cluster.
The Figure below shows Kafka Streams cluster. Akka Streams implementation can be scaled the same way

![scaling](images/Kafkastreamsclusters.png)


# Prerequisites

This application relies on Kafka (current version is 1.0) and requires a Kafka deployment available.
It uses 2 topics:
* `models_data` - topic used for sending data
* `models_models` - topic used for sending models
Model provider and data provider applications check if their corresponding topics exist. Run them 
first if not sure whether the topics exist.

It also relies on InfluxDB/Grafana for visualization. Both need to be installed before running applications

For InfluxDB, the database `serving` with retentionPolicy `default` is used. 
The application checks on startup whether the database exists and create it, if necessary.
The application also ensures that the Grafana data source and dashboard definitions exist and create them, if necessary.


# Build the code

We recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/) for managing and building the code. 
The project is organized as several modules:

* `akkaServer` - Akka Streams implementation of model serving
* `client` - Data and model loader used to run either Akka or Kafka streams application
* `configuration` - Shared configurations and InfluxDB support (see [prerequisites](#Prerequisites))
* `model` - Implementation of both Tensorflow anf PMML models.
* `protobufs` - Shared models in protobuf format.
* `server` -  Kafka Streams implementation of model serving.

Additionally, the following folders are used:

* `data` - some data files for running the applications
* `images` - diagrams used for this document

The build is done via SBT

    cd KafkaStreamsModelServer
    sbt compile
    # For IntelliJ users, just import a project and use IntelliJ commands

# Deploy and Run

This project contains 3 executables:
* `akkaserver`    - Akka Streams implementation of model serving
* `kafkaserver`   - Kafka Streams implementation of model serving
* `dataprovider`  - Data publisher

Each application can run either locally (on user's machine) or on the server.

## Running locally

Running locally can be done either using SBT or Intellij (If you run locally, make sure to change 
kafka configuration `(broker quarum and zookeeper)`), InfluxDB configuration `(host and port)` and 
Grafana configuration `(host and port)`

`dataprovider` application allow for changing of frequency of sending data, by
specifying desired frequency (in ms) as an application parameter. If the parameter is not specified
data is send once a sec and model - once every 5 mins.

Both `akkaserver` and `kafkaserver` implement queryable state. 

To query `akkaserver` state connect your browser to `host:5500/stats` to get statistics of the current execution
Currently `akkaserver` supports only statistics for a given server. If a cluster is used, each server needs to be
queried (with the same port)


To query `kafkaserver` state connect your browser to `host:8888`. This contains several URLs:
* `/state/instances` returns the list of instances participating in the cluster
* `/state/instances/{storeName}` returns the list of instances containing a store. Store name used 
in our application is `modelStore`
* `/state/{storeName}/value` returns current state of the model serving

## Running on the server
The applications are included in `fdp-package-sample-apps` docker image. See documentation there
for running them on the server




