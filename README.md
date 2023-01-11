<div align="center">

![Memphis light logo](https://github.com/memphisdev/memphis-broker/blob/master/logo-white.png?raw=true#gh-dark-mode-only)

</div>

<div align="center">

![Memphis light logo](https://github.com/memphisdev/memphis-broker/blob/master/logo-black.png?raw=true#gh-light-mode-only)

</div>

<div align="center">
<h4>Simple as RabbitMQ, Robust as Apache Kafka, and Perfect for microservices.</h4>

<img width="750" alt="Memphis UI" src="https://user-images.githubusercontent.com/70286779/204081372-186aae7b-a387-4253-83d1-b07dff69b3d0.png"><br>


<a href="https://landscape.cncf.io/?selected=memphis"><img width="200" alt="CNCF Silver Member" src="https://github.com/cncf/artwork/raw/master/other/cncf-member/silver/white/cncf-member-silver-white.svg#gh-dark-mode-only"></a>

</div>

<div align="center">

  <img width="200" alt="CNCF Silver Member" src="https://github.com/cncf/artwork/raw/master/other/cncf-member/silver/color/cncf-member-silver-color.svg#gh-light-mode-only">

</div>

 <p align="center">
  <a href="https://sandbox.memphis.dev/" target="_blank">Sandbox</a> - <a href="https://memphis.dev/docs/">Docs</a> - <a href="https://twitter.com/Memphis_Dev">Twitter</a> - <a href="https://www.youtube.com/channel/UCVdMDLCSxXOqtgrBaRUHKKg">YouTube</a>
</p>

<p align="center">
<a href="https://discord.gg/WZpysvAeTf"><img src="https://img.shields.io/discord/963333392844328961?color=6557ff&label=discord" alt="Discord"></a>
<a href="https://github.com/memphisdev/memphis-broker/issues?q=is%3Aissue+is%3Aclosed"><img src="https://img.shields.io/github/issues-closed/memphisdev/memphis-broker?color=6557ff"></a> 
<a href="https://github.com/memphisdev/memphis-broker/blob/master/CODE_OF_CONDUCT.md"><img src="https://img.shields.io/badge/Code%20of%20Conduct-v1.0-ff69b4.svg?color=ffc633" alt="Code Of Conduct"></a> 
<a href="https://docs.memphis.dev/memphis/release-notes/releases/v0.4.2-beta"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/memphisdev/memphis-broker?color=61dfc6"></a>
<img src="https://img.shields.io/github/last-commit/memphisdev/memphis-broker?color=61dfc6&label=last%20commit">
</p>

**[Memphis](https://memphis.dev)** is a next-generation message broker.<br>
A simple, robust, and durable cloud-native message broker wrapped with<br>
an entire ecosystem that enables fast and reliable development of next-generation event-driven use cases.<br><br>
Memphis enables building next-generation applications that require large volumes of streamed and enriched data,<br>
modern protocols, zero ops, rapid development, extreme cost reduction,<br>
and a significantly lower amount of dev time for data-oriented developers and data engineers.
<br><br>
Creator: [@adrianNEMO](https://github.com/nemoengineering/memphis.kt/commits?author=adrianNEMO)<br>
Maintainer: [@adrianNEMO](https://github.com/nemoengineering/memphis.kt/commits?author=adrianNEMO), [@Memphis team](https://github.com/memphisdev)

# Installation
After installing and running memphis broker,<br>
Add to the dependencies in your gradle file

Currently, releases are published to jitpack: [jitpack.io/#memphisdev/memphis.kt](https://jitpack.io/#memphisdev/memphis.kt)

```kotlin
 repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.memphisdev:memphis.kt:<version>")
}
```

# Importing
```kotlin
import dev.memphis.sdk.Memphis
```

### Connecting to Memphis
```kotlin
val memphis = Memphis.connect("<memphis-host>", "<application type username>", "<broker-token>")
```
<br>
It is possible to pass connection configuration parameters.

```kotlin
val memphis = Memphis.connect("<memphis-host>", "<application type username>", "<broker-token>") {
    port = 6666
    autoReconnect = true
    maxReconnects = 3
    reconnectWait = 5.seconds
    connectionTimeout = 15.seconds
            
}
```

Once connected, all features offered by Memphis are available.<br>

### Disconnecting from Memphis
To disconnect from Memphis, call Close() on the Memphis connection object.<br>

```kotlin
memphis.close()
```

### Creating a Station
Stations can be created from Conn<br>
Passing optional parameters<br>
_If a station already exists nothing happens, the new configuration will not be applied_<br>

```kotlin
val station = memphis.createStation("<station-name>")

val station = memphis.createStation("<station-name>") {
    retentionType = RetentionType.MAX_AGE_SECONDS
    retentionValue = 604800
    storageType = StorageType.DISK
    replicas = 1
    idempotencyWindow = 2.minutes
    schemaName = "<Schema Name>"
    sendPoisonMsgToDls = true
    sendSchemaFailedMsgToDls = true
}
```

### Retention Types
Memphis currently supports the following types of retention:<br>

```kotlin
RetentionType.MAX_AGE_SECONDS
```

The above means that every message persists for the value set in the retention value field (in seconds).

```kotlin
RetentionType.MESSAGES
```

The above means that after the maximum number of saved messages (set in retention value)<br>has been reached, the oldest messages will be deleted.

```kotlin
RetentionType.BYTES
```

The above means that after maximum number of saved bytes (set in retention value)<br>has been reached, the oldest messages will be deleted.

### Storage Types
Memphis currently supports the following types of messages storage:<br>

```kotlin
StorageType.DISK
```

The above means that messages persist on disk.

```kotlin
StorageType.MEMORY
```

The above means that messages persist on the main memory.<br>

### Destroying a Station
Destroying a station will remove all its resources (including producers and consumers).<br>

```kotlin
station.Destroy()
```

### Attaching a Schema to an Existing Station

```kotlin
memphis.attachSchema("<schema-name>", "<station-name>")

// Or from a station

station.attachSchema("<schema-name>")
```

### Detaching a Schema from Station

```kotlin
memphis.detachSchema("<station-name>")

// Or from a station
station.detachSchema()
```

### Produce and Consume Messages
The most common client operations are producing messages and consuming messages.<br><br>
Messages are published to a station and consumed from it<br>by creating a consumer and consuming the resulting flow.<br>Consumers are pull-based and consume all the messages in a station<br> unless you are using a consumers group,<br>in which case messages are spread across all members in this group.<br><br>
Memphis messages are payload agnostic. Payloads are `ByteArray`.<br><br>
In order to stop receiving messages, you have to call ```consumer.stopConsuming()```.<br>The consumer will terminate regardless of whether there are messages in flight for the client.

### Creating a Producer

```kotlin
val producer = memphis.producer("<station-name>", "<producer-name>") {
    genUniqueSuffix = false
}
```

### Producing a message

```kotlin
producer.produce("<message in ByteArray or (schema validated station - protobuf) or ByteArray(schema validated station - json schema) or ByteArray (schema validated station - graphql schema)>") {
    ackWait = 15.seconds
    messageId = "<message Id>"
}
```

### Add headers

```kotlin
producer.produce("<message in ByteArray or (schema validated station - protobuf) or ByteArray(schema validated station - json schema) or ByteArray (schema validated station - graphql schema)>") {
    headers.put("key", "value")
}
```

### Async produce
Meaning your application won't wait for broker acknowledgement - use only in case you are tolerant for data loss

```kotlin
producer.produceAsync("<message in ByteArray or (schema validated station - protobuf) or ByteArray(schema validated station - json schema) or ByteArray (schema validated station - graphql schema)>")
```

### Message ID
Stations are idempotent by default for 2 minutes (can be configured), Idempotency achieved by adding a message id

```kotlin
producer.produce("<message in ByteArray or (schema validated station - protobuf) or ByteArray(schema validated station - json schema) or ByteArray (schema validated station - graphql schema)>") {
    messageId = "123"
}
```

### Destroying a Producer

```kotlin
producer.destroy()
```

### Creating a Consumer

```kotlin
val consumer = memphis.consumer("<station-name>", "<consumer-name>") {
    consumerGroup = "<consumer-group>"
    pullInterval = 1.seconds
    batchSize = 10
    batchMaxTimeToWait = 5.seconds
    maxAckTime = 30.seconds
    maxMsgDeliveries = 10
    genUniqueSuffix = false
}
```

### Processing Messages
To consume messages you just need to collect the messages from the flow.

```kotlin
consumer.consume().collect {
    println("Received message:")
    println(it.data.toString(Charset.defaultCharset()))
    println(it.headers)
    println()
    it.ack()
}
```

If you need tighter control on when a new message should be fetched. `subscribeMessages` only fetches a message when an item in the flow is collected.
It does not listen on DLS messages. You need to listen separately for DLS messages with `subscribeDls`
```kotlin
consumer.subscribeMessages().collect {
    println("Received message:")
    println(it.data.toString(Charset.defaultCharset()))
    println(it.headers)
    println()
    it.ack()
}

consumer.subscribeDls().collect {
    println("Received DLS message:")
    println(it.data.toString(Charset.defaultCharset()))
    println(it.headers)
    println()
    it.ack()
}
```

### Acknowledging a Message
Acknowledging a message indicates to the Memphis server to not <br>re-send the same message again to the same consumer or consumers group.

```kotlin
message.ack()
```

### Get headers
Get headers per message
```kotlin
val headers = msg.headers
```
### Destroying a Consumer

```kotlin
consumer.destroy()
```
