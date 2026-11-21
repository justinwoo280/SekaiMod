package moe.matsuri.nb4a.proxy.snell

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.preference.EditTextPreferenceModifiers
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.ui.profile.ProfileSettingsActivity
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type
import moe.matsuri.nb4a.ui.SimpleMenuPreference

class SnellSettingsActivity : ProfileSettingsActivity<SnellBean>() {
    override fun createEntity() = SnellBean().applyDefaultValues()
    private val bindings = PreferenceBindingManager().apply {
        for (field in listOf("name", "serverAddress", "psk", "userKey", "network", "obfsMode", "obfsHost", "mode")) {
            add(PreferenceBinding(Type.Text, field))
        }
        add(PreferenceBinding(Type.TextToInt, "serverPort"))
        add(PreferenceBinding(Type.TextToInt, "version"))
        add(PreferenceBinding(Type.Bool, "reuse"))
    }

    override fun SnellBean.init() = bindings.writeToCacheAll(this)
    override fun SnellBean.serialize() {
        bindings.fromCacheAll(this)
    }

    override fun PreferenceFragmentCompat.createPreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.snell_preferences)
        bindings.setPreferenceFragment(this)
        findPreference<EditTextPreference>("serverPort")!!.setOnBindEditTextListener(EditTextPreferenceModifiers.Port)
        for (key in listOf("psk", "userKey")) {
            findPreference<EditTextPreference>(key)!!.summaryProvider = PasswordSummaryProvider
        }
        val version = findPreference<SimpleMenuPreference>("version")!!
        val obfs = findPreference<SimpleMenuPreference>("obfsMode")!!
        val host = findPreference<EditTextPreference>("obfsHost")!!
        val mode = findPreference<SimpleMenuPreference>("mode")!!
        fun update(versionValue: String, obfsValue: String) {
            obfs.isVisible = versionValue == "4"
            host.isVisible = versionValue == "4" && obfsValue in listOf("http", "tls")
            mode.isVisible = versionValue == "6"
        }
        update(version.value, obfs.value)
        version.setOnPreferenceChangeListener { _, value -> update(value.toString(), obfs.value); true }
        obfs.setOnPreferenceChangeListener { _, value -> update(version.value, value.toString()); true }
    }
}
