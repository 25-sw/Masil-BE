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
    private val CREATIVE_MODE: Boolean = false  // set true to let the model interpret more freely

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
                                    제목과 부제라는 단어는 포함하지 말고, 첫 줄은 짧고 강렬한 문장, 두 번째 줄은 그 문장을 보완하는 한 줄만 반환해. 다른 설명은 넣지 마.
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
            val imagePrompt = if (CREATIVE_MODE)
                buildImagenPromptCreative(request, title, subtitle, bodyLines)
            else
                buildImagenPrompt(request, title, subtitle, bodyLines)
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
                        negativePrompt = if (CREATIVE_MODE) "" else buildNegativePrompt(request.serviceName),
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
                ?.ifEmpty { listOf("지금만 한정 판매", "${request.serviceName} 오늘의 추천") }
                ?: listOf("지금만 한정 판매", "${request.serviceName} 오늘의 추천")
        } catch (_: Exception) {
            listOf("지금만 한정 판매", "${request.serviceName} 오늘의 추천")
        }
    }

    private fun buildNegativePrompt(serviceName: String): String {
        val base = mutableListOf(
            "watermark",
            "caption watermark",
            "brand logo overlays",
            "UI",
            "hands",
            "fingers",
            "people",
            "body parts",
            "artifacts",
            "lowres",
            "blurry",
            "jpeg artifacts",
            "frame",
            "border",
            "heavy grain",
            "distortion",
            // generic text suppression
            "text",
            "caption",
            "subtitle",
            "korean letters",
            "english letters"
        )

        val s = serviceName.lowercase()
        fun addAll(vararg items: String) { base.addAll(items) }

        // If it's a fruit beverage (주스/에이드/스무디), block dairy & coffee cues
        if (s.contains("주스") || s.contains("에이드") || s.contains("스무디") || s.contains("juice") || s.contains("ade") || s.contains("smoothie")) {
            addAll(
                "coffee",
                "espresso",
                "latte",
                "cappuccino",
                "americano",
                "macchiato",
                "mocha",
                "mug",
                "paper cup",
                "milk froth",
                "milk foam",
                "latte art",
                "crema",
                "coffee beans",
                "portafilter",
                "steam wand",
                "ceramic mug",
                "takeout cup",
                "brown coffee color"
            )
            // and avoid dairy look if not explicitly hot/milk-based
            if (!s.contains("라떼") && !s.contains("latte") && !s.contains("milk") && !s.contains("hot") && !s.contains("따뜻")) {
                addAll(
                    "milk",
                    "milky",
                    "cream",
                    "whipped cream",
                    "ice cream",
                    "yogurt"
                )
            }
        }

        // Explicit ingredient grounding: if serviceName mentions watermelon, forbid other fruits that often creep in
        if (s.contains("수박") || s.contains("watermelon")) {
            addAll(
                "strawberry",
                "strawberries",
                "berry",
                "blueberry",
                "raspberry",
                "cherry",
                "peach",
                "mango",
                "banana",
                "kiwi",
                "grape",
                "orange",
                "lemon slice",
                "lime slice",
                "coffee"
            )
        }

        return base.joinToString(", ")
    }


    private fun buildImagenPrompt(
        request: CreatePosterRequest,
        title: String,
        subtitle: String,
        bodyLines: List<String>
    ): String {
        val s = request.serviceName.lowercase()
        val allowSteam = listOf("hot", "핫", "따뜻", "데운", "뜨거운").any { s.contains(it) }
        val steamPolicy = if (allowSteam)
            "Steam/vapor is allowed only if the product name explicitly indicates a hot item."
        else
            "Do NOT render steam or vapor; treat as cold/room-temperature."

        // 요청 JSON을 그대로 넣어 모델을 고정(ground)시킴
        val groundedJson = """
    {
      "storeName": "${request.storeName}",
      "serviceName": "${request.serviceName}",
      "category": "${request.category}",
      "targetAudience": "${request.targetAudience}"
    }
    """.trimIndent()

        val smallCopy = bodyLines
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(separator = "\n- ", prefix = "- ", postfix = "")

        return """
You are generating ONE photorealistic commercial PRODUCT PHOTO for Instagram (portrait/vertical).
Use ONLY the information below as ground truth. Do not infer other flavors or ingredients.

=== Grounding Data (JSON) ===
$groundedJson
=== End ===

Optional small-copy suggestions (for layout spacing only, DO NOT render as text):
$smallCopy

Rules:
- Depict exactly the product described by "serviceName".
- If ingredients are implied by words in "serviceName", use ONLY those literal ingredients.
- Do NOT add or substitute any other fruits/coffee/toppings not present in the name.
- If ambiguous, prefer a minimal neutral presentation appropriate to the item.
- $steamPolicy
- No text/captions/UI/logos/watermarks/frames/borders. (Typography will be added later.)

Style & Composition:
- Premium, modern, clean studio product photography (not illustration).
- Soft studio lighting, subtle bokeh, crisp details.
- Strong central hero subject; keep clean background and breathing room for later typography.
- Background/props must logically belong to the product in "serviceName"; avoid unrelated items.

Output: one high-resolution portrait image (no embedded text).
""".trimIndent()
    }

    private fun buildImagenPromptCreative(
        request: CreatePosterRequest,
        title: String,
        subtitle: String,
        bodyLines: List<String>
    ): String {
        val smallCopy = bodyLines
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(separator = "\n- ", prefix = "- ", postfix = "")

        // A lighter, interpretation-friendly prompt: minimal constraints, no grounding JSON, no explicit fruit/prop lists.
        return """
You are an art director creating ONE compelling vertical product shot suitable for a poster.
Interpret the brief creatively and choose plausible styling/props that fit naturally.

Brief:
- Store: ${request.storeName}
- Offering: ${request.serviceName}
- Category: ${request.category}
- Target audience: ${request.targetAudience}

Creative guidance:
- You may infer a reasonable scene, mood, and supporting props that match the offering and category.
- Aim for premium, modern studio aesthetics; photorealistic result (not illustration).
- Strong central hero; keep some breathing room for later typography.
- Keep color palette and props coherent with the offering and season implied by the name.

Optional small-copy hints (for spacing only, DO NOT render as text):
$smallCopy

Hard limits:
- Do not render UI, logos, watermarks, frames, or borders.
- Avoid embedding any readable text in the image (typography is added later).
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
            val subSize = (w * 0.045).toInt() // slightly smaller subtitle font
            // Shorten the title if too long
            val shortTitle = if (title.length > 20) title.take(20) + "…" else title
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
            val titleWidth = fmTitle.stringWidth(shortTitle)
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
            drawTextWithShadow(g, shortTitle, titleX, titleY, java.awt.Color.WHITE)
            if (subtitle.isNotBlank()) {
                g.font = subFont
                val subX = titleX
                // Place subtitle at the bottom margin, aligned left with title
                val subY = h - margin - fmSub.descent
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