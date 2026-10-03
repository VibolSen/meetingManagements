package Vibol.SEN.meetingManagements.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemInfoResponse {
    private String appName;
    private String version;
    private String builder;
    private String environment;
    private String status;
    private String serverTime;
}
