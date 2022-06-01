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
class AuditService2 {

//    @PostConstruct //todo uncomment
    fun run() {
        val url = "jdbc:postgresql://localhost:5432/postgres"
        val props = Properties()
        PGProperty.USER.set(props, "postgres")
        PGProperty.PASSWORD.set(props, "postgres")
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(props, "9.4")
        PGProperty.REPLICATION.set(props, "database")
        PGProperty.PREFER_QUERY_MODE.set(props, "simple")

        val con: Connection = DriverManager.getConnection(url, props)
        //org.postgresql.util.PSQLException: Подсоединение по адресу localhost:6432 отклонено. Проверьте что хост и порт указаны правильно и что postmaster принимает TCP/IP-подсоединения.
        val replConnection: PGConnection = con.unwrap(PGConnection::class.java)

        replConnection.replicationAPI
            .createReplicationSlot()
            .logical()
            .withSlotName("demo_logical_slot4")
//            .withOutputPlugin("pgoutput")
            .withOutputPlugin("test_decoding")
            .make()

        //some changes after create replication slot to demonstrate receive it
//        sqlConnection.setAutoCommit(true)
//        var st: Statement = sqlConnection.createStatement()
//        st.execute("insert into test_logic_table(name) values('first tx changes')")
//        st.close()
//
//        st = sqlConnection.createStatement()
//        st.execute("update test_logic_table set name = 'second tx change' where pk = 1")
//        st.close()
//
//        st = sqlConnection.createStatement()
//        st.execute("delete from test_logic_table where pk = 1")
//        st.close()

        val stream = replConnection.replicationAPI
            .replicationStream()
            .logical()
            .withSlotName("demo_logical_slot4")
            .withSlotOption("include-xids", false)
            .withSlotOption("skip-empty-xacts", true)
            .withStatusInterval(20, TimeUnit.SECONDS)
            .start()

        while (true) {
            //non blocking receive message
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

//        BEGIN
//        table public.test_logic_table: INSERT: pk[integer]:1 name[character varying]:'first tx changes'
//        COMMIT
//        BEGIN
//        table public.test_logic_table: UPDATE: pk[integer]:1 name[character varying]:'second tx change'
//        COMMIT
//        BEGIN
//        table public.test_logic_table: DELETE: pk[integer]:1
//        COMMIT

//        GET MESSAGE!!!
//        5
//        BEGIN
//        6
//        7
//        GET MESSAGE!!!
//        5
//        table public.exec_data: INSERT: id[bigint]:3 session_id[character varying]:'autoapp-44.0.3-fafddf' date_coverage[date]:'2022-05-20' microservice_name[character varying]:'autoapp' microservice_version[character varying]:'44.0.3' microservice_hash[character varying]:'fafddf'
//        6
//        7
//        GET MESSAGE!!!
//        5
//        COMMIT
//        6
//        7





    }

}