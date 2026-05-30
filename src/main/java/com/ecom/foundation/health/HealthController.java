package  com.ecom.foundation.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.time.Instant;

@RestController
public class HealthController {
    public static final Logger logger = LoggerFactory.getLogger(HealthController.class);   

    @GetMapping("/health")
    public Map<String, Object> health() {
        logger.info("Health check requested");
        return Map.of("status", "Service is running", "timestamp", Instant.now());
    }
}
