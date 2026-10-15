package com.menglang.ticket.presentation.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    @DisplayName("Success response has success=true and formatted timestamp")
    void testSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("test-data", "Operation completed");

        assertTrue(response.isSuccess());
        assertEquals("Operation completed", response.getMessage());
        assertEquals("test-data", response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    @DisplayName("Error response has success=false and null data")
    void testErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error("Something went wrong");

        assertFalse(response.isSuccess());
        assertEquals("Something went wrong", response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }
}
