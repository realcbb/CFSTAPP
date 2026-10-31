package com.cfst.app

data class ClashOptions(
    val uuid: String = "", val sni: String = "", val host: String = "",
    val path: String = "/?ed=2048", val port: String = "443",
    val mixedPort: String = "7890", val autoSelect: Boolean = true
) {
    fun error(): String? = when {
        !Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}").matches(uuid) -> "请填写有效的 UUID"
        sni.isBlank() || sni.any { it.isWhitespace() || it in "/:\\" } -> "请填写有效的 TLS SNI 域名"
        host.isBlank() || host.any { it.isWhitespace() || it in "/\\" } -> "请填写有效的 WebSocket Host"
        !path.startsWith("/") || path.any { it.isISOControl() } -> "WS 路径须以 / 开头且不含控制字符"
        (port.toIntOrNull() ?: 0) !in 1..65535 || (mixedPort.toIntOrNull() ?: 0) !in 1..65535 -> "端口须为 1–65535"
        else -> null
    }
}

object ClashConfig {
    // Single-quoted YAML scalars escape apostrophes by doubling them.
    private fun quote(value: String) = "'" + value.replace("'", "''") + "'"

    fun generate(addresses: List<String>, options: ClashOptions): String {
        require(options.error() == null) { options.error().orEmpty() }
        val ips = addresses.distinct()
        require(ips.isNotEmpty()) { "请至少选择一个 IP" }
        require(ips.all { ip ->
            !ip.contains('%') && if (ip.contains(':')) {
                ip.matches(Regex("[0-9a-fA-F:]+")) &&
                    runCatching { java.net.InetAddress.getByName(ip) is java.net.Inet6Address }.getOrDefault(false)
            } else {
                ip.split('.').let { parts -> parts.size == 4 && parts.all {
                    it.matches(Regex("0|[1-9][0-9]{0,2}")) && (it.toIntOrNull() ?: 256) <= 255
                } }
            }
        }) { "结果包含无效 IP" }
        val names = ips.indices.map { "CF-IP${it + 1}" }
        val list = names.joinToString("\n") { "      - $it" }
        return buildString {
            appendLine("# Clash Meta / mihomo · CFST 本地生成")
            appendLine("mixed-port: ${options.mixedPort.toInt()}")
            appendLine("allow-lan: false\nmode: rule\nlog-level: warning\nipv6: false\nunified-delay: true\ntcp-concurrent: true\nfind-process-mode: off")
            appendLine("""

geodata-mode: true
geo-auto-update: true
geo-update-interval: 168
geox-url:
  geoip: https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geoip.dat
  geosite: https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geosite.dat
  mmdb: https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/country.mmdb
dns:
  enable: true
  ipv6: false
  enhanced-mode: fake-ip
  fake-ip-range: 198.18.0.1/16
  fake-ip-filter: ['*.lan', '*.local', '+.msftconnecttest.com', '+.msftncsi.com', 'time.*.com', 'ntp.*.com']
  default-nameserver: [223.5.5.5, 119.29.29.29]
  nameserver: [https://dns.alidns.com/dns-query, https://doh.pub/dns-query]
  proxy-server-nameserver: [https://dns.alidns.com/dns-query]
  nameserver-policy:
    'geosite:geolocation-!cn': [https://1.1.1.1/dns-query, https://8.8.8.8/dns-query]

proxies:
""".trimIndent())
            ips.forEachIndexed { i, ip ->
                appendLine("  - name: ${names[i]}")
                appendLine("    type: vless\n    server: ${quote(ip)}\n    port: ${options.port.toInt()}")
                appendLine("    uuid: ${quote(options.uuid)}\n    tls: true\n    servername: ${quote(options.sni)}")
                appendLine("    client-fingerprint: chrome\n    network: ws\n    ws-opts:")
                appendLine("      path: ${quote(options.path)}\n      headers:\n        Host: ${quote(options.host)}")
            }
            appendLine("\nproxy-groups:\n  - name: 🚀 节点选择\n    type: select\n    proxies:")
            if (options.autoSelect) appendLine("      - ♻️ 自动选择")
            appendLine(list)
            appendLine("      - DIRECT")
            if (options.autoSelect) {
                appendLine("  - name: ♻️ 自动选择\n    type: url-test\n    proxies:")
                appendLine(list)
                appendLine("    url: https://www.gstatic.com/generate_204\n    interval: 300\n    tolerance: 50")
            }
            appendLine("  - name: 🤖 AI服务\n    type: select\n    proxies:\n      - 🚀 节点选择")
            appendLine(list)
            appendLine("  - name: 🐟 漏网之鱼\n    type: select\n    proxies:\n      - 🚀 节点选择\n      - DIRECT")
            appendLine("""

rules:
  - GEOSITE,category-ads-all,REJECT
  - GEOSITE,private,DIRECT
  - GEOSITE,openai,🤖 AI服务
  - GEOSITE,anthropic,🤖 AI服务
  - GEOSITE,google-gemini,🤖 AI服务
  - GEOSITE,apple-cn,DIRECT
  - GEOSITE,microsoft@cn,DIRECT
  - GEOSITE,geolocation-!cn,🚀 节点选择
  - GEOSITE,cn,DIRECT
  - GEOIP,private,DIRECT,no-resolve
  - GEOIP,CN,DIRECT
  - MATCH,🐟 漏网之鱼
""".trimIndent())
        }
    }
}
