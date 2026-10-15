package com.menglang.ticket.presentation.advice;

import com.menglang.ticket.domain.exception.InvalidTransitionException;
import com.menglang.ticket.domain.exception.ResourceNotFoundException;
import com.menglang.ticket.domain.exception.UnauthorizedClientException;
import com.menglang.ticket.presentation.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("ResourceNotFoundException should map to 404 NOT_FOUND")
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Ticket TCK-1 not found");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleResourceNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Ticket TCK-1 not found", response.getBody().getMessage());
    }

    @Test
    @DisplayName("InvalidTransitionException should map to 422 UNPROCESSABLE_ENTITY")
    void testHandleInvalidTransition() {
        InvalidTransitionException ex = new InvalidTransitionException("Cannot move from closed to in_progress");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleInvalidTransition(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Cannot move from closed to in_progress", response.getBody().getMessage());
    }

    @Test
    @DisplayName("UnauthorizedClientException should map to 401 UNAUTHORIZED")
    void testHandleUnauthorizedClient() {
        UnauthorizedClientException ex = new UnauthorizedClientException("Invalid client credentials");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUnauthorizedClient(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid client credentials", response.getBody().getMessage());
    }

    @Test
    @DisplayName("IllegalArgumentException should map to 400 BAD_REQUEST")
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad input parameter");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Bad input parameter", response.getBody().getMessage());
    }
}
