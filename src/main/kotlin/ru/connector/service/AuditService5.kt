package ru.connector.service

import org.postgresql.PGConnection
import org.postgresql.PGProperty
import org.postgresql.replication.ReplicationType
import org.postgresql.util.PSQLException
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct


@Component
class AuditService5 {
    val logicalSlotName: String = "logicalSlot4AuditDB";

//    @PostConstruct
    fun run() {
        val url = "jdbc:postgresql://localhost:6432/postgres"
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
                .withSlotName(logicalSlotName)
//                .withOutputPlugin("test_decoding")
                .withOutputPlugin("wal2json")
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
            .withSlotName(logicalSlotName)
            //Parameters: https://github.com/eulerto/wal2json/tree/wal2json_2_4 (Readme.md)
            .withSlotOption("include-timestamp", true)
            .withSlotOption("include-pk", true)
            .withSlotOption("include-lsn", true)
            .withSlotOption("filter-origins", "")
            .withSlotOption("filter-tables", "*.databasechangeloglock")
            .withSlotOption("format-version", 2)
            .withStatusInterval(20, TimeUnit.SECONDS)
            .start() //org.postgresql.util.PSQLException: ERROR: replication slot "demo_logical_slot999" does not exist

        while (true) {
            val msg: ByteBuffer? = stream.readPending()

            if (msg == null) {
                TimeUnit.MILLISECONDS.sleep(10L)
                continue
            }

            val offset: Int = msg.arrayOffset()
            val source: ByteArray = msg.array()
            val length = source.size - offset
            val str = String(source, offset, length)
            if (str.contains("\"action\":\"B\"") || str.contains("\"action\":\"C\"")) {
                continue
            }
            println()
            println(str)
            //feedback
            stream.setAppliedLSN(stream.lastReceiveLSN)
            stream.setFlushedLSN(stream.lastReceiveLSN)
        }





    }

}