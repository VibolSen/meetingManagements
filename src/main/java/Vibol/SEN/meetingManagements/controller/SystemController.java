package Vibol.SEN.meetingManagements.controller;

import Vibol.SEN.meetingManagements.dto.SystemInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Value("${info.app.name:Meeting Management System}")
    private String appName;

    @Value("${info.app.version:1.0.0}")
    private String version;

    @Value("${info.app.builder:Vibol SEN}")
    private String builder;

    @Value("${info.app.environment:production}")
    private String environment;

    @GetMapping("/info")
    public ResponseEntity<SystemInfoResponse> getSystemInfo() {
        SystemInfoResponse response = SystemInfoResponse.builder()
                .appName(appName)
                .version(version)
                .builder(builder)
                .environment(environment)
                .status("OPERATIONAL")
                .serverTime(Instant.now().toString())
                .build();
        return ResponseEntity.ok(response);
    }
}
