package com.jfc.owl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Simple application test
 * 簡單的應用程式測試
 */
@SpringBootTest
@ActiveProfiles("test")
class SimpleOwlApplicationTests {

    @Test
    void contextLoads() {
        // This test just verifies that the Spring context loads
        // 此測試僅驗證 Spring 上下文是否載入
    }
}