package sw2025.masil.domain.notification.enum

enum class RelatedInstitution(val displayName: String) {
    EMPLOYMENT_LABOR("고용노동부"),
    SCIENCE_TECH("과기부"),
    LAND_TRANSPORT("국토교통부"),
    FINANCIAL_INSTITUTION("금융기관"),
    SMES_MINISTRY("중기부"),
    OTHER("그 외 기관");

    companion object {
        fun fromDisplayName(name: String): RelatedInstitution? =
            values().find { it.displayName == name }
    }
}
