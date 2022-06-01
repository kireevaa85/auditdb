package ru.connector.service

import org.postgresql.PGConnection
import org.postgresql.PGProperty
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct


@Component
class AuditService {

//    @PostConstruct //todo uncomment
    fun run() {
        //Create replication connection
        val url = "jdbc:postgresql://localhost:5432/postgres"
        val props = Properties()
        PGProperty.USER.set(props, "postgres")
        PGProperty.PASSWORD.set(props, "postgres")
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(props, "9.4")
        PGProperty.REPLICATION.set(props, "database")
        PGProperty.PREFER_QUERY_MODE.set(props, "simple")

        val con: Connection = DriverManager.getConnection(url, props)
        val replConnection: PGConnection = con.unwrap(PGConnection::class.java)

        //Create replication slot via pgjdbc API
        replConnection.replicationAPI
            .createReplicationSlot()
            .logical()
            .withSlotName("demo_logical_slot4")
            .withOutputPlugin("test_decoding")
            .make() //org.postgresql.util.PSQLException: ERROR: replication slot "demo_logical_slot4" already exists
                    //org.postgresql.util.PSQLException: ERROR: all replication slots are in use

        //Create logical replication stream.
        val stream = replConnection.replicationAPI
            .replicationStream()
            .logical()
            .withSlotName("demo_logical_slot4")
            .withSlotOption("include-xids", false)
            .withSlotOption("skip-empty-xacts", true)
            .withStatusInterval(20, TimeUnit.SECONDS)
            .start()
        /*LogSequenceNumber waitLSN = LogSequenceNumber.valueOf("6F/E3C53568");
        val stream = replConnection.replicationAPI
            .replicationStream()
            .logical()
            .withSlotName("demo_logical_slot")
            .withSlotOption("include-xids", false)
            .withSlotOption("skip-empty-xacts", true)
            .withStatusInterval(20, TimeUnit.SECONDS)
            .withStartPosition(waitLSN)
            .start()*/

        //Receive changes via replication stream
        while (true) {
            //non blocking receive message
            val msg: ByteBuffer? = stream.readPending() //org.springframework.beans.factory.BeanCreationException:
                                                        // Error creating bean with name 'auditService': Invocation of init
                                                        // method failed; nested exception is org.postgresql.util.PSQLException:
                                                        // PGStream is closed
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
        }


    }

//    GET MESSAGE!!!
//    5
//    BEGIN
//    6
//    GET MESSAGE!!!
//    5
//    table public.exec_data: INSERT: id[bigint]:1 session_id[character varying]:'autoapp-44.0.1-dafsdf' date_coverage[date]:'2021-05-20' microservice_name[character varying]:'autoapp' microservice_version[character varying]:'44.0.1' microservice_hash[character varying]:'dafsdf'
//    6
//    GET MESSAGE!!!
//    5
//    COMMIT
//    6
    //через какое-то время:
    //Caused by: org.postgresql.util.PSQLException: PGStream is closed


    //Add feedback indicating a successfully process LSN
//    while (true) {
//        //Receive last successfully send to queue message. LSN ordered.
//        LogSequenceNumber successfullySendToQueue = getQueueFeedback();
//        if (successfullySendToQueue != null) {
//            stream.setAppliedLSN(successfullySendToQueue);
//            stream.setFlushedLSN(successfullySendToQueue);
//        }
//
//        //non blocking receive message
//        ByteBuffer msg = stream.readPending();
//
//        if (msg == null) {
//            TimeUnit.MILLISECONDS.sleep(10L);
//            continue;
//        }
//
//        asyncSendToQueue(msg, stream.getLastReceiveLSN());
//    }

}