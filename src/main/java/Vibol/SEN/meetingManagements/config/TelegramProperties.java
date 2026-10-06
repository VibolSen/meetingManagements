package Vibol.SEN.meetingManagements.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "telegram")
@Getter
@Setter
public class TelegramProperties {

    private Bot bot = new Bot();
    private Reminder reminder = new Reminder();

    @Getter
    @Setter
    public static class Bot {
        private boolean enabled = true;
        private String token = "8874617484:AAF2jAjzgGkxYla38mFlokCuKDS3bDpjxtY";
        private String username = "MMS_Meeting_Alert_Bot";
        private String defaultChatId = "1035574371";
    }

    @Getter
    @Setter
    public static class Reminder {
        private int defaultMinutes = 10;
    }
}
