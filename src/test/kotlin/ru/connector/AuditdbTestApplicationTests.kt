package ru.connector

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AuditdbTestApplicationTests {
//    Some terminology: (https://severalnines.com/database-blog/using-postgresql-logical-replication-maintain-always-date-readwrite-test-server)
//
//    Publication: A set of changes from a set of tables defined in a specific database on a physical replication primary server. A Publication may handle all or some of: INSERT, DELETE, UPDATE, TRUNCATE.
//    Publisher node: The server where the publication resides.
//    Replica identity: A way to identify the row on the subscriber side for UPDATEs and DELETEs.
//    Subscription: A connection to a publisher node and one or more publications in it. A subscription uses a dedicated replication slot on the publisher for replication. Additional replication slots may be used for the initial synchronization step.
//    Subscriber node: The server where the subscription resides.

//    Logical replication of a table consists of two stages:
//    Taking a snapshot of the table on the publisher and copying it to the subscriber
//    Applying all changes (since the snapshot) in the same sequence
//    Logical replication is transactional and guarantees that the order of changes being applied to the subscriber remains the same as on the publisher.

//    Restrictions
//    Only DML operations are supported. No DDL. The schema has to be defined beforehand
//    Sequences are not replicated
//    Large Objects are not replicated
//    Only plain base tables are supported (materialized views, partition root tables, foreign tables are not supported)








    //check INSERT, DELETE, UPDATE, TRUNCATE
    //check create table and change him online (checks news tables after start replication?)
}