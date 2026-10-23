package com.fptaptech.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiArtifactTest {

    private static final String[] GROUPS = {"public-v1", "public-v2", "admin", "partner"};

    @Autowired
    MockMvc mvc;

    @Test
    void committedSpecsMatchTheGeneratedContract() throws Exception {
        boolean update = Boolean.getBoolean("update.openapi.specs");
        Path dir = Path.of("docs/api");
        Files.createDirectories(dir);
        for (String group : GROUPS) {
            String yaml = mvc.perform(get("/v3/api-docs.yaml/" + group))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            Path file = dir.resolve("openapi-" + group + ".yaml");
            if (update) {
                Files.writeString(file, yaml.endsWith("\n") ? yaml : yaml + "\n");
            }
            assertThat(Files.exists(file))
                    .as("Thiếu %s. Chạy ./mvnw test -Dupdate.openapi.specs=true rồi commit lại file.", file)
                    .isTrue();
            Yaml parser = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, Object> expected = parser.load(Files.readString(file));
            Map<String, Object> actual = parser.load(yaml);
            assertThat(actual)
                    .as("Spec %s lệch với docs/api. Cập nhật bằng -Dupdate.openapi.specs=true và commit.", group)
                    .isEqualTo(expected);
        }
    }
}
