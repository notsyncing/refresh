package io.github.notsyncing.refresh.server.tests

import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.common.utils.deleteRecursive
import io.github.notsyncing.refresh.server.app.AppManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class AppManagerTest {
    private lateinit var am: AppManager
    private lateinit var tempPath: Path

    @Before
    fun setUp() {
        tempPath = Files.createTempDirectory("refresh-test-")
        AppManager.appFileStoragePath = tempPath

        am = AppManager()
    }

    @After
    fun tearDown() {
        deleteRecursive(tempPath)
    }

    private fun createTestApp(appName: String, version: String, phase: Int, pkgContent: String): Triple<Path, Path, Path> {
        val testFile = Files.createTempFile(tempPath, "refresh-test-file-", ".zip")
        Files.write(testFile, pkgContent.toByteArray())
        am.createAppVersion(appName, Version.parse(version)!!, phase, testFile, "zip")
        val appPath = tempPath.resolve("apps").resolve(appName).resolve(version)
        val phaseFile = appPath.resolve(".phase")
        val typeFile = appPath.resolve(".type")

        return Triple(appPath, phaseFile, typeFile)
    }

    @Test
    fun testCreateAppVersion() {
        val (appPath, phaseFile, typeFile) = createTestApp("testApp", "1.0.1", 2, "123")

        assertTrue(Files.isDirectory(appPath))
        assertEquals("123", Files.readAllBytes(appPath.resolve("package.zip")).toString(Charsets.UTF_8))
        assertEquals("zip", Files.readAllBytes(typeFile).toString(Charsets.UTF_8))
        assertEquals("2", Files.readAllBytes(phaseFile).toString(Charsets.UTF_8))
    }

    @Test
    fun testGetAppLatestVersion() {
        createTestApp("testApp", "1.0.0", 2, "123")
        createTestApp("testApp", "1.0.1", 2, "123")

        assertEquals(Version.parse("1.0.1")!!, am.getAppLatestVersion("testApp"))
    }

    @Test
    fun testGetAppLatestVersionPhased() {
        createTestApp("testApp", "1.0.0", 0, "123")
        createTestApp("testApp", "1.0.1", 2, "123")

        assertEquals(Version.parse("1.0.0")!!, am.getAppLatestVersion("testApp", Client("", "", "", Version.empty, ""), 1))
    }

    @Test
    fun testGetAppVersions() {
        createTestApp("testApp", "1.0.0", 0, "123")
        createTestApp("testApp", "1.0.1", 2, "123")
        val vl = am.getAppVersions("testApp")

        assertEquals(2, vl.size)
        assertEquals("1.0.1", vl[0].toString())
        assertEquals("1.0.0", vl[1].toString())
    }

    @Test
    fun testGetAppVersionsPhased() {
        createTestApp("testApp", "1.0.0", 0, "123")
        createTestApp("testApp", "1.0.1", 2, "123")
        val vl = am.getAppVersions("testApp", Client("", "", "", Version.empty, ""), 0)

        assertEquals(1, vl.size)
        assertEquals("1.0.0", vl[0].toString())
    }

    @Test
    fun testGetAppPhasedVersions() {
        createTestApp("testApp", "1.0.0", 0, "123")
        createTestApp("testApp", "1.0.1", 2, "123")
        val vl = am.getAppVersionPhases("testApp")

        assertEquals(2, vl.size)
        assertEquals("1.0.1@2", vl[0].toString())
        assertEquals(2, vl[0].phase)
        assertEquals("1.0.0@0", vl[1].toString())
        assertEquals(0, vl[1].phase)
    }

    @Test
    fun testDeleteAppVersion() {
        val (appPath, _) = createTestApp("testApp", "1.0.0", 0, "123")
        createTestApp("testApp", "1.0.1", 2, "123")
        am.deleteAppVersion("testApp", Version(1, 0, 0))
        val vl = am.getAppVersions("testApp")

        assertEquals(1, vl.size)
        assertEquals("1.0.1", vl[0].toString())
        assertFalse(Files.exists(appPath))
    }

    @Test
    fun testDeleteApp() {
        createTestApp("testApp", "1.0.0", 0, "123")
        assertTrue(Files.exists(tempPath.resolve("apps").resolve("testApp")))

        am.deleteApp("testApp")
        assertFalse(Files.exists(tempPath.resolve("apps").resolve("testApp")))

        val vl = am.getAppVersions("testApp")
        assertEquals(0, vl.size)
    }

    @Test
    fun testAppHasVersion() {
        assertFalse(am.appHasVersion("testApp", Version(1, 0, 0)))
        createTestApp("testApp", "1.0.0", 0, "123")
        assertTrue(am.appHasVersion("testApp", Version(1, 0, 0)))
    }

    @Test
    fun testHasApp() {
        assertFalse(am.hasApp("testApp"))
        createTestApp("testApp", "1.0.0", 0, "123")
        assertTrue(am.hasApp("testApp"))
    }

    @Test
    fun testGetAppList() {
        createTestApp("testApp1", "1.0.0", 0, "123")
        createTestApp("testApp2", "1.0.1", 2, "123")
        val l = am.getAppList()

        assertEquals(2, l.size)
        assertEquals("testApp1", l[0].name)
        assertEquals(1, l[0].versions.size)
        assertEquals("1.0.0", l[0].versions[0].toString())
        assertEquals(1, l[0].versionPhases.entries.size)
        assertEquals(0, l[0].versionPhases[Version(1, 0, 0)])
        assertEquals("testApp2", l[1].name)
        assertEquals(1, l[1].versions.size)
        assertEquals("1.0.1", l[1].versions[0].toString())
        assertEquals(1, l[1].versionPhases.entries.size)
        assertEquals(2, l[1].versionPhases[Version(1, 0, 1)])
    }

    @Test
    fun testGetAppListAfterReload() {
        createTestApp("testApp1", "1.0.0", 0, "123")
        createTestApp("testApp2", "1.0.1", 2, "123")
        am.reload()
        val l = am.getAppList()

        assertEquals(2, l.size)
        assertEquals("testApp1", l[0].name)
        assertEquals(1, l[0].versions.size)
        assertEquals("1.0.0", l[0].versions[0].toString())
        assertEquals(1, l[0].versionPhases.entries.size)
        assertEquals(0, l[0].versionPhases[Version(1, 0, 0)])
        assertEquals("testApp2", l[1].name)
        assertEquals(1, l[1].versions.size)
        assertEquals("1.0.1", l[1].versions[0].toString())
        assertEquals(1, l[1].versionPhases.entries.size)
        assertEquals(2, l[1].versionPhases[Version(1, 0, 1)])
    }

    @Test
    fun testGetApp() {
        createTestApp("testApp", "1.0.0", 1, "123")
        val app = am.getApp("testApp")

        assertNotNull(app)
        assertEquals("testApp", app!!.name)
        assertEquals(1, app.versions.size)
        assertEquals("1.0.0", app.versions[0].toString())
        assertEquals(1, app.versionPhases.size)
        assertEquals(1, app.versionPhases[Version(1, 0, 0)])
    }

    @Test
    fun testGetAppPackage() {
        createTestApp("testApp", "1.0.0", 1, "123")
        val appPath = am.getAppPackage("testApp", Version(1, 0, 0))
        assertEquals(tempPath.resolve("apps").resolve("testApp").resolve("1.0.0").resolve("package"), appPath)
    }

    @Test
    fun testSetAppVersionPhase() {
        createTestApp("testApp", "1.0.0", 1, "123")
        am.setAppVersionPhase("testApp", Version(1, 0, 0), 2)
        val l = am.getAppVersionPhases("testApp")
        assertEquals(2, l[0].phase)
    }
}