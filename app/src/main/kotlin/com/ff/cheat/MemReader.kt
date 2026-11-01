package com.ff.cheat

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MemReader {
    private var pid: Int = -1
    var baseAddr: Long = 0L
        private set

    object Offsets {
        // FILL: atualiza após cada patch com Il2CppDumper ou GameGuardian
        const val PLAYER_LIST   = 0x04FC4430L
        const val PLAYER_COUNT  = 0x04FC4438L
        const val LOCAL_PLAYER  = 0x04FC4420L
        const val CAM_VIEWMAT   = 0x0523A200L
        const val HEALTH        = 0x000001A4L
        const val MAX_HEALTH    = 0x000001A8L
        const val IS_DEAD       = 0x00000204L
        const val POS_X         = 0x000000B8L
        const val POS_Y         = 0x000000BCL
        const val POS_Z         = 0x000000C0L
        const val BONE_LIST_PTR = 0x00000350L
        const val HEAD_BONE_IDX = 0x18L
        const val RECOIL_VEC    = 0x00000310L
    }

    fun init(): Boolean {
        pid = findPid() ?: return false
        baseAddr = findBase(pid, "libil2cpp.so") ?: return false
        return true
    }

    fun isReady() = pid > 0 && baseAddr != 0L

    fun readFloat(addr: Long): Float {
        val b = readBytes(addr, 4) ?: return 0f
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).float
    }

    fun readInt32(addr: Long): Int {
        val b = readBytes(addr, 4) ?: return 0
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
    }

    fun readInt64(addr: Long): Long {
        val b = readBytes(addr, 8) ?: return 0L
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).long
    }

    fun readVec3(addr: Long) = Triple(readFloat(addr), readFloat(addr + 4), readFloat(addr + 8))

    fun readMatrix4x4(addr: Long): FloatArray {
        val out = FloatArray(16)
        val b = readBytes(addr, 64) ?: return out
        val bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        repeat(16) { out[it] = bb.float }
        return out
    }

    private fun readBytes(addr: Long, size: Int): ByteArray? {
        return try {
            RandomAccessFile("/proc/$pid/mem", "r").use { f ->
                f.seek(addr); val buf = ByteArray(size); f.read(buf); buf
            }
        } catch (_: Exception) {
            val hex = shell("dd if=/proc/$pid/mem bs=1 skip=$addr count=$size 2>/dev/null | xxd -p")
                ?.replace("\n", "")?.trim() ?: return null
            if (hex.length < size * 2) return null
            ByteArray(size) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        }
    }

    fun shell(cmd: String): String? {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val out = p.inputStream.bufferedReader().readText().trim()
            p.waitFor(); out.ifEmpty { null }
        } catch (_: Exception) { null }
    }

    private fun findPid(): Int? {
        val out = shell("pidof com.dts.freefireth") ?: shell("pidof com.dts.freefiremax") ?: return null
        return out.split("\\s+".toRegex()).firstOrNull()?.trim()?.toIntOrNull()
    }

    private fun findBase(pid: Int, lib: String): Long? {
        val maps = shell("grep '$lib' /proc/$pid/maps") ?: return null
        val line = maps.lines().firstOrNull { it.contains("r-xp") || it.contains("r--p") } ?: return null
        return line.trim().split("\\s+".toRegex()).firstOrNull()?.split("-")?.firstOrNull()?.toLongOrNull(16)
    }
}
