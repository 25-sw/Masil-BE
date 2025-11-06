package sw2025.masil.domain.poster.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import sw2025.masil.domain.poster.persistence.entity.Poster
import sw2025.masil.domain.poster.persistence.repository.PosterRepository
import sw2025.masil.domain.poster.presentation.dto.req.CreatePosterRequest
import sw2025.masil.domain.poster.presentation.dto.res.CreatePosterResponse
import sw2025.masil.domain.storeowner.facade.StoreOwnerFacade
import sw2025.masil.infra.feign.ai.gemini.client.GeminiClient
import sw2025.masil.infra.feign.ai.gemini.dto.req.GeminiPromptRequestDto
import sw2025.masil.infra.feign.ai.gemini.properties.GeminiProperties
import sw2025.masil.infra.feign.ai.imagen.client.ImagenClient
import sw2025.masil.infra.feign.ai.imagen.dto.req.ImagenPromptRequest
import sw2025.masil.infra.feign.ai.imagen.properties.ImagenProperties
import sw2025.masil.infra.s3.util.S3Util
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.util.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.auth.oauth2.GoogleCredentials

@Service
class CreatePosterService(
    private val geminiClient: GeminiClient,
    private val imagenClient: ImagenClient,
    private val s3Util: S3Util,
    private val storeOwnerFacade: StoreOwnerFacade,
    private val geminiProperties: GeminiProperties,
    private val imagenProperties: ImagenProperties,
    private val posterRepository: PosterRepository
) {
    private val mapper: ObjectMapper = jacksonObjectMapper()
    private val log = LoggerFactory.getLogger(CreatePosterService::class.java)

    fun execute(request: CreatePosterRequest): CreatePosterResponse {
        log.info(
            "[Poster] generate start: storeName={}, serviceName={}, category={}, targetAudience={}",
            request.storeName, request.serviceName, request.category, request.targetAudience
        )
        try {
            val promptReq = GeminiPromptRequestDto(
                contents = listOf(
                    GeminiPromptRequestDto.Content(
                        role = "user",
                        parts = listOf(
                            GeminiPromptRequestDto.Part(
                                text = """
                                    가게 이름: ${request.storeName}
                                    홍보 서비스: ${request.serviceName}
                                    포스터 목적: ${request.category}
                                    대상 고객: ${request.targetAudience}
                                    위 정보를 바탕으로 포스터 홍보 문구를 만들어줘.
                                    첫 줄은 제목(짧고 강렬하게), 다음 줄은 부제(한 줄). 그 외 설명은 넣지 마.
                                """.trimIndent()
                            )
                        )
                    )
                )
            )

            log.debug("[Poster] text prompt req: {}", mapper.writeValueAsString(promptReq))

            val promptRes = geminiClient.generateText(
                model = geminiProperties.model,
                apiKey = geminiProperties.apiKey,
                body = promptReq
            )

            log.debug("[Poster] text prompt res candidates={}", promptRes.candidates?.size ?: 0)

            val combined: String = promptRes.candidates
                .firstOrNull()?.content?.parts
                ?.firstOrNull()?.text
                ?.trim()
                ?.replace("**", "")
                ?: "AI 포스터\n설명을 입력해 주세요"

            val (title, subtitle) = combined
                .split("\n", limit = 2)
                .let { it.getOrElse(0) { "AI 포스터" } to it.getOrElse(1) { "" } }
            val bodyLines: List<String> = generateBodyCopy(request, title, subtitle)

            log.info("[Poster] imagen generate start")
            val imagePrompt = buildImagenPrompt(request, title, subtitle, bodyLines)
            log.debug("[Poster] imagen prompt=\n{}", imagePrompt)
            val googleCreds = GoogleCredentials.getApplicationDefault()
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
            googleCreds.refreshIfExpired()
            val bearerToken = "Bearer ${googleCreds.accessToken.tokenValue}"
            log.debug(
                "[Poster] obtained Google access token via ADC; expiresAt={}",
                googleCreds.accessToken.expirationTime
            )
            val imageResponse = imagenClient.generateImage(
                projectId = imagenProperties.projectId,
                location = imagenProperties.location,
                accessToken = bearerToken,
                body = ImagenPromptRequest(
                    instances = listOf(ImagenPromptRequest.Instance(prompt = imagePrompt)),
                    parameters = ImagenPromptRequest.Parameters(
                        sampleCount = 1,
                        aspectRatio = "3:4",
                        negativePrompt = "watermark, caption watermark, brand logo overlays, UI, hands, fingers, people, body parts, artifacts, lowres, blurry, jpeg artifacts, frame, border, heavy grain, distortion",
                        safetySetting = "block_some"
                    )
                )
            )
            val imageBase64: String = imageResponse.predictions
                ?.firstOrNull()
                ?.bytesBase64Encoded
                ?: throw IllegalStateException("Imagen did not return predictions[0].bytesBase64Encoded")

            log.debug(
                "[Poster] imagen base64 length={}, head={}...",
                imageBase64.length,
                imageBase64.take(32)
            )

            val imageBytes = try {
                java.util.Base64.getDecoder().decode(imageBase64)
            } catch (e: IllegalArgumentException) {
                log.error("[Poster] base64 decode failed: {}", e.message)
                throw IllegalStateException("Invalid base64 from Imagen", e)
            }

            val composedPng: ByteArray = composePoster(imageBytes, title, subtitle)
            val inputStream = ByteArrayInputStream(composedPng)
            val imageUrl = s3Util.uploadStream("poster_${UUID.randomUUID()}.png", inputStream, "image/png")
            log.info("[Poster] S3 uploaded: url={}", imageUrl)

            savePoster(imageUrl)

            return CreatePosterResponse(
                title = title,
                subtitle = subtitle,
                url = imageUrl
            )
        } catch (e: feign.FeignException) {
            when (e.status()) {
                401 -> {
                    log.error(
                        "[Poster] 401 Unauthorized from Vertex AI Imagen. Check ADC and scopes. Hints: set GOOGLE_APPLICATION_CREDENTIALS to a service account JSON with roles/aiplatform.user; ensure projectId='{}' is correct; location='{}'; and billing is enabled.",
                        imagenProperties.projectId,
                        imagenProperties.location
                    )
                }

                403 -> {
                    log.error(
                        "[Poster] 403 Forbidden from Vertex AI Imagen. Hints: service account lacks roles/aiplatform.user or Vertex AI API not enabled on project '{}'.",
                        imagenProperties.projectId
                    )
                }
            }
            throw e
        } catch (e: Exception) {
            log.error("[Poster] generation failed: {}", e.message, e)
            throw e
        }
    }

    @Transactional
    fun savePoster(imageUrl: String) {
        val currentOwner = storeOwnerFacade.currentStoreOwner()
        val poster = Poster(
            fileName = imageUrl,
            storeOwner = currentOwner
        )
        posterRepository.save(poster)
    }

    private fun generateBodyCopy(request: CreatePosterRequest, title: String, subtitle: String): List<String> {
        val promptReq = GeminiPromptRequestDto(
            contents = listOf(
                GeminiPromptRequestDto.Content(
                    role = "user",
                    parts = listOf(
                        GeminiPromptRequestDto.Part(
                            text = """
                                가게 이름: ${request.storeName}
                                홍보 서비스: ${request.serviceName}
                                포스터 목적: ${request.category}
                                대상 고객: ${request.targetAudience}

                                위 정보를 바탕으로, 메인 제목과 부제 외에 이미지 안에 작은 안내 문구 2줄을 만들어줘.
                                - 한국어 14~24자 내외로, 이모지/특수문자/해시태그 금지
                                - 예: 혜택, 기간, 위치/구매유도성 문구 등
                                - 줄바꿈으로 구분된 2줄만 반환하고 다른 설명은 넣지 마.
                            """.trimIndent()
                        )
                    )
                )
            )
        )

        return try {
            val res = geminiClient.generateText(
                model = geminiProperties.model,
                apiKey = geminiProperties.apiKey,
                body = promptReq
            )
            val raw = res.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.replace("**", "")?.trim()
            raw
                ?.lines()
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.take(2)
                ?.ifEmpty { listOf("지금만 한정 판매", "신선한 딸기 가득") }
                ?: listOf("지금만 한정 판매", "신선한 딸기 가득")
        } catch (_: Exception) {
            listOf("지금만 한정 판매", "신선한 딸기 가득")
        }
    }

    private fun buildImagenPrompt(request: CreatePosterRequest, title: String, subtitle: String, bodyLines: List<String>): String {
        val theme = request.serviceName.ifBlank { "Signature item" }
        val store = request.storeName.ifBlank { "The store" }
        val category = request.category.ifBlank { "Cafe / Dessert" }
        val target = request.targetAudience.ifBlank { "young adults" }
        return """
Create a photorealistic commercial product PHOTO for an Instagram feed portrait (vertical). Language context: Korean.

Brand/store: $store
Category: $category
Product/theme: $theme (iced strawberry latte; no visible steam; fresh strawberries)
Target audience: $target

Visual direction:
- Style: premium, modern, clean; real product photography look (not illustration). Soft studio lighting, subtle bokeh, crisp details.
- Composition: portrait layout with strong central hero (glass cup of strawberry latte). Clean background; enough breathing room around the subject.
- Palette: fresh strawberry reds, milk white, light cool neutrals; avoid warm vapor/steam.
- Props: realistic strawberries (whole & sliced), minimal marble or pastel surface. Avoid hands/people.

Typography to render INSIDE the image (Korean):
- Render a SMALL caption block with these exact lines (do NOT translate or rewrite):
  • ${bodyLines.getOrNull(0) ?: "지금만 한정 판매"}
  • ${bodyLines.getOrNull(1) ?: "신선한 딸기 가득"}
- Place this caption at the lower-left area inside a subtle semi-transparent rounded rectangle. Keep high legibility.
- Use modest size (caption-level), do not create a large headline. Avoid extra text other than the two lines above.

Hard constraints:
- Do NOT add logos, watermarks, UI, frames, borders, or any extra text besides the two caption lines above.
- No people or hands. No heavy grain or distortions.
Output: a single high-resolution portrait image suitable for overlaying an external main title/subtitle later.
""".trimIndent()
    }

    private fun extractBase64FromText(raw: String): String? {
        val noFences = raw
            .replace(Regex("```[a-zA-Z]*\\s*"), "")
            .replace(Regex("```\\s*"), "")

        val jsonKey =
            Regex("\"image_base64\"\\s*:\\s*\"([^\"]+)\"", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
        jsonKey.find(noFences)?.let { return it.groupValues[1] }

        val dataUrl = Regex("data:image/(png|jpeg|jpg);base64,([A-Za-z0-9+/=_-]+)", RegexOption.IGNORE_CASE)
        dataUrl.find(noFences)?.let { return it.groupValues[2] }

        val loneB64 = Regex("([A-Za-z0-9+/=_-]{120,})", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
        loneB64.find(noFences)?.let { return it.groupValues[1] }

        return null
    }

    private fun stripCodeFence(src: String): String {
        var s = src.trim()
        if (s.startsWith("```")) {
            val firstNewline = s.indexOf('\n')
            if (firstNewline != -1) s = s.substring(firstNewline + 1)
            if (s.endsWith("```")) s = s.removeSuffix("```").trim()
        }
        if (s.startsWith("&lt;")) {
            throw IllegalStateException("HTML-like response returned instead of image data.")
        }
        return s.trim()
    }

    private fun extractFirstBase64Block(s: String): String? {
        val regex = Regex("""[A-Za-z0-9+/=]{400,}""")
        return regex.find(s)?.value
    }

    private fun decodeBase64Strict(b64: String): ByteArray {
        var s = b64.trim()

        s = s.replace("\\/", "/")
            .replace("\\n", "")
            .replace("\\r", "")
            .replace("\\t", "")
            .replace("\\\"", "\"")

        s = s.replace(Regex("[^A-Za-z0-9+/=_-]"), "")

        s = s.replace('-', '+').replace('_', '/')

        val pad = (4 - (s.length % 4)) % 4
        if (pad in 1..3) s += "=".repeat(pad)

        if (s.length < 64) throw IllegalStateException("Base64 too short")

        return try {
            java.util.Base64.getDecoder().decode(s)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Invalid base64 data length or characters.", e)
        }
    }

    private fun padBase64(s: String): String {
        val rem = s.length % 4
        return if (rem == 0) s else s + "=".repeat(4 - rem)
    }

    private fun composePoster(baseImageBytes: ByteArray, title: String, subtitle: String): ByteArray {
        val src = javax.imageio.ImageIO.read(ByteArrayInputStream(baseImageBytes))
            ?: throw IllegalStateException("Failed to read image bytes")
        val cropped = centerCropToAspect(src, 4.0, 5.0)
        val g = cropped.createGraphics()
        try {
            g.setRenderingHint(
                java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            )
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY)
            g.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL, java.awt.RenderingHints.VALUE_STROKE_NORMALIZE)
            g.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            val w = cropped.width
            val h = cropped.height
            val margin = (w * 0.06).toInt()
            val titleSize = (w * 0.095).toInt()
            val subSize = (w * 0.05).toInt()
            val titleFont = chooseFont(
                listOf(
                    "Noto Sans CJK KR",
                    "NanumGothic",
                    "Apple SD Gothic Neo",
                    "Malgun Gothic",
                    "Pretendard",
                    "NanumSquare",
                    "SansSerif"
                ), java.awt.Font.BOLD, titleSize
            )
            val subFont = chooseFont(
                listOf(
                    "Noto Sans CJK KR",
                    "NanumGothic",
                    "Apple SD Gothic Neo",
                    "Malgun Gothic",
                    "Pretendard",
                    "NanumSquare",
                    "SansSerif"
                ), java.awt.Font.PLAIN, subSize
            )
            g.font = titleFont
            val fmTitle = g.fontMetrics
            val titleWidth = fmTitle.stringWidth(title)
            val titleHeight = fmTitle.height
            g.font = subFont
            val fmSub = g.fontMetrics
            val subWidth = fmSub.stringWidth(subtitle)
            val subHeight = fmSub.height
            val blockWidth = maxOf(titleWidth, subWidth) + margin
            val blockHeight =
                titleHeight + (if (subtitle.isNotBlank()) subHeight + (margin * 0.25).toInt() else 0) + margin / 2
            val blockX = margin
            val blockY = margin
            val bg = java.awt.Color(17, 17, 17, (255 * 0.35).toInt())
            g.color = bg
            drawRoundRectFilled(g, blockX, blockY, blockWidth, blockHeight, (margin * 0.3).toInt())
            g.font = titleFont
            val titleX = blockX + (margin * 0.3).toInt()
            val titleY = blockY + fmTitle.ascent + (margin * 0.2).toInt()
            drawTextWithShadow(g, title, titleX, titleY, java.awt.Color.WHITE)
            if (subtitle.isNotBlank()) {
                g.font = subFont
                val subX = titleX
                val subY = titleY + (titleHeight * 1.1).toInt()
                drawTextWithShadow(g, subtitle, subX, subY, java.awt.Color.WHITE)
            }
        } finally {
            g.dispose()
        }
        val baos = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(cropped, "png", baos)
        return baos.toByteArray()
    }

    private fun drawRoundRectFilled(g: java.awt.Graphics2D, x: Int, y: Int, w: Int, h: Int, r: Int) {
        val old = g.renderingHints[java.awt.RenderingHints.KEY_ANTIALIASING]
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
        g.fillRoundRect(x, y, w, h, r, r)
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, old)
    }

    private fun drawTextWithShadow(g: java.awt.Graphics2D, text: String, x: Int, y: Int, color: java.awt.Color) {
        g.color = java.awt.Color(0, 0, 0, (255 * 0.35).toInt())
        g.drawString(text, x + 2, y + 2)
        g.color = color
        g.drawString(text, x, y)
    }

    private fun chooseFont(names: List<String>, style: Int, size: Int): java.awt.Font {
        for (n in names) {
            try {
                val f = java.awt.Font(n, style, size)
                if (f.family != "Dialog") return f
            } catch (_: Exception) {
            }
        }
        return java.awt.Font("SansSerif", style, size)
    }

    private fun centerCropToAspect(
        src: java.awt.image.BufferedImage,
        targetWratio: Double,
        targetHratio: Double
    ): java.awt.image.BufferedImage {
        val targetAspect = targetWratio / targetHratio
        val srcAspect = src.width.toDouble() / src.height.toDouble()
        var cropX = 0
        var cropY = 0
        var cropW = src.width
        var cropH = src.height
        if (srcAspect > targetAspect) {
            cropW = (src.height * targetAspect).toInt()
            cropX = (src.width - cropW) / 2
        } else if (srcAspect < targetAspect) {
            cropH = (src.width / targetAspect).toInt()
            cropY = (src.height - cropH) / 2
        }
        return src.getSubimage(cropX, cropY, cropW, cropH)
    }
}