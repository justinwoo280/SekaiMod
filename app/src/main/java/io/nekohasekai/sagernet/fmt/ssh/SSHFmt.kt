package io.nekohasekai.sagernet.fmt.ssh

import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma

fun buildSingBoxOutboundSSHBean(bean: SSHBean): SingBoxOptions.Outbound_SSHOptions {
    return SingBoxOptions.Outbound_SSHOptions().apply {
        type = "ssh"
        server = bean.serverAddress
        server_port = bean.serverPort
        user = bean.username
        if (bean.hostKeyAlgorithms.isNotBlank()) host_key_algorithms = bean.hostKeyAlgorithms.listByLineOrComma()
        if (bean.clientVersion.isNotBlank()) client_version = bean.clientVersion
        if (bean.ciphers.isNotBlank()) cipher = bean.ciphers.listByLineOrComma()
        if (bean.macs.isNotBlank()) mac = bean.macs.listByLineOrComma()
        if (bean.kexAlgorithms.isNotBlank()) kex_algorithm = bean.kexAlgorithms.listByLineOrComma()
        if (bean.publicKey.isNotBlank()) {
            host_key = bean.publicKey.listByLineOrComma()
        }
        when (bean.authType) {
            SSHBean.AUTH_TYPE_PRIVATE_KEY -> {
                private_key = bean.privateKey
                private_key_passphrase = bean.privateKeyPassphrase
            }
            else -> {
                password = bean.password
            }
        }
    }
}
