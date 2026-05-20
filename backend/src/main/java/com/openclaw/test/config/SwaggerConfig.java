package com.openclaw.test.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("OpenClaw Spring Test API")
                .description("Spring Boot 集成 OpenClaw Gateway 的测试接口\n\n" +
                    "功能包括：\n" +
                    "- 对话：发送消息给 Agent 并获取回复\n" +
                    "- 文本解析：上传 CSV/文本文件并让 Agent 分析\n" +
                    "- 文件生成：让 Agent 生成文件并下载\n" +
                    "- 文件下载：从 nginx 下载 Agent 生成的文件")
                .version("1.0.0")
                .contact(new Contact()
                    .name("OpenClaw Integration")
                    .url("http://localhost:8088/swagger-ui.html")))
            .servers(List.of(
                new Server().url("/").description("当前服务器")
            ));
    }
}
