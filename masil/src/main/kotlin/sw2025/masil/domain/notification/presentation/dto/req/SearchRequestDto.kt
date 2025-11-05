package sw2025.masil.domain.notification.presentation.dto.req

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SearchRequestDto(
    val sortModel: List<SortModel> = listOf(SortModel("pbancSn", "desc")),
    val search: SearchCriteria = SearchCriteria(),
    val paging: Boolean = true,
    val startRow: Int = 0,
    val endRow: Int = 10
)

data class SortModel(
    val colId: String,
    val sort: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SearchCriteria(
    val pbancNm: String? = "",
    val rcrtTypeCdList: List<String> = emptyList(),
    val rcrtTypeCdNmList: List<String> = emptyList(),
    val rcrtTypeCdNmListDisplay: String? = "",
    val regionNmList: List<String> = emptyList(),
    val regionNmListDisplay: String? = "",
    val departNmList: List<String> = emptyList(),
    val departNmListDisplay: String? = "",
    val tpbizCdList: List<String> = emptyList(),
    val tpbizCdListDisplay: String? = "",
    val bhis: Range? = Range(),
    val wrkr: Range? = Range(),
    val sls: Range? = Range(),
    val aplySeYn: String? = "N",
    val sbrPbancYn: String? = "N",
    val itrstPbancYn: String? = "N",
    val searchBox: String? = null,
    val regionCdList: List<String> = emptyList()
)

data class Range(
    val from: String? = null,
    val to: String? = null
)
