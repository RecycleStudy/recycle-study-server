package com.recyclestudy.restdocs;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.documentationConfiguration;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(RestDocumentationExtension.class)
public abstract class APIBaseTest {

    protected static final String DEFAULT_REST_DOC_PATH = "{class_name}/{method_name}/";

    protected RequestSpecification spec;

    @LocalServerPort
    int port;

    @BeforeEach
    protected void setUpRestDocs(RestDocumentationContextProvider provider) {
        RestAssured.port = port;

        this.spec = new RequestSpecBuilder()
                .setPort(port)
                .addFilter(documentationConfiguration(provider)
                        .operationPreprocessors()
                        .withRequestDefaults(
                                modifyUris().scheme("http").host("localhost").port(8080),
                                prettyPrint()
                        )
                        .withResponseDefaults(prettyPrint())
                )
                .build();
    }
}
