package com.nexocampo.modules.weather;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.alertas-climaticos", name = "fontes", havingValue = "stub", matchIfMissing = true)
public class StubWeatherAlertSource implements WeatherAlertSource {

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public List<IncomingAlert> official() {
        return List.of();
    }

    @Override
    public List<IncomingAlert> forecast(double latitude, double longitude) {
        return List.of();
    }
}
