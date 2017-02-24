package io.github.notsyncing.refresh.server

import io.github.notsyncing.cowherd.Cowherd
import io.github.notsyncing.cowherd.service.ComponentInstantiateType
import io.github.notsyncing.cowherd.service.DependencyInjector
import io.github.notsyncing.lightfur.DatabaseManager
import io.github.notsyncing.lightfur.common.LightfurConfigBuilder
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.authenticate.SceneAuthenticator
import io.github.notsyncing.manifold.feature.FeatureAuthenticator
import io.github.notsyncing.manifold.feature.FeatureAuthenticator.Companion.FeatureAuthBuilder.Companion.noAuth
import io.github.notsyncing.refresh.server.enums.Auth
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.Module
import io.github.notsyncing.refresh.server.user.SimpleUserAuth
import org.apache.commons.daemon.Daemon
import org.apache.commons.daemon.DaemonContext

class RefreshServerApp : Daemon {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val app = RefreshServerApp()
            app.start()
        }
    }

    private val server = Cowherd()

    private fun createDependencyInjector(): DependencyInjector {
        return object : DependencyInjector {
            private val di = Manifold.dependencyProvider

            override fun clear() {
                di!!.reset()
            }

            override fun init() {
                di!!.init()
            }

            override fun registerComponent(interfaceType: Class<Any>, objectType: Class<Any>, createType: ComponentInstantiateType, createEarly: Boolean) {
                di!!.registerMapping(objectType, interfaceType)

                if (createType == ComponentInstantiateType.Singleton) {
                    if (createEarly) {
                        di.get(interfaceType, true)
                    } else {
                        di.registerSingleton(interfaceType)
                    }
                }
            }

            override fun registerComponent(type: Class<*>, createType: ComponentInstantiateType, createEarly: Boolean) {
                if (createType == ComponentInstantiateType.Singleton) {
                    if (createEarly) {
                        di!!.get(type, true)
                    } else {
                        di!!.registerSingleton(type)
                    }
                }
            }

            override fun registerComponent(type: Class<Any>, o: Any) {
                di!!.registerAs<Any, Any>(o, type)
            }

            override fun registerComponent(o: Any) {
                di!!.register(o)
            }

            override fun registerComponent(c: Class<*>) {
            }

            override fun <T> getComponent(type: Class<T>): T? {
                return di!!.get(type)
            }

            override fun getComponent(className: String): Any? {
                return di!!.get(Class.forName(className))
            }

            override fun hasComponent(type: Class<*>): Boolean {
                return true
            }

            override fun <T> makeObject(type: Class<T>): T? {
                return di!!.get(type, true)
            }
        }
    }

    override fun start() {
        val dbConf = LightfurConfigBuilder()
                .database("refresh_database")
                .databaseVersioning(true)
                .build()

        DatabaseManager.getInstance().init(dbConf)

        SceneAuthenticator.authModule<Module>()
        SceneAuthenticator.authType<Auth>()

        Manifold.enableFeatureManagement = true
        Manifold.authInfoProvider<SimpleUserAuth>()

        FeatureAuthenticator.configure {
            our feature arrayOf(Features.GetAppList, Features.GetAppVersion, Features.GetAppLatestVersion, Features.GetAppPhasedVersion) needs Module.App type Auth.View
            our feature arrayOf(Features.CreateAppVersion, Features.ReloadApp, Features.SetAppVersionPhase) needs Module.App type Auth.Edit
            our feature arrayOf(Features.DeleteApp, Features.DeleteAppVersion) needs Module.App type Auth.Delete
            our feature arrayOf(Features.GetAppClientLatestVersion, Features.GetAppClientVersion) needs noAuth

            our feature arrayOf(Features.GetClientList, Features.GetClientUpdatePhase) needs Module.Client type Auth.View
            our feature Features.SetClientUpdatePhase needs Module.Client type Auth.Edit
        }

        Manifold.init()

        Cowherd.dependencyInjector = createDependencyInjector()
        server.start()
    }

    override fun stop() {
        server.stop()
    }

    override fun destroy() {
        stop()
    }

    override fun init(context: DaemonContext?) {
    }
}