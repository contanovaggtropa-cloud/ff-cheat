package com.ff.cheat

import android.graphics.Canvas
import kotlinx.coroutines.*

class CheatCore {
    @Volatile var espEnabled      = true
    @Volatile var snapEnabled     = true
    @Volatile var aimbotEnabled   = false
    @Volatile var noRecoilEnabled = true

    var screenW = 1080
    var screenH = 2400

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    @Volatile private var players  = listOf<PlayerData>()
    @Volatile private var viewProj = FloatArray(16)
    @Volatile var nearestTarget: PlayerData? = null
        private set

    fun start() {
        if (!MemReader.init()) return
        job = scope.launch { while (isActive) { runCatching { tick() }; delay(10L) } }
    }

    fun stop() { job?.cancel(); players = emptyList(); nearestTarget = null }

    private fun tick() {
        val base = MemReader.baseAddr
        viewProj = MemReader.readMatrix4x4(base + MemReader.Offsets.CAM_VIEWMAT)
        val count    = MemReader.readInt32(base + MemReader.Offsets.PLAYER_COUNT).coerceIn(0, 50)
        val listPtr  = MemReader.readInt64(base + MemReader.Offsets.PLAYER_LIST)
        val localPtr = MemReader.readInt64(base + MemReader.Offsets.LOCAL_PLAYER)
        val (lx, ly, lz) = MemReader.readVec3(localPtr + MemReader.Offsets.POS_X)
        val newList = mutableListOf<PlayerData>()
        var nearDist = Float.MAX_VALUE; var nearPd: PlayerData? = null
        for (i in 0 until count) {
            val pPtr = MemReader.readInt64(listPtr + i * 8L)
            if (pPtr == 0L || pPtr == localPtr) continue
            val dead = MemReader.readInt32(pPtr + MemReader.Offsets.IS_DEAD) != 0
            val hp   = MemReader.readFloat(pPtr + MemReader.Offsets.HEALTH)
            if (dead || hp <= 0f) continue
            val maxHp = MemReader.readFloat(pPtr + MemReader.Offsets.MAX_HEALTH)
            val (wx, wy, wz) = MemReader.readVec3(pPtr + MemReader.Offsets.POS_X)
            val bonePtr = MemReader.readInt64(pPtr + MemReader.Offsets.BONE_LIST_PTR)
            val (hx, hy, hz) = MemReader.readVec3(bonePtr + MemReader.Offsets.HEAD_BONE_IDX)
            val feetScr = EspRenderer.w2s(wx, wy, wz, viewProj, screenW, screenH) ?: continue
            val headScr = EspRenderer.w2s(hx, hy, hz, viewProj, screenW, screenH) ?: continue
            val dist = Math.sqrt(((wx-lx)*(wx-lx)+(wy-ly)*(wy-ly)+(wz-lz)*(wz-lz)).toDouble()).toFloat()
            val pd = PlayerData(wx,wy,wz,hx,hy,hz,hp,maxHp,
                feetScr.first,feetScr.second,headScr.first,headScr.second,dist)
            newList.add(pd)
            if (dist < nearDist) { nearDist = dist; nearPd = pd }
        }
        players = newList
        nearestTarget = if (aimbotEnabled) nearPd else null
        if (noRecoilEnabled && localPtr != 0L) {
            val pid = MemReader.shell("pidof com.dts.freefireth")
                ?: MemReader.shell("pidof com.dts.freefiremax") ?: return
            val addr = localPtr + MemReader.Offsets.RECOIL_VEC
            MemReader.shell("printf '\\x00%.0s' {1..12} | dd of=/proc/$pid/mem bs=1 seek=$addr conv=notrunc 2>/dev/null")
        }
    }

    fun drawEsp(canvas: Canvas) {
        if (espEnabled) EspRenderer.render(canvas, players, screenW, screenH, snapEnabled)
    }
}
