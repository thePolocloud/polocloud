package dev.httpmarco.polocloud.agent.i18n

import java.util.*

open class I18nProvider(private val resourceBundlePrefix: String) : I18n {

    private val utf8ResourceBundleControl = UTF8ResourceBundleControl()
    private val locale = Locale.ENGLISH // default/backup locale is English

    override fun get(key: String): String? {
        return get(key, mutableListOf<Any?>())
    }

    override fun get(key: String, vararg format: Any): String {
        val resourceBundle: ResourceBundle = this.resourceBundle(locale)

        if (!resourceBundle.containsKey(key)) {
            return key
        }
        if (format.isEmpty()) {
            //todo
            //return LoggingColors.translate(resourceBundle.getString(key))
        }
        //return LoggingColors.translate(String.format(resourceBundle.getString(key), *format))
        return resourceBundle.getString(key)
    }

    override fun get(key: String, locale: Locale, vararg format: Any): String {
        return getDefault(key, *format)
    }

    override fun getDefault(key: String, vararg format: Any): String {
        return get(key, this.locale, format)
    }

    override fun resourceBundle(locale: Locale): ResourceBundle {
        try {
            return this.localBundle(locale)
        } catch (exception: MissingResourceException) {
            return defaultResourceBundle()
        }
    }

    override fun defaultResourceBundle(): ResourceBundle {
        return this.localBundle(this.locale)
    }

    fun localBundle(locale: Locale): ResourceBundle {
        return ResourceBundle.getBundle(
            this.resourceBundlePrefix,
            locale,
            this.javaClass.getClassLoader(),
            utf8ResourceBundleControl
        )
    }
}