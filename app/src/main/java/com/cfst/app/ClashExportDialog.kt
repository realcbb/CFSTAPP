package com.cfst.app

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cfst.app.model.SpeedTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ClashExportDialog(results: List<SpeedTestResult>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("clash_export", Context.MODE_PRIVATE) }
    var uuid by rememberSaveable { mutableStateOf(prefs.getString("uuid", "").orEmpty()) }
    var sni by rememberSaveable { mutableStateOf(prefs.getString("sni", "").orEmpty()) }
    var host by rememberSaveable { mutableStateOf(prefs.getString("host", "").orEmpty()) }
    var path by rememberSaveable { mutableStateOf(prefs.getString("path", "/?ed=2048").orEmpty()) }
    var port by rememberSaveable { mutableStateOf(prefs.getString("port", "443").orEmpty()) }
    var mixed by rememberSaveable { mutableStateOf(prefs.getString("mixed", "7890").orEmpty()) }
    var auto by rememberSaveable { mutableStateOf(prefs.getBoolean("auto", true)) }
    var selected by rememberSaveable { mutableStateOf(ArrayList(results.map { it.ipAddress }.distinct())) }
    var pendingYaml by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/yaml")) { uri ->
        if (uri != null) {
            val snapshot = pendingYaml
            saving = true
            scope.launch {
                val outcome = withContext(Dispatchers.IO) {
                    runCatching {
                        require(snapshot.isNotEmpty()) { "配置内容已失效，请重新导出" }
                        val output = context.contentResolver.openOutputStream(uri, "wt")
                            ?: kotlin.error("无法打开保存位置")
                        output.bufferedWriter(Charsets.UTF_8).use { it.write(snapshot) }
                    }
                }
                saving = false
                pendingYaml = ""
                outcome.onSuccess {
                    Toast.makeText(context, "YAML 已保存，可导入 Clash Meta", Toast.LENGTH_LONG).show()
                    onDismiss()
                }.onFailure { error = "保存失败：${it.localizedMessage}" }
            }
        } else pendingYaml = ""
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("导出 Clash Meta", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("VLESS · WebSocket · TLS", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text("连接参数保存在本机；每个选中 IP 生成一个节点。", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(uuid, { uuid = it.trim() }, label = { Text("UUID") }, singleLine = true)
                OutlinedTextField(sni, { sni = it.trim() }, label = { Text("TLS SNI 域名") }, singleLine = true)
                OutlinedTextField(host, { host = it.trim() }, label = { Text("WebSocket Host") }, singleLine = true)
                OutlinedTextField(path, { path = it }, label = { Text("WebSocket 路径") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(port, { port = it }, label = { Text("TLS 端口") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(mixed, { mixed = it }, label = { Text("本地端口") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(auto, { auto = it })
                    Spacer(Modifier.width(8.dp))
                    Text("启用自动选择节点")
                }
                Text("测速 IP · 已选 ${selected.size} / ${results.map { it.ipAddress }.distinct().size}", fontWeight = FontWeight.Bold)
                Row {
                    TextButton(onClick = { selected = ArrayList(results.map { it.ipAddress }.distinct()) }) { Text("全选") }
                    TextButton(onClick = { selected = arrayListOf() }) { Text("清空") }
                }
                results.distinctBy { it.ipAddress }.forEach { result ->
                    val toggle = {
                        selected = ArrayList(if (result.ipAddress in selected) selected - result.ipAddress else selected + result.ipAddress)
                    }
                    Row(Modifier.fillMaxWidth().clickable { toggle() }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(result.ipAddress in selected, { toggle() })
                        Column {
                            Text(result.ipAddress, style = MaterialTheme.typography.bodyMedium)
                            Text("${result.avgLatency} ms · ${result.downloadSpeed} MB/s", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = selected.isNotEmpty() && !saving, onClick = {
                val options = ClashOptions(uuid, sni, host, path, port, mixed, auto)
                val generated = runCatching { ClashConfig.generate(selected, options) }
                generated.onSuccess { yaml ->
                    error = null
                    prefs.edit().putString("uuid", uuid).putString("sni", sni).putString("host", host)
                        .putString("path", path).putString("port", port).putString("mixed", mixed)
                        .putBoolean("auto", auto).apply()
                    pendingYaml = yaml
                    launcher.launch("cfst-config.yaml")
                }.onFailure { error = it.localizedMessage }
            }) { Text(if (saving) "保存中…" else "保存 YAML") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("取消") } }
    )
}
