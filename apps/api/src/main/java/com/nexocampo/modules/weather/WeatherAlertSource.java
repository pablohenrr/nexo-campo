package com.nexocampo.modules.weather;

import java.util.List;

public interface WeatherAlertSource {
    boolean isLive();

    List<IncomingAlert> official();

    List<IncomingAlert> forecast(double latitude, double longitude);
}
