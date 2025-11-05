package sw2025.masil

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@ConfigurationPropertiesScan
@EnableFeignClients
@SpringBootApplication
class MasilApplication

fun main(args: Array<String>) {
    runApplication<MasilApplication>(*args)
}
