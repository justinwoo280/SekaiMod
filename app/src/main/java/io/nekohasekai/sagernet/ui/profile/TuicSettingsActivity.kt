package io.nekohasekai.sagernet.ui.profile

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.fmt.tuic.TuicBean
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type

class TuicSettingsActivity : ProfileSettingsActivity<TuicBean>() {

    override fun createEntity() = TuicBean().applyDefaultValues()
    private val extra = PreferenceBindingManager().apply {
        add(PreferenceBinding(Type.Bool, "udpOverStream"))
        add(PreferenceBinding(Type.Text, "heartbeat"))
    }

    override fun TuicBean.init() {
        extra.writeToCacheAll(this)
        DataStore.profileName = name
        DataStore.serverAddress = serverAddress
        DataStore.serverPort = serverPort
        DataStore.serverUsername = uuid
        DataStore.serverPassword = token
        DataStore.serverALPN = alpn
        DataStore.serverCertificates = caText
        DataStore.serverUDPRelayMode = udpRelayMode
        DataStore.serverCongestionController = congestionController
        DataStore.serverDisableSNI = disableSNI
        DataStore.serverSNI = sni
        DataStore.serverReduceRTT = reduceRTT
        DataStore.serverAllowInsecure = allowInsecure
        DataStore.profileCacheStore.putBoolean("enableECH", enableECH)
        DataStore.profileCacheStore.putString("echConfig", echConfig)
        DataStore.profileCacheStore.putString("echQueryServerName", echQueryServerName)
    }

    override fun TuicBean.serialize() {
        extra.fromCacheAll(this)
        name = DataStore.profileName
        serverAddress = DataStore.serverAddress
        serverPort = DataStore.serverPort
        uuid = DataStore.serverUsername
        token = DataStore.serverPassword
        alpn = DataStore.serverALPN
        caText = DataStore.serverCertificates
        udpRelayMode = DataStore.serverUDPRelayMode
        congestionController = DataStore.serverCongestionController
        disableSNI = DataStore.serverDisableSNI
        sni = DataStore.serverSNI
        reduceRTT = DataStore.serverReduceRTT
        allowInsecure = DataStore.serverAllowInsecure
        enableECH = DataStore.profileCacheStore.getBoolean("enableECH")
        echConfig = DataStore.profileCacheStore.getString("echConfig") ?: ""
        echQueryServerName = DataStore.profileCacheStore.getString("echQueryServerName") ?: ""
    }

    override fun PreferenceFragmentCompat.createPreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        addPreferencesFromResource(R.xml.tuic_preferences)
        extra.setPreferenceFragment(this)
        val udpOverStream = findPreference<SwitchPreference>("udpOverStream")!!
        val udpRelayMode = findPreference<androidx.preference.Preference>(Key.SERVER_UDP_RELAY_MODE)!!
        udpRelayMode.isEnabled = !udpOverStream.isChecked
        udpOverStream.setOnPreferenceChangeListener { _, value ->
            udpRelayMode.isEnabled = value != true
            true
        }

        val disableSNI = findPreference<SwitchPreference>(Key.SERVER_DISABLE_SNI)!!
        val sni = findPreference<EditTextPreference>(Key.SERVER_SNI)!!
        sni.isEnabled = !disableSNI.isChecked
        disableSNI.setOnPreferenceChangeListener { _, newValue ->
            sni.isEnabled = !(newValue as Boolean)
            true
        }

        findPreference<EditTextPreference>(Key.SERVER_PASSWORD)!!.apply {
            summaryProvider = PasswordSummaryProvider
        }
    }

}
