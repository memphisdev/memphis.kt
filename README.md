![Github (3)](https://github.com/memphisdev/memphis.java/assets/107035359/18151b8f-18a8-4285-a7a6-6d6cce721a6e)
<p align="center">
<a href="https://memphis.dev/discord"><img src="https://img.shields.io/discord/963333392844328961?color=6557ff&label=discord" alt="Discord"></a>
<a href="https://github.com/memphisdev/memphis/issues?q=is%3Aissue+is%3Aclosed"><img src="https://img.shields.io/github/issues-closed/memphisdev/memphis?color=6557ff"></a> 
  <img src="https://img.shields.io/npm/dw/memphis-dev?color=ffc633&label=installations">
<a href="https://github.com/memphisdev/memphis/blob/master/CODE_OF_CONDUCT.md"><img src="https://img.shields.io/badge/Code%20of%20Conduct-v1.0-ff69b4.svg?color=ffc633" alt="Code Of Conduct"></a> 
<img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/memphisdev/memphis?color=61dfc6">
<img src="https://img.shields.io/github/last-commit/memphisdev/memphis?color=61dfc6&label=last%20commit">
</p>

 <b><p align="center">
  <a href="https://memphis.dev/pricing/">Cloud</a> - <a href="github.com/memphisdev/memphis-dev-academy">Academy</a> - <a href="https://memphis.dev/docs/">Docs</a> - <a href="https://twitter.com/Memphis_Dev">X</a> - <a href="https://www.youtube.com/channel/UCVdMDLCSxXOqtgrBaRUHKKg">YouTube</a>
</p></b>

<div align="center">

  <h4>

**[Memphis.dev](https://memphis.dev)** is more than a broker. It's a new streaming stack.<br>
Memphis.dev is a highly scalable event streaming and processing engine.<br>

<img width="177" alt="cloud_native 2 (5)" src="https://github.com/memphisdev/memphis/assets/107035359/a20ea11c-d509-42bb-a46c-e388c8424101">

  </h4>
  
</div>

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
To connect using Username and Password 
```kotlin
 val memphis = Memphis.connect("<memphis-host>", "<application type username>", Memphis.Password("<user-password>"))
```
To connect using Token 
```kotlin
 val memphis = Memphis.connect("<memphis-host>", "<application type username>", Memphis.ConnectionToken("<user-password>"))
```
<br>
It is possible to pass connection configuration parameters.

```kotlin
val memphis = Memphis.connect("<memphis-host>", "<application type username>", Memphis.Password("<user-password>")) {
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

### Retention Values

The `retention values` are directly related to the `retention types` mentioned above,<br> where the values vary according to the type of retention chosen.

All retention values are of type `int` but with different representations as follows:

`RetentionType.MAX_AGE_SECONDS` is represented **in seconds**, `RetentionType.MESSAGES` in a **number of messages** <br> and finally `RetentionType.BYTES` in a **number of bytes**.

After these limits are reached oldest messages will be deleted.

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
