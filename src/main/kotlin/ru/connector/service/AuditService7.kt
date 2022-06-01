package ru.connector.service

import org.postgresql.PGConnection
import org.postgresql.PGProperty
import org.postgresql.replication.LogSequenceNumber
import org.postgresql.replication.PGReplicationStream
import org.postgresql.util.PSQLException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct


@Component
class AuditService7 {
    private val logicalSlotName: String = "logicalSlot4AuditDB";
    private val urlDB: String = "jdbc:postgresql://localhost:6432/postgres"

    @PostConstruct
    fun run() {
        val props = Properties()
        PGProperty.USER.set(props, "postgres")
        PGProperty.PASSWORD.set(props, "postgres")
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(props, "9.4")
        PGProperty.REPLICATION.set(props, "database")
        PGProperty.PREFER_QUERY_MODE.set(props, "simple")

        while (true) {
            var connection: Connection? = null
            var replConnection: PGConnection? = null
            var stream: PGReplicationStream? = null

            try {
                connection = DriverManager.getConnection(urlDB, props)
                replConnection = connection.unwrap(PGConnection::class.java)

                //create replication if not exist
                try {
                    //replConnection.replicationAPI.dropReplicationSlot(logicalSlotName);
                    replConnection.replicationAPI
                        .createReplicationSlot()
                        .logical()
                        .withSlotName(logicalSlotName)
                        .withOutputPlugin("wal2json")
                        .make()
                } catch (e: PSQLException) {
                    // TODO: 01.06.22 look link
                    //https://stackoverflow.com/questions/43611659/how-to-check-if-a-postgresql-logical-replication-slot-already-exists-with-java
                    if (e.message?.contains("already exists") != true) {
                        log.error(e.message)
                        throw e
                    }
                }

                stream = replConnection.replicationAPI
                    .replicationStream()
                    .logical()
                    .withSlotName(logicalSlotName)
//                    var startLsn: LogSequenceNumber? = LogSequenceNumber.valueOf("0/16335B8")
//                    .withStartPosition(startLsn)
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
                    val replMsg = String(source, offset, length)
                    if (replMsg.contains("\"action\":\"B\"") || replMsg.contains("\"action\":\"C\"")) {
                        continue
                    }
                    log.info(replMsg)
                    //feedback
                    val lastReceiveLSN = stream.lastReceiveLSN
                    stream.setAppliedLSN(lastReceiveLSN)
                    stream.setFlushedLSN(lastReceiveLSN)
                    stream.forceUpdateStatus()
                }
            } catch (e1: SQLException) {
                log.error(e1.message)

                if (stream != null) {
                    try {
                        stream.close()
                    } catch (e2: SQLException) {
                        log.error(e2.message)
                    }
                }
                if (connection != null) {
                    try {
                        connection.close()
                    } catch (e2: SQLException) {
                        log.error(e2.message)
                    }
                }
            }
        }

    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(AuditService6::class.java)
    }

}