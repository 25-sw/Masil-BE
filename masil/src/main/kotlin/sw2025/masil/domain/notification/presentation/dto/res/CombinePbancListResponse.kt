package sw2025.masil.domain.notification.presentation.dto.res

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/* -------------------- 원본 매핑 DTO (API 구조 그대로) -------------------- */

@JsonIgnoreProperties(ignoreUnknown = true)
data class CombinePbancApiResponse(
    val result: Boolean? = null,
    val message: String? = null,
    val url: String? = null,
    val data: DataContainer? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataContainer(
    // 키 이름이 "default"라 @JsonProperty로 안전 매핑
    @JsonProperty("default")
    val defaultBlock: DefaultBlock? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DefaultBlock(
    val page: PageInfo? = null,
    val total: Int? = null,
    // list/rows/content 형태 어느 쪽이 와도 받도록 방어
    @JsonAlias("list", "rows", "content")
    val list: List<PbancItem> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PageInfo(
    val totalPages: Int? = null,
    val totalElements: Int? = null,
    val last: Boolean? = null,
    val size: Int? = null,
    val number: Int? = null,
    val first: Boolean? = null,
    val numberOfElements: Int? = null,
    val empty: Boolean? = null
    // 필요하면 pageable/sort도 하위 DTO로 추가 가능
)

/**
 * 리스트 아이템 원본(필요 가능성이 있는 필드만 추림)
 * 실제 JSON에 훨씬 더 많은 필드가 있으므로 unknown 무시는 필수
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PbancItem(
    val pbancSn: Long? = null, // 공고 식별자(일부 B구분엔 null)
    val pbancId: String? = null, // 유관기관 쪽 식별자
    val pbancGubun: String? = null, // A:공단, B:유관기관
    val pbancNm: String? = null, // 공고명
    val rcrtTypeCdNm: String? = null, // 대상 유형 명(소상공인, 중소기업 등)
    val aplyPd: String? = null, // 접수기간(문자열 구간형: "YYYY-MM-DD ~ YYYY-MM-DD" 또는 "예산 소진시까지")
    val bizType: String? = null, // 공단지원사업/유관기관지원사업
    val regionNm: String? = null, // 지역(대부분 null)
    val departNm: String? = null, // 주관/부서
    val regDt: String? = null, // 등록일(예: 2025-10-30T09:55:57)
    val aplyPsbltySe: String? = null, // 신청가능 등 상태
    val hstgNm: String? = null // 해시태그(유관기관 일부에 존재)
)

/* -------------------- 요약 DTO (서비스/프론트 사용권장) -------------------- */

@JsonIgnoreProperties(ignoreUnknown = true)
data class CombinePbancListResponse(
    val page: SimplePage? = null,
    val total: Int? = null,
    val rows: List<PbancSummary> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimplePage(
    val totalPages: Int? = null,
    val totalElements: Int? = null,
    val pageSize: Int? = null,
    val pageNumber: Int? = null,
    val first: Boolean? = null,
    val last: Boolean? = null,
    val numberOfElements: Int? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PbancSummary(
    val id: String? = null, // pbancSn 있으면 그 값, 없으면 pbancId
    val gubun: String? = null, // A/B
    val title: String? = null,
    val type: String? = null, // rcrtTypeCdNm
    val department: String? = null, // departNm
    val region: String? = null, // regionNm
    val applyPeriod: String? = null, // 원문 구간 문자열 전체(표시용)
    val applyStart: String? = null, // 구간 파싱해 앞쪽(없으면 null)
    val applyEnd: String? = null, // 구간 파싱해 뒤쪽(없으면 null)
    val bizType: String? = null, // 공단지원사업/유관기관지원사업
    val status: String? = null, // aplyPsbltySe
    val registeredAt: String? = null // regDt
)

/* -------------------- 매핑 유틸 -------------------- */

fun DefaultBlock.toCombinePbancListResponse(): CombinePbancListResponse {
    val p = this.page
    val simple = SimplePage(
        totalPages = p?.totalPages,
        totalElements = p?.totalElements,
        pageSize = p?.size,
        pageNumber = p?.number,
        first = p?.first,
        last = p?.last,
        numberOfElements = p?.numberOfElements
    )

    val rows = this.list.map { it.toSummary() }
    return CombinePbancListResponse(page = simple, total = this.total, rows = rows)
}

private fun PbancItem.toSummary(): PbancSummary {
    // aplyPd가 "YYYY-MM-DD ~ YYYY-MM-DD" 형태면 양쪽으로 분리
    val (start, end) = run {
        val raw = aplyPd?.trim()
        if (raw.isNullOrBlank()) {
            null to null
        } else if (raw.contains("~")) {
            val parts = raw.split("~")
            parts.getOrNull(0)?.trim() to parts.getOrNull(1)?.trim()
        } else {
            null to null
        }
    }

    return PbancSummary(
        id = pbancSn?.toString() ?: pbancId,
        gubun = pbancGubun,
        title = pbancNm,
        type = rcrtTypeCdNm,
        department = departNm,
        region = regionNm,
        applyPeriod = aplyPd,
        applyStart = start,
        applyEnd = end,
        bizType = bizType,
        status = aplyPsbltySe,
        registeredAt = regDt
    )
}
