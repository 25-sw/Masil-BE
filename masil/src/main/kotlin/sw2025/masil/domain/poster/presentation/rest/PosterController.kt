package sw2025.masil.domain.poster.presentation.rest

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sw2025.masil.domain.poster.presentation.dto.req.CreatePosterRequest
import sw2025.masil.domain.poster.presentation.dto.res.CreatePosterResponse
import sw2025.masil.domain.poster.presentation.dto.res.GetPosterByCurrentUserResponse
import sw2025.masil.domain.poster.service.CreatePosterService
import sw2025.masil.domain.poster.service.FindPostersByCurrentUserService

@RestController
@RequestMapping("/posters")
class PosterController(
    private val createPosterService: CreatePosterService,
    private val findPostersByCurrentUserService: FindPostersByCurrentUserService
) {
    @PostMapping
    fun create(@RequestBody request: CreatePosterRequest): ResponseEntity<CreatePosterResponse> {
        val response = createPosterService.execute(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getPosterList(): ResponseEntity<List<GetPosterByCurrentUserResponse>> {
        val responses = findPostersByCurrentUserService.execute()
        return ResponseEntity.ok(responses)
    }
}