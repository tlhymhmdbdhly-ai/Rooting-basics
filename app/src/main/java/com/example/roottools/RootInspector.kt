package com.example.roottools

import android.os.Build
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class RootManager {
    MAGISK,
    KERNELSU,
    APATCH,
    UNKNOWN
}

enum class MountAccess {
    READ_ONLY,
    READ_WRITE,
    UNKNOWN
}

data class DeviceReport(
    val hasRoot: Boolean,
    val rootManager: RootManager,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val partitions: Map<String, MountAccess>
)

/** فحص معلوماتي فقط؛ لا ينفذ remount أو يكتب إلى ملفات النظام. */
object RootInspector {
    private val partitionsToCheck = listOf("/system", "/vendor", "/product")

    suspend fun inspect(): DeviceReport = withContext(Dispatchers.IO) {
        val shellResult = runCatching { Shell.getShell() }
        val hasRoot = shellResult.getOrNull()?.isRoot == true
        val manager = if (hasRoot) detectRootManager() else RootManager.UNKNOWN
        val mountInfo = if (hasRoot) readMountInfo() else null

        DeviceReport(
            hasRoot = hasRoot,
            rootManager = manager,
            manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "غير معروف" },
            model = Build.MODEL.orEmpty().ifBlank { "غير معروف" },
            androidVersion = Build.VERSION.RELEASE.orEmpty().ifBlank { "غير معروف" },
            partitions = partitionsToCheck.associateWith { path ->
                mountInfo?.let { accessForPath(it, path) } ?: MountAccess.UNKNOWN
            }
        )
    }

    private fun detectRootManager(): RootManager {
        val command = """
            if [ -e /data/adb/ksu ] || [ -e /data/adb/ksud ] || command -v ksud >/dev/null 2>&1; then
                echo KERNELSU
            elif [ -e /data/adb/ap ] || [ -e /data/adb/apd ] || command -v apd >/dev/null 2>&1; then
                echo APATCH
            elif [ -e /data/adb/magisk ] || [ -e /sbin/.magisk ] || command -v magisk >/dev/null 2>&1; then
                echo MAGISK
            else
                echo UNKNOWN
            fi
        """.trimIndent()
        val result = runCatching { Shell.cmd(command).exec() }.getOrNull()
            ?: return RootManager.UNKNOWN
        if (!result.isSuccess) return RootManager.UNKNOWN

        return when (result.out.joinToString("").trim()) {
            "MAGISK" -> RootManager.MAGISK
            "KERNELSU" -> RootManager.KERNELSU
            "APATCH" -> RootManager.APATCH
            else -> RootManager.UNKNOWN
        }
    }

    private fun readMountInfo(): String? {
        val result = runCatching { Shell.cmd("cat /proc/self/mountinfo").exec() }.getOrNull()
            ?: return null
        return if (result.isSuccess) result.out.joinToString("\n") else null
    }

    private fun accessForPath(mountInfo: String, path: String): MountAccess {
        val matchingMount = mountInfo.lineSequence()
            .mapNotNull { line ->
                val fields = line.split(' ')
                if (fields.size < 10) return@mapNotNull null
                val mountPoint = fields[4]
                    .replace("\\040", " ")
                    .replace("\\011", "\t")
                    .replace("\\134", "\\")
                val matches = path == mountPoint ||
                    (mountPoint == "/" && path.startsWith("/")) ||
                    path.startsWith(mountPoint.trimEnd('/') + "/")
                if (matches) mountPoint to fields else null
            }
            .maxByOrNull { it.first.length }
            ?: return MountAccess.UNKNOWN

        val fields = matchingMount.second
        val separatorIndex = fields.indexOf("-")
        val mountOptions = fields.getOrNull(5).orEmpty().split(',')
        val superOptions = if (separatorIndex >= 0) fields.lastOrNull().orEmpty().split(',') else emptyList()
        return when {
            "rw" in mountOptions || "rw" in superOptions -> MountAccess.READ_WRITE
            "ro" in mountOptions || "ro" in superOptions -> MountAccess.READ_ONLY
            else -> MountAccess.UNKNOWN
        }
    }
}
