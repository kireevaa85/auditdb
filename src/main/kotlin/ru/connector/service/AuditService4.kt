package ru.connector.service

import org.postgresql.PGConnection
import org.postgresql.PGProperty
import org.postgresql.util.PSQLException
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct


@Component
class AuditService4 {

//    @PostConstruct
    fun run() {
        val url = "jdbc:postgresql://localhost:5432/postgres"
        val props = Properties()
        PGProperty.USER.set(props, "postgres")
        PGProperty.PASSWORD.set(props, "postgres")
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(props, "9.4")
        PGProperty.REPLICATION.set(props, "database")
        PGProperty.PREFER_QUERY_MODE.set(props, "simple")

        val con: Connection = DriverManager.getConnection(url, props)
        val replConnection: PGConnection = con.unwrap(PGConnection::class.java)

        try {
            replConnection.replicationAPI
                .createReplicationSlot()
                .logical()
                .withSlotName("demo_logical_slot")
                .withOutputPlugin("test_decoding")
                .make()
        } catch (e: PSQLException) {
            //https://stackoverflow.com/questions/43611659/how-to-check-if-a-postgresql-logical-replication-slot-already-exists-with-java
            if (e.message?.contains("already exists") != true) {
                println(e.message)
                throw e
            }
        }

        val stream = replConnection.replicationAPI
            .replicationStream()
            .logical()
            .withSlotName("demo_logical_slot")
            .withSlotOption("include-xids", true)
            .withSlotOption("include-timestamp", "on")
            .withSlotOption("skip-empty-xacts", true)
            .withStatusInterval(20, TimeUnit.SECONDS)
            .start() //org.postgresql.util.PSQLException: ERROR: replication slot "demo_logical_slot999" does not exist

        while (true) {
            val msg: ByteBuffer? = stream.readPending()

            if (msg == null) {
                TimeUnit.MILLISECONDS.sleep(10L)
                continue
            }

            println("GET MESSAGE!!!")
            val offset: Int = msg.arrayOffset()
            val source: ByteArray = msg.array()
            val length = source.size - offset
            println("5")
            println(String(source, offset, length))
            println("6")
            //feedback
            stream.setAppliedLSN(stream.lastReceiveLSN);
            stream.setFlushedLSN(stream.lastReceiveLSN);
            println("7")
        }





    }

}