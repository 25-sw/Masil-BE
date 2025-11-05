package sw2025.masil.domain.notification.enum

enum class IndustryType(val displayName: String) {
    AGRICULTURE_FORESTRY_FISHERIES("농업, 임업 및 어업"),
    MINING("광업"),
    MANUFACTURING("제조업"),
    ELECTRICITY_GAS_AIR_CONDITIONING("전기, 가스, 증기 및 공기 조절 공급업"),
    WATER_WASTE_MANAGEMENT("수도, 하수 및 폐기물 처리, 원료 재생업"),
    CONSTRUCTION("건설업"),
    WHOLESALE_RETAIL("도매 및 소매업"),
    TRANSPORT_STORAGE("운수 및 창고업"),
    ACCOMMODATION_FOOD("숙박 및 음식점업"),
    INFORMATION_COMMUNICATION("정보통신업"),
    FINANCE_INSURANCE("금융 및 보험업"),
    REAL_ESTATE("부동산업"),
    PROFESSIONAL_SCIENTIFIC_TECHNICAL("전문, 과학 및 기술 서비스업"),
    FACILITY_MANAGEMENT_RENTAL("사업시설 관리, 사업 지원 및 임대 서비스업"),
    PUBLIC_ADMIN_SOCIAL_SECURITY("공공 행정, 국방 및 사회보장 행정"),
    EDUCATION("교육 서비스업"),
    HEALTH_SOCIAL_WORK("보건업 및 사회복지 서비스업"),
    ARTS_SPORTS_RECREATION("예술, 스포츠 및 여가관련 서비스업"),
    ASSOCIATION_REPAIR_OTHER_SERVICES("협회 및 단체, 수리 및 기타 개인 서비스업"),
    HOUSEHOLD_EMPLOYMENT_PRODUCTION("가구내 고용활동 및 달리 분류되지 않은 자가소비 생산활동"),
    INTERNATIONAL_ORGANIZATION("국제 및 외국기관");

    companion object {
        fun fromDisplayName(name: String): IndustryType? =
            values().find { it.displayName == name }
    }
}
