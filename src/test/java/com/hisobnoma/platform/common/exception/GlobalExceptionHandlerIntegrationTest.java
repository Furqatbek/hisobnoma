package com.hisobnoma.platform.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GlobalExceptionHandlerIntegrationTest.TestControllerConfig.class)
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestControllerConfig {
        @Bean
        TestController testController() { return new TestController(); }
    }

    @RestController
    @RequestMapping("/test-exceptions")
    public static class TestController {
        @GetMapping("/not-found")
        public String throwNotFound() { throw new NotFoundException("User", 42L); }

        @GetMapping("/validation")
        public String throwValidation() {
            throw new ValidationException("email", "must not be blank");
        }

        @GetMapping("/business")
        public String throwBusiness() {
            throw new BusinessException("Insufficient funds", "BUSINESS_ERR");
        }

        @GetMapping("/duplicate")
        public String throwDuplicate() {
            throw new DuplicateResourceException("User", "username", "admin");
        }

        @GetMapping("/forbidden")
        public String throwForbidden() { throw new ForbiddenException(); }

        @GetMapping("/unauthorized")
        public String throwUnauthorized() { throw new UnauthorizedException("Authentication required"); }

        @GetMapping("/general")
        public String throwGeneral() { throw new RuntimeException("Internal detail"); }

        @PostMapping("/validate-body")
        public String validateBody(@Valid @RequestBody TestBody body) {
            return "ok";
        }
    }

    public static class TestBody {
        @NotBlank
        private String email;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @Test
    @WithMockUser
    void handleNotFoundException_returns404WithMessage() throws Exception {
        mockMvc.perform(get("/test-exceptions/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    void handleValidationException_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(get("/test-exceptions/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors[0].field").value("email"));
    }

    @Test
    @WithMockUser
    void handleBusinessException_returns400WithMessage() throws Exception {
        mockMvc.perform(get("/test-exceptions/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient funds"))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERR"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @WithMockUser
    void handleDuplicateResourceException_returns409WithMessage() throws Exception {
        mockMvc.perform(get("/test-exceptions/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    void handleForbiddenException_returns403() throws Exception {
        mockMvc.perform(get("/test-exceptions/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    void handleUnauthorizedException_returns401() throws Exception {
        mockMvc.perform(get("/test-exceptions/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    void handleGeneralException_returns500WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test-exceptions/general"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @WithMockUser
    void handleConstraintViolationException_returns400WithViolationDetails() throws Exception {
        mockMvc.perform(post("/test-exceptions/validate-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors[0].field").value("email"));
    }
}
