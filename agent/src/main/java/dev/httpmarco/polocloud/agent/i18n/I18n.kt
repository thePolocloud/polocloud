package dev.httpmarco.polocloud.agent.i18n

import java.util.*

interface I18n {

    /**
     * The default language is used here.
     * @param key the key of translation
     * @return the translation for the key
     */
    fun get(key: String): String?

    /**
     * The default language is used here.
     * @param key the key of translation
     * @param format format the translation with argument {0} -> format
     * @return the translation for the key
     */
    fun get(key: String, vararg format: Any): String?

    /**
     * @param key the key of translation
     * @param locale the locale of the translation
     * @param format format the translation with argument {0} -> format
     * @return the translation for the key
     */
    fun get(key: String, locale: Locale, vararg format: Any): String?

    /**
     * The default language is used here.
     * @param key the key of translation
     * @param format format the translation with argument {0} -> format
     * @return the translation for the key
     */
    fun getDefault(key: String, vararg format: Any): String

    /**
     * @param locale the locale of the translation
     * @return the resource bundle for the locale
     */
    fun resourceBundle(locale: Locale): ResourceBundle

    /**
     * @return the resource bundle for the default language
     */
    fun defaultResourceBundle(): ResourceBundle

}