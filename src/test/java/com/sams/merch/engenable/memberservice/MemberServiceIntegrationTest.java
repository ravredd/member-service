package com.sams.merch.engenable.memberservice;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import com.sams.merch.engenable.memberservice.web.ApiStaticExamples;
import com.sams.testing.utils.contract.OpenApiExchangeResultConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import net.javacrumbs.jsonunit.core.Option;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureWebTestClient(timeout = "30000") //This is for debugging purposes.  Since it is non blocking it defaults to waiting 5secs. 
public class MemberServiceIntegrationTest extends WithDBAndMockServerContainersIT {

    @Autowired
    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

    @Value("${open-api.filename}")
    private String openApiContractFileName;
    
    private OpenApiExchangeResultConsumer openApiExchangeResultConsumer;

    @BeforeEach
    public void beforeEach(){
        this.openApiExchangeResultConsumer = new OpenApiExchangeResultConsumer(openApiContractFileName);
    }

    @Test
    @DisplayName("Test GET /members")
    void testGetAllMembers() throws Exception {

        String expected = ApiStaticExamples.MEMBER_ARRAY_EXAMPLE;

        String body = webTestClient.get().uri("/members").exchange()
                                        .expectStatus().isOk()
                                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                                        .expectHeader().valueMatches(HttpHeaders.LOCATION, "(http://|https://|www\\.)([^ '\"]*)(/members$)")
                                        .expectBody(String.class)
                                        .consumeWith(openApiExchangeResultConsumer)//Validates the open api contract for request and response.
                                        .returnResult()
                                        .getResponseBody();

        assertThatJson(body)
        .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_ARRAY_ITEMS)
        .isArray()
        .isEqualTo(expected);
    }


    @Test
    @DisplayName("Test GET /members via id")
    void testGetMemberByUpcNbr() throws Exception {

        String expected = ApiStaticExamples.MEMBER_EXAMPLE1;

        String body = webTestClient.get().uri("/members/"+ApiStaticExamples.MEMBER_EXAMPLE1_ID).exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectHeader().valueMatches(HttpHeaders.LOCATION, "(http://|https://|www\\.)([^ '\"]*)(/members/"+ApiStaticExamples.MEMBER_EXAMPLE1_ID+")")
                .expectBody(String.class)
                .consumeWith(openApiExchangeResultConsumer)// Validates the open api contract for request and response.
                .returnResult().getResponseBody();

        assertThatJson(body).isEqualTo(expected);

    }

    @Test
    @DisplayName("Test POST and DELETE /members")
    void testCreateAndDeleteOrder() throws Exception {
        //Tests POST create, DELETE and GET when member doesnt exist so 404
        //This way we dont mess with test data

        String requestJson = "{\"firstName\":\"Test\",\"lastName\":\"Dummy\",\"email\":\"tdummy@gmail.com\"}";
        String expected = "{\"id\":\"${json-unit.ignore-element}\", \"firstName\":\"Test\",\"lastName\":\"Dummy\",\"email\":\"tdummy@gmail.com\"}";
        
        //Execute POST request
        EntityExchangeResult<String> result = webTestClient.post().uri("/members")
                                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                                    .bodyValue(requestJson).exchange()
                                                    .expectStatus().isCreated()
                                                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                                                    .expectHeader().valueMatches(HttpHeaders.LOCATION, "(http://|https://|www\\.)([^ '\"]*)(/members/\\d+$)")
                                                    .expectBody(String.class)
                                                    .consumeWith(openApiExchangeResultConsumer)// Validates the open api contract for request and response.
                                                    .returnResult();
        
        assertThatJson(result.getResponseBody()).isEqualTo(expected);
        String location = result.getResponseHeaders().getLocation().toString();//Grab the location to use in GET

        //Now lets verify it was stored in db
        //execute GET request
        result = webTestClient.get().uri(location).exchange()
                            .expectStatus().isOk()
                            .expectHeader().contentType(MediaType.APPLICATION_JSON)
                            .expectHeader().valueEquals(HttpHeaders.LOCATION, location)
                            .expectBody(String.class)
                            .consumeWith(openApiExchangeResultConsumer)// Validates the open api contract for request and response.
                            .returnResult();
        assertThatJson(result.getResponseBody()).isEqualTo(expected);//Still should equal the one we posted
        
        // Now lets delete what we created
        webTestClient.delete().uri(location)
                    .exchange()
                    .expectStatus()
                    .is2xxSuccessful()
                    .expectBody(String.class)
                    .consumeWith(openApiExchangeResultConsumer);// Validates the open api contract for request and response.

        // Now verifiy it was deleted
        webTestClient.get().uri(location)
                    .exchange()
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .consumeWith(openApiExchangeResultConsumer);// Validates the open api contract
                                                                                     
    }
}
