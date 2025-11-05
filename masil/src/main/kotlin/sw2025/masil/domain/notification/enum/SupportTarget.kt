package sw2025.masil.domain.notification.enum

enum class SupportTarget(val displayName: String) {
    SMALL_BUSINESS_OWNER("소상공인"),
    SMALL_ENTERPRISE("소공인"),
    TRADITIONAL_MARKET("전통시장"),
    PROSPECTIVE_ENTREPRENEUR("예비창업자"),
    CLOSED_SMALL_BUSINESS_OWNER("소상공인(폐업)"),
    ETC("기타");

    companion object {
        fun fromDisplayName(name: String): SupportTarget? =
            values().find { it.displayName == name }
    }
}
