package io.github.notsyncing.refresh.server.tests

import io.github.notsyncing.refresh.common.enums.LoginResult
import io.github.notsyncing.refresh.common.utils.deleteRecursive
import io.github.notsyncing.refresh.common.utils.sha256Salted
import io.github.notsyncing.refresh.server.user.UserManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class UserManagerTest {
    private lateinit var um: UserManager
    private lateinit var tempPath: Path

    @Before
    fun setUp() {
        tempPath = Files.createTempDirectory("refresh-test-")
        UserManager.userFileStoragePath = tempPath

        um = UserManager()
    }

    @After
    fun tearDown() {
        um.destroy()

        deleteRecursive(tempPath)
    }

    @Test
    fun testLogin() {
        val rand = um.generateLoginRand("admin")
        val (r, token) = um.login("admin", ("123456".sha256Salted() + rand).sha256Salted() ?: "")
        assertEquals(LoginResult.Success, r)
        assertEquals("admin", um.getUsernameByToken(token))
    }

    @Test
    fun testLogout() {
        val rand = um.generateLoginRand("admin")
        val (r, token) = um.login("admin", ("123456".sha256Salted() + rand).sha256Salted() ?: "")
        assertEquals(LoginResult.Success, r)

        um.logout(token)
        assertNull(um.getUsernameByToken(token))
    }

    @Test
    fun testModifyPassword() {
        um.modifyPassword("admin", "111111".sha256Salted() ?: "")
        val rand = um.generateLoginRand("admin")

        val (r, token) = um.login("admin", ("123456".sha256Salted() + rand).sha256Salted() ?: "")
        assertEquals(LoginResult.WrongPassword, r)

        val (r2, token2) = um.login("admin", ("111111".sha256Salted() + rand).sha256Salted() ?: "")
        assertEquals(LoginResult.Success, r2)
    }

    @Test
    fun testModifyPasswordWithOldPassword() {
        um.modifyPassword("admin", "123456".sha256Salted() ?: "", "111111".sha256Salted() ?: "")
        val rand = um.generateLoginRand("admin")

        val (r, token) = um.login("admin", ("123456".sha256Salted() + rand).sha256Salted() ?: "")
        assertEquals(LoginResult.WrongPassword, r)

        val (r2, token2) = um.login("admin", ("111111".sha256Salted() + rand).sha256Salted() ?: "")
        assertEquals(LoginResult.Success, r2)
    }
}