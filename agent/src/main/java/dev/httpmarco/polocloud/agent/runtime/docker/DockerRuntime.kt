package dev.httpmarco.polocloud.agent.runtime.docker

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.Runtime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class DockerRuntime : Runtime {

    override fun runnable(): Boolean {
        return try {
            val future = CompletableFuture.supplyAsync {
                val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerCertPath("/data/.docker")
                    .build()

                val httpClient = ApacheDockerHttpClient.Builder()
                    .dockerHost(config.dockerHost)
                    .sslConfig(config.sslConfig)
                    .build()

                val client = DockerClientImpl.getInstance(config, httpClient)
                client.infoCmd().exec()
                true
            }

            future.get(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.debug("Docker daemon not available: ${e.javaClass.simpleName} - ${e.message}")
            false
        }
    }
}