package com.befapress.service;

import com.befapress.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
@Slf4j
public class FaceVerificationService {

    @Value("${face.python-command:python}")
    private String pythonCommand;

    @Value("${face.script-dir:../scripts}")
    private String scriptDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Run the python script to extract a 128-dimensional face descriptor.
     *
     * @param imagePath Absolute path to the face image file
     * @return JSON string of the face descriptor array (e.g. "[0.1, -0.2, ...]")
     */
    public String extractFaceDescriptor(Path imagePath) {
        log.info("Extracting face descriptor for image: {}", imagePath);
        
        Path scriptBasePath = Paths.get(scriptDir).toAbsolutePath().normalize();
        Path scriptPath = scriptBasePath.resolve("extract_face_descriptor.py");
        
        if (!scriptPath.toFile().exists()) {
            log.error("Face descriptor script not found at: {}", scriptPath);
            throw new RuntimeException("Face verification script not found on the server.");
        }

        try {
            List<String> command = new ArrayList<>();
            command.add(pythonCommand);
            command.add(scriptPath.toString());
            command.add(imagePath.toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            // Set working directory to the scripts folder so the model files
            // (nn4.small2.v1.t7, blaze_face_short_range.tflite) can be found
            pb.directory(scriptBasePath.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Read stdout and stderr concurrently to avoid deadlocks
            // (Python could fill the stderr buffer while we block reading stdout)
            Thread stderrThread = new Thread(() -> {});
            StringBuilder stderr = new StringBuilder();
            stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.warn("Error reading Python stderr", e);
                }
            });
            stderrThread.setDaemon(true);
            stderrThread.start();

            // Read stdout on the main thread
            StringBuilder stdout = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line);
                }
            }

            // Wait for process with timeout (60s — first run downloads models)
            boolean completed = process.waitFor(60, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("Face verification timed out (60s). Please try again.");
            }

            stderrThread.join(2000); // Wait for stderr thread to finish

            int exitCode = process.exitValue();
            String stdoutStr = stdout.toString().trim();
            String stderrStr = stderr.toString().trim();

            if (!stderrStr.isEmpty()) {
                log.info("Python script stderr: {}", stderrStr);
            }

            if (exitCode != 0) {
                log.error("Python script exited with code {}. Output: {}. Stderr: {}", exitCode, stdoutStr, stderrStr);
                // Try to parse stdout for a JSON error message from the script
                if (!stdoutStr.isEmpty()) {
                    try {
                        JsonNode node = objectMapper.readTree(stdoutStr);
                        if (node.has("error")) {
                            throw new BadRequestException(node.get("error").asText());
                        }
                    } catch (BadRequestException e) {
                        throw e;
                    } catch (Exception ignored) {}
                }
                throw new BadRequestException("Face verification failed. Please try again with a clearer picture.");
            }

            if (stdoutStr.isEmpty()) {
                log.error("Python script output is empty");
                throw new BadRequestException("Face verification failed. Please try again with a clearer picture.");
            }

            // Parse stdout JSON
            JsonNode node = objectMapper.readTree(stdoutStr);
            if (node.has("error")) {
                String errorMsg = node.get("error").asText();
                log.warn("Face detection error: {}", errorMsg);
                throw new BadRequestException(errorMsg);
            }
            
            if (node.isArray()) {
                // Valid descriptor — return as JSON string
                return stdoutStr;
            } else {
                throw new BadRequestException("Invalid response format from face verification service.");
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing python face descriptor extraction", e);
            throw new RuntimeException("Face verification system error: " + e.getMessage(), e);
        }
    }
}
