package net.azisaba.yukitexture

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion

data class VersionPack(
    val minProtocol: Int?,
    val maxProtocol: Int?,
    val url: String,
    @Volatile var sha1: String? = null
) {
    fun matches(protocolVersion: Int): Boolean {
        if (minProtocol != null && protocolVersion < minProtocol) return false
        if (maxProtocol != null && protocolVersion > maxProtocol) return false
        return true
    }

    companion object {
        fun parseProtocol(value: Any?): Int? {
            if (value == null) return null
            if (value is Number) return value.toInt()
            val str = value.toString().trim()
            val directInt = str.toIntOrNull()
            if (directInt != null) return directInt

            try {
                val clean = str.removePrefix("v")
                val candidates = mutableListOf(str, clean)
                if (!clean.startsWith("1.") && clean.matches(Regex("""^\d+(\..*)?$"""))) {
                    candidates.add("1.$clean")
                } else if (clean.startsWith("1.")) {
                    candidates.add(clean.removePrefix("1."))
                }

                for (candidate in candidates.distinct()) {
                    val pv = ProtocolVersion.getClosest(candidate)
                    if (pv != null && pv != ProtocolVersion.unknown) {
                        return pv.version
                    }
                }
            } catch (_: Throwable) {
            }

            return null
        }
    }
}
