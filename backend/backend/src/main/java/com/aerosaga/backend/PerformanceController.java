

package com.aerosaga.backend;

import org.graalvm.polyglot.Context;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    @GetMapping("/benchmark")
    public Map<String, Object> benchmark() {
        Map<String, Object> results = new LinkedHashMap<>();

        // Warm up Python, then measure the operation.
        try (Context context = Context.newBuilder("python")
                .allowAllAccess(true)
                .build()) {

            context.eval("python", "1 + 1");

            long pythonStart = System.nanoTime();
            context.eval("python", "sum(range(1000))");
            long pythonElapsed = System.nanoTime() - pythonStart;

            results.put("pythonExecutionNanos", pythonElapsed);
            results.put("pythonExecutionMicros",
                    pythonElapsed / 1_000.0);
        }

        // Measure a simple in-process Java computation.
        long javaStart = System.nanoTime();

        long sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i;
        }

        long javaElapsed = System.nanoTime() - javaStart;

        results.put("javaBaselineNanos", javaElapsed);
        results.put("javaBaselineMicros",
                javaElapsed / 1_000.0);
        results.put("baselineType",
                "In-process Java computation, not an HTTP request");
        results.put("pythonOperation", "sum(range(1000))");
        results.put("javaResult", sum);

        return results;
    }
}
