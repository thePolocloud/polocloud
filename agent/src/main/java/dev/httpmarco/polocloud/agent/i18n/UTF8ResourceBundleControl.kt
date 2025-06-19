package dev.httpmarco.polocloud.agent.i18n

import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle

class UTF8ResourceBundleControl : ResourceBundle.Control() {

    override fun newBundle(
        baseName: String,
        locale: Locale,
        format: String,
        loader: ClassLoader,
        reload: Boolean
    ): ResourceBundle? {
        if (format != "java.properties") {
            return super.newBundle(baseName, locale, format, loader, reload)
        }

        val bundleName = toBundleName(baseName, locale)
        val resourceName = toResourceName(bundleName, "properties")

        val inputStream = getInputStream(resourceName, loader, reload)
        val reader = inputStream?.let { InputStreamReader(it, StandardCharsets.UTF_8) }

        return reader?.use { PropertyResourceBundle(it) }
            ?: super.newBundle(baseName, locale, format, loader, reload)
    }

    private fun getInputStream(resource: String, loader: ClassLoader, reload: Boolean): InputStream? {
        return if (reload) {
            val url: URL? = loader.getResource(resource)
            url?.openConnection()?.apply { useCaches = false }?.getInputStream()
        } else {
            loader.getResourceAsStream(resource)
        }
    }
}
