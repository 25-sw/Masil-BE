package sw2025.masil.global.config

import feign.RequestInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SbizFeignConfig {

    @Bean
    fun sbizHeaderInterceptor(
        @Value("\${sbiz.cookies}") cookies: String
    ): RequestInterceptor = RequestInterceptor {
        it.header("Accept", "application/json")
        it.header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
        it.header("Connection", "keep-alive")
        it.header("Content-Type", "application/json;charset=UTF-8")
        it.header("Origin", "https://www.sbiz24.kr")
        it.header("Origin-Method", "GET")
        it.header("Referer", "https://www.sbiz24.kr/")
        it.header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36")
        it.header("Sec-Fetch-Dest", "empty")
        it.header("Sec-Fetch-Mode", "cors")
        it.header("Sec-Fetch-Site", "same-origin")
        it.header("sec-ch-ua", "\"Google Chrome\";v=\"141\", \"Not?A_Brand\";v=\"8\", \"Chromium\";v=\"141\"")
        it.header("sec-ch-ua-mobile", "?0")
        it.header("sec-ch-ua-platform", "\"macOS\"")
        it.header("Cookie", cookies)
    }
}
