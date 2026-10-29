package com.engbase.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object UpdateApi {
    private const val LATEST =
        "https://api.github.com/repos/mundoarkade1-arch/eng-base/releases/latest"

    class Info(val code: Int, val name: String, val apkUrl: String, val shaUrl: String)

    fun fetchLatest(): Info {
        val json = JSONObject(httpGet(LATEST))
        val tag = json.getString("tag_name")
        val code = tag.removePrefix("build-").toIntOrNull()
            ?: throw IllegalStateException("Versão desconhecida: $tag")
        val assets = json.getJSONArray("assets")
        var apk: String? = null
        var sha: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            when (a.getString("name")) {
                "EngBase.apk" -> apk = a.getString("browser_download_url")
                "EngBase.apk.sha256" -> sha = a.getString("browser_download_url")
            }
        }
        if (apk == null) throw IllegalStateException("A versão nova não tem APK")
        if (sha == null) throw IllegalStateException("A versão nova não tem verificação de segurança")
        return Info(code, json.optString("name", tag), apk, sha)
    }

    fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "EngBase")
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("Erro $code ao procurar atualização")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    fun download(url: String, dest: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.setRequestProperty("User-Agent", "EngBase")
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("Erro $code ao baixar")
            conn.inputStream.use { input -> dest.outputStream().use { out -> input.copyTo(out) } }
        } finally {
            conn.disconnect()
        }
    }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

enum class UpStatus { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, NEED_PERMISSION, ERROR }

class UpdateViewModel(private val app: Application) : AndroidViewModel(app) {
    var status by mutableStateOf(UpStatus.IDLE)
        private set
    var message by mutableStateOf("")
        private set

    private var info: UpdateApi.Info? = null
    private var apkFile: File? = null

    val installedName: String
        get() = try {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }

    private fun installedCode(): Long =
        PackageInfoCompat.getLongVersionCode(app.packageManager.getPackageInfo(app.packageName, 0))

    init {
        check(false)
    }

    fun check(manual: Boolean) {
        if (status == UpStatus.CHECKING || status == UpStatus.DOWNLOADING) return
        status = UpStatus.CHECKING
        message = ""
        viewModelScope.launch {
            try {
                val i = withContext(Dispatchers.IO) { UpdateApi.fetchLatest() }
                info = i
                if (i.code > installedCode()) {
                    status = UpStatus.AVAILABLE
                    message = i.name
                } else {
                    status = UpStatus.UP_TO_DATE
                }
            } catch (e: Exception) {
                if (manual) {
                    status = UpStatus.ERROR
                    message = e.message ?: "Não consegui procurar atualização"
                } else {
                    status = UpStatus.IDLE
                }
            }
        }
    }

    fun downloadAndInstall() {
        val i = info ?: return
        if (status == UpStatus.DOWNLOADING) return
        status = UpStatus.DOWNLOADING
        message = ""
        viewModelScope.launch {
            try {
                val f = withContext(Dispatchers.IO) {
                    val dir = File(app.cacheDir, "updates").apply { mkdirs() }
                    val file = File(dir, "EngBase.apk")
                    UpdateApi.download(i.apkUrl, file)
                    val expected = UpdateApi.httpGet(i.shaUrl).trim().split(Regex("\\s+"))[0].lowercase()
                    val got = UpdateApi.sha256(file)
                    if (expected != got) {
                        file.delete()
                        throw IllegalStateException("O arquivo baixado não confere. Não vou instalar.")
                    }
                    file
                }
                apkFile = f
                install()
            } catch (e: Exception) {
                status = UpStatus.ERROR
                message = e.message ?: "Erro ao baixar a atualização"
            }
        }
    }

    fun install() {
        val f = apkFile ?: return
        if (!app.packageManager.canRequestPackageInstalls()) {
            status = UpStatus.NEED_PERMISSION
            return
        }
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", f)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(intent)
        status = UpStatus.AVAILABLE
    }

    fun openPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${app.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(intent)
    }
}

@Composable
fun UpdateBanner(vm: UpdateViewModel) {
    when (vm.status) {
        UpStatus.AVAILABLE -> Card(Modifier.fillMaxWidth().padding(8.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Nova versão disponível: ${vm.message}", style = MaterialTheme.typography.titleSmall)
                Button(onClick = { vm.downloadAndInstall() }) { Text("Atualizar agora") }
            }
        }
        UpStatus.DOWNLOADING -> Card(Modifier.fillMaxWidth().padding(8.dp)) {
            Text("Baixando e conferindo a atualização...", Modifier.padding(12.dp))
        }
        UpStatus.NEED_PERMISSION -> Card(Modifier.fillMaxWidth().padding(8.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Para instalar, o Android precisa da sua permissão para o Eng base instalar apps.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.openPermissionSettings() }) { Text("Abrir permissão") }
                    TextButton(onClick = { vm.install() }) { Text("Já permiti, instalar") }
                }
            }
        }
        UpStatus.ERROR -> Card(Modifier.fillMaxWidth().padding(8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(vm.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { vm.check(true) }) { Text("Tentar de novo") }
            }
        }
        else -> Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (vm.status == UpStatus.UP_TO_DATE) "Versão ${vm.installedName} (a mais nova)"
                else "Versão ${vm.installedName}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            TextButton(onClick = { vm.check(true) }) { Text("Procurar atualização") }
        }
    }
}
