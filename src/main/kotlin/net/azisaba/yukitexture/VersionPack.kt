package net.azisaba.yukitexture

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
        private val VERSION_TO_PROTOCOL = mapOf(
            "1.7" to 4, "1.7.2" to 4, "1.7.4" to 4, "1.7.5" to 4,
            "1.7.6" to 5, "1.7.7" to 5, "1.7.8" to 5, "1.7.9" to 5, "1.7.10" to 5,
            "1.8" to 47, "1.8.8" to 47, "1.8.9" to 47,
            "1.9" to 107, "1.9.1" to 108, "1.9.2" to 109, "1.9.3" to 110, "1.9.4" to 110,
            "1.10" to 210, "1.10.1" to 210, "1.10.2" to 210,
            "1.11" to 315, "1.11.1" to 316, "1.11.2" to 316,
            "1.12" to 335, "1.12.1" to 338, "1.12.2" to 340,
            "1.13" to 393, "1.13.1" to 401, "1.13.2" to 404,
            "1.14" to 477, "1.14.1" to 480, "1.14.2" to 485, "1.14.3" to 490, "1.14.4" to 498,
            "1.15" to 573, "1.15.1" to 575, "1.15.2" to 578,
            "1.16" to 735, "1.16.1" to 736, "1.16.2" to 751, "1.16.3" to 753, "1.16.4" to 754, "1.16.5" to 754,
            "1.17" to 755, "1.17.1" to 756,
            "1.18" to 757, "1.18.1" to 757, "1.18.2" to 758,
            "1.19" to 759, "1.19.1" to 760, "1.19.2" to 760, "1.19.3" to 761, "1.19.4" to 762,
            "1.20" to 763, "1.20.1" to 763, "1.20.2" to 764, "1.20.3" to 765, "1.20.4" to 765, "1.20.5" to 766, "1.20.6" to 766,
            "1.21" to 767, "1.21.1" to 767, "1.21.2" to 768, "1.21.3" to 768, "1.21.4" to 769
        )

        fun parseProtocol(value: Any?): Int? {
            if (value == null) return null
            if (value is Number) return value.toInt()
            val str = value.toString().trim()
            val directInt = str.toIntOrNull()
            if (directInt != null) return directInt

            val clean = str.removePrefix("v").removePrefix("V")
            val candidates = mutableListOf(str, clean)
            if (!clean.startsWith("1.") && clean.matches(Regex("""^\d+(\..*)?$"""))) {
                candidates.add("1.$clean")
            } else if (clean.startsWith("1.")) {
                candidates.add(clean.removePrefix("1."))
            }

            val dots = clean.split(".")
            if (dots.size >= 2) {
                candidates.add("${dots[0]}.${dots[1]}")
            }

            for (candidate in candidates.distinct()) {
                val directMatch = VERSION_TO_PROTOCOL[candidate]
                if (directMatch != null) return directMatch
            }

            return resolveWithViaVersion(candidates)
        }

        private fun resolveWithViaVersion(candidates: List<String>): Int? {
            try {
                val clazz = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion")
                val getClosestMethod = clazz.getMethod("getClosest", String::class.java)
                val getVersionMethod = clazz.getMethod("getVersion")

                for (candidate in candidates.distinct()) {
                    val pv = getClosestMethod.invoke(null, candidate)
                    if (pv != null) {
                        val ver = getVersionMethod.invoke(pv) as? Int
                        if (ver != null && ver != -1) {
                            return ver
                        }
                    }
                }
            } catch (_: Throwable) {
            }

            try {
                val clazz = Class.forName("us.myles.ViaVersion.api.protocol.ProtocolVersion")
                val getClosestMethod = clazz.getMethod("getClosest", String::class.java)
                val getVersionMethod = clazz.getMethod("getVersion")

                for (candidate in candidates.distinct()) {
                    val pv = getClosestMethod.invoke(null, candidate)
                    if (pv != null) {
                        val ver = getVersionMethod.invoke(pv) as? Int
                        if (ver != null && ver != -1) {
                            return ver
                        }
                    }
                }
            } catch (_: Throwable) {
            }

            return null
        }
    }
}
