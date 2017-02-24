package io.github.notsyncing.refresh.server.tests

import io.github.notsyncing.lightfur.DataSession
import io.github.notsyncing.lightfur.DatabaseManager
import io.github.notsyncing.lightfur.common.LightfurConfigBuilder
import io.github.notsyncing.lightfur.entity.EntityDataMapper
import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.server.client.ClientManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ClientManagerTest {
    private lateinit var cm: ClientManager
    private lateinit var db: DataSession
    private lateinit var dbName: String

    @Before
    fun setUp() {
        dbName = "refresh-test-db-${Math.random().toString().substring(3)}"

        val d = DatabaseManager.getInstance()
        d.init(LightfurConfigBuilder()
                .database(dbName)
                .databaseVersioning(true)
                .build()).get()

        cm = ClientManager()
        db = DataSession(EntityDataMapper())
    }

    @After
    fun tearDown() {
        db.end().get()

        val d = DatabaseManager.getInstance()
        d.close().get()
        d.init("postgres").get()
        d.dropDatabase(dbName).get()
        d.close().get()
    }

    @Test
    fun testUpdateClientData() {
        val clientData = Client("bbb", "aaa", "ddd", Version.parse("1.0.1")!!, "eee")
        val r = cm.updateClientData(clientData).get()
        assertEquals(OperationResult.Success, r)

        val data = db.query("SELECT * FROM clients").get()
        assertEquals(1, data.numRows)
        assertEquals("aaa", data.rows[0].getString("machine_id"))
        assertEquals("bbb", data.rows[0].getString("account_id"))
        assertEquals("ddd", data.rows[0].getString("account_name"))
        assertEquals("1.0.1", data.rows[0].getString("current_version"))
        assertEquals("eee", data.rows[0].getString("additional_data"))
    }

    @Test
    fun testUpdateExistingClientData() {
        db.execute("INSERT INTO clients (machine_id, account_id, account_name, current_version, additional_data) VALUES ('aaa', 'bbb', 'ccc', '1.0.0', '')").get()

        val clientData = Client("bbb", "aaa", "ddd", Version.parse("1.0.1")!!, "eee")
        val r = cm.updateClientData(clientData).get()
        assertEquals(OperationResult.Success, r)

        val data = db.query("SELECT * FROM clients").get()
        assertEquals(1, data.numRows)
        assertEquals("aaa", data.rows[0].getString("machine_id"))
        assertEquals("bbb", data.rows[0].getString("account_id"))
        assertEquals("ddd", data.rows[0].getString("account_name"))
        assertEquals("1.0.1", data.rows[0].getString("current_version"))
        assertEquals("eee", data.rows[0].getString("additional_data"))
    }

    @Test
    fun testGetClientUpdatePhase() {
        db.execute("INSERT INTO client_phases (account_id, phase) VALUES ('aaa', 2)").get()

        val r = cm.getClientUpdatePhase("aaa").get()
        assertEquals(2, r)
    }

    @Test
    fun testSetClientUpdatePhase() {
        val r = cm.setClientUpdatePhase("aaa", 3).get()
        assertEquals(OperationResult.Success, r)

        val data = db.query("SELECT * FROM client_phases").get()
        assertEquals(1, data.numRows)
        assertEquals("aaa", data.rows[0].getString("account_id"))
        assertEquals(3, data.rows[0].getInteger("phase"))
    }

    @Test
    fun testSetExistingClientUpdatePhase() {
        db.execute("INSERT INTO client_phases (account_id, phase) VALUES ('aaa', 5)").get()

        val r = cm.setClientUpdatePhase("aaa", 4).get()
        assertEquals(OperationResult.Success, r)

        val data = db.query("SELECT * FROM client_phases").get()
        assertEquals(1, data.numRows)
        assertEquals("aaa", data.rows[0].getString("account_id"))
        assertEquals(4, data.rows[0].getInteger("phase"))
    }

    @Test
    fun testGetClientList() {
        db.execute("INSERT INTO clients (machine_id, account_id, account_name, current_version, additional_data) VALUES ('aaa1', 'bbb1', 'ccc1', '1.0.0', 'q')").get()
        db.execute("INSERT INTO clients (machine_id, account_id, account_name, current_version, additional_data) VALUES ('aaa2', 'bbb2', 'ccc2', '1.0.1', 'w')").get()
        db.execute("INSERT INTO clients (machine_id, account_id, account_name, current_version, additional_data) VALUES ('aaa3', 'bbb3', 'ccc3', '1.0.2', 'e')").get()

        val l = cm.getClientList().get()
        assertEquals(3, l.size)

        assertEquals("aaa1", l[0].machineIdentifier)
        assertEquals("bbb1", l[0].accountIdentifier)
        assertEquals("ccc1", l[0].accountName)
        assertEquals("1.0.0", l[0].currentVersion.toString())
        assertEquals("q", l[0].additionalData)

        assertEquals("aaa2", l[1].machineIdentifier)
        assertEquals("bbb2", l[1].accountIdentifier)
        assertEquals("ccc2", l[1].accountName)
        assertEquals("1.0.1", l[1].currentVersion.toString())
        assertEquals("w", l[1].additionalData)

        assertEquals("aaa3", l[2].machineIdentifier)
        assertEquals("bbb3", l[2].accountIdentifier)
        assertEquals("ccc3", l[2].accountName)
        assertEquals("1.0.2", l[2].currentVersion.toString())
        assertEquals("e", l[2].additionalData)
    }
}