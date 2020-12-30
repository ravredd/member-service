package com.sams.testing.utils.contract;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;

import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.util.MimeType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A {@link Consumer} that consumes an {@link EntityExchangeResult} which
 * utilizes {@link OpenApiInteractionValidator} from Atlassian, to validate
 * requests and response of the EnitiyExchangeResult used by WebTestClient of
 * webflux.
 *
 * If you are not using WebTestClient you can look up other options on
 * Atlassian's site.
 * https://bitbucket.org/atlassian/swagger-request-validator/src/master/
 * 
 * Essentially this converts EntityExchangeResult into Atlassian's {@link Request} and {@link Response} 
 * then calls {@link OpenApiInteractionValidator} to validate them against given spec.  
 * 
 * Once you have created an instance use it with the cosumeWith method on the BodySpec of the WebtestClient
 * You can use thi sin a test like so:  
 *  
 *       OpenApiExchangeResultConsumer  openApiExchangeResultConsumer =  new OpenApiExchangeResultConsumer(openApiContractFileUrl);
 *       String body = webTestClient.get().uri("/items/17082877721").exchange()
 *               .expectStatus().isOk()
 *               .expectHeader().contentType(MediaType.APPLICATION_JSON)
 *               .expectBody(String.class)
 *               .consumeWith(openApiExchangeResultConsumer)// Validates the open api contract for request and response.
 *               .returnResult().getResponseBody();
 */
public class OpenApiExchangeResultConsumer implements Consumer<EntityExchangeResult<String>> {
    
    private final OpenApiInteractionValidator validator;

    /**
     * If you already have created your own OpenApiInteractionValidator then you can use it.
     * @param validator
     */
    public OpenApiExchangeResultConsumer(final OpenApiInteractionValidator validator){
        this.validator = validator;
    }

    /**
     * Creates an {@link OpenApiInteractionValidator} from the given openApi spec file. 
     * @param specUrlOrDefinition - location of openApi spec. either json or yaml  can be local or external url.
     */
    public OpenApiExchangeResultConsumer(final String specUrlOrDefinition) {
        this.validator = OpenApiInteractionValidator.createForSpecificationUrl(specUrlOrDefinition).build();
    }

    private void validateRequestAndResponse(EntityExchangeResult<String> result){
        final ValidationReport validationReport = validator.validate(requestFromResult(result), responseFromResult(result));

        if (validationReport.hasErrors()) {
            throw new OpenApiValidationException(validationReport);
        }
    }

    private static Request requestFromResult(EntityExchangeResult<String> result){
        final UriComponents uriComponents = UriComponentsBuilder.fromUri(result.getUrl()).build();
        
        Request.Method method = fromHttpMethod(result.getMethod());
        String path = uriComponents.getPath();

        MediaType contentType = result.getRequestHeaders().getContentType();

        SimpleRequest.Builder builder = new SimpleRequest.Builder(method, path);
        result.getRequestHeaders().forEach(builder::withHeader);
        uriComponents.getQueryParams().forEach(builder::withQueryParam);
        String bodyAsString = getRequestBodyAsString(result, contentType);
        builder.withBody(bodyAsString, getCharset(contentType));
        return builder.build();
    }

    private static Response responseFromResult(EntityExchangeResult<String> result) {

        SimpleResponse.Builder builder = new SimpleResponse.Builder(result.getRawStatusCode());
        result.getResponseHeaders().forEach(builder::withHeader);
        builder.withBody(result.getResponseBody(), getCharset(result.getResponseHeaders().getContentType()));
        return builder.build();
    }

    private static Request.Method fromHttpMethod(final HttpMethod method) {
    return Request.Method.valueOf(method.name());
    }

    private static String getContentTypeString(@Nullable final MediaType mediaType){
        return Optional.ofNullable(mediaType).map(MediaType::getType).orElse(MediaType.APPLICATION_JSON_VALUE);
    }

    private static Charset getCharset(@Nullable final MediaType mediaType) {
        return Optional.ofNullable(mediaType).map(MimeType::getCharset).orElse(StandardCharsets.ISO_8859_1);
    }

    private static String getRequestBodyAsString(EntityExchangeResult<String> result, MediaType contentType){
        return convertRequestBodyToString(getRequestBodyAsByteArray(result), contentType);
    }

    private static byte[] getRequestBodyAsByteArray(EntityExchangeResult<String> result){
        return Optional.ofNullable(result.getRequestBodyContent()).isPresent()? result.getRequestBodyContent() : new byte[0];
    }

    private static String convertRequestBodyToString(byte[] body, MediaType contentType){
        return body.length == 0 ? null : new String(body, getCharset(contentType));
    }

    /**
     * A {@link RestClientException} which indicates that the request or response
     * does not conform to the swagger spec
     *
     * @since 2.1
     */
    public static class OpenApiValidationException extends NestedRuntimeException {
        private static final long serialVersionUID = 5861255760450026998L;
        private final ValidationReport report;

        public OpenApiValidationException(final ValidationReport report) {
            super(JsonValidationReportFormat.getInstance().apply(report));
            this.report = report;
        }

        /**
         * @return The validation report that generating this exception
         */
        public ValidationReport getValidationReport() {
            return report;
        }
    }

    @Override
    public void accept(EntityExchangeResult<String> t) {
        validateRequestAndResponse(t);
    }

}
