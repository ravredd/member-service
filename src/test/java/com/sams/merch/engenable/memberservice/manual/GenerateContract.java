package com.sams.merch.engenable.memberservice.manual;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sams.merch.engenable.memberservice.WithDBAndMockServerContainersIT;
import com.sams.merch.engenable.memberservice.web.ApiStaticExamples;

import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.mock.OpenAPIExpectation.openAPIExpectation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
public class GenerateContract extends WithDBAndMockServerContainersIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Value("${springdoc.staticdocs.dir}")
    private String documentPath;

    @Value("${open-api.generate.path}")
    private String oas3Path;

    @Value("${open-api.filename}")
    private String openApiFileName;

    @Value("${mock-server.expecations.filename:mock_server_expectations.json}")
    private String mockServerExpectationsFileName;

    // MockServer in order to generate expectations.json file that can be used by
    // other services.
    @Container
    public static MockServerContainer MOCK_SERVER = new MockServerContainer(
            DockerImageName.parse("hub.docker.prod.walmart.com/jamesdbloom/mockserver:mockserver-5.11.1")
                    .asCompatibleSubstituteFor("jamesdbloom/mockserver"))
                            .withEnv("MOCKSERVER_LIVENESS_HTTP_GET_PATH", "/health")
                            .waitingFor(Wait.forHttp("/health").forStatusCode(200));

    @Test
    public void generateOpenApiContract() throws Exception {
        ResponseEntity<String> contractResponseEntity = retrieveOpenApiContract();
        String absoluteApiDocumentPath = getStaticFilesAbsolutePath();
        writeStringToFile(contractResponseEntity.getBody(), absoluteApiDocumentPath, openApiFileName, StandardCharsets.UTF_8);
    }

    @Test
    public void generateMockServerExpectationsFile() throws Exception {

        
        // lets add expectations to MockServer will use Examples so they map to mock data
        MockServerClient mockServerClient = new MockServerClient(MOCK_SERVER.getHost(), MOCK_SERVER.getServerPort());
        loadGetJsonExpectation(mockServerClient, "/members/" + ApiStaticExamples.MEMBER_EXAMPLE1_ID, ApiStaticExamples.MEMBER_EXAMPLE1);
        loadGetJsonExpectation(mockServerClient, "/members/" + ApiStaticExamples.MEMBER_EXAMPLE2_ID, ApiStaticExamples.MEMBER_EXAMPLE2);
        loadGetJsonExpectation(mockServerClient, "/members/" + ApiStaticExamples.MEMBER_EXAMPLE3_ID, ApiStaticExamples.MEMBER_EXAMPLE3);
        loadGetJsonExpectation(mockServerClient, "/members/999", "Member with such id doesn't exists.", 404, MediaType.TEXT_PLAIN);

        //Now add the rest of the openApi contract to mockServer
        ResponseEntity<String> contractResponseEntity = retrieveOpenApiContract();
        mockServerClient.upsert(openAPIExpectation(contractResponseEntity.getBody()));

        // Export the expectations from mock server to export to file.  So others can import and use
        ResponseEntity<String> expectations = this.testRestTemplate.exchange("http://" + MOCK_SERVER.getHost() + ":"
                + MOCK_SERVER.getServerPort() + "/mockserver/retrieve?type=active_expectations", HttpMethod.PUT, null, String.class);

        String absoluteApiDocumentPath = getStaticFilesAbsolutePath();
        writeStringToFile(expectations.getBody(), absoluteApiDocumentPath, mockServerExpectationsFileName, StandardCharsets.UTF_8);
    }

    private String getStaticFilesAbsolutePath() {
        String absoluteApiDocumentPath = new File(documentPath).getAbsolutePath();
        return absoluteApiDocumentPath;
    }

    private ResponseEntity<String> retrieveOpenApiContract() {
        ResponseEntity<String> contractResponseEntity = this.testRestTemplate
                .exchange("http://localhost:" + this.port + this.oas3Path, HttpMethod.GET, null, String.class);
        return contractResponseEntity;
    }

    private void writeStringToFile(String stringToWrite, String absolutePathString, String outputFileName, Charset charset) throws IOException{
        Path outputExpectationsFile = Paths.get(absolutePathString, outputFileName);
        try (BufferedWriter writer = Files.newBufferedWriter(outputExpectationsFile, charset)) {
            writer.write(stringToWrite);
        }
    }

    private void loadGetJsonExpectation(MockServerClient mockServerClient, String path, String body) {
        loadGetJsonExpectation(mockServerClient, path, body, 200, MediaType.APPLICATION_JSON);
    }

    private void loadGetJsonExpectation(MockServerClient mockServerClient, String path, String body, int responseCode,
            MediaType responseMediaType) {
        mockServerClient.when(request().withMethod("GET").withPath(path))
                .respond(response().withStatusCode(responseCode).withContentType(responseMediaType).withBody(body));
    }

}
