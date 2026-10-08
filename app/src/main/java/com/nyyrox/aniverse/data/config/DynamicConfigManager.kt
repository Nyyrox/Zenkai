package com.nyyrox.aniverse.data.config

import com.nyyrox.aniverse.data.DEFAULT_STATIC_PROVIDERS
import com.nyyrox.aniverse.data.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DynamicConfigManager {
    private val _activeProviders = MutableStateFlow<List<Provider>>(DEFAULT_STATIC_PROVIDERS)
    val activeProviders: StateFlow<List<Provider>> = _activeProviders.asStateFlow()

    private val _workerConfigs = MutableStateFlow<Map<String, String>>(
        mapOf(
            "zenkai_stream_proxy" to "https://zenkai-stream-proxy.ammhfoo.workers.dev",
            "manga_worker_origin" to "https://brhgxkmnoken-mud-a7f7nyroxx.ammhfoo.workers.dev",
            "manga_proxy_origin" to "https://holy-union-7b91.mahfoooozzzzalm.workers.dev",
            "dekho_direct_worker" to "https://animedekho-resolver.ammhfoo.workers.dev",
            "direct_multi_worker" to "https://misty-dawn-fe70.mahfoozalam7850.workers.dev/api/resolve"
        )
    )
    val workerConfigs: StateFlow<Map<String, String>> = _workerConfigs.asStateFlow()

    fun updateProviders(servers: List<Provider>) {
        if (servers.isNotEmpty()) {
            val remoteIds = servers.map { it.id }.toSet()
            _activeProviders.value = servers + DEFAULT_STATIC_PROVIDERS.filter { it.id !in remoteIds }
        }
    }

    fun updateWorkerConfigs(configs: Map<String, String>) {
        if (configs.isNotEmpty()) {
            _workerConfigs.value = _workerConfigs.value + configs
        }
    }

    fun getWorkerUrl(key: String, fallback: String): String {
        return _workerConfigs.value[key]?.takeIf { it.isNotBlank() } ?: fallback
    }

    val currentProviders: List<Provider> get() = _activeProviders.value
    val currentSubProviders: List<Provider> get() = _activeProviders.value.filter { !it.isDub && !it.isMulti }
    val currentDubProviders: List<Provider> get() = _activeProviders.value.filter { it.isDub }
    val currentMultiProviders: List<Provider> get() = _activeProviders.value.filter { it.isMulti }
}
