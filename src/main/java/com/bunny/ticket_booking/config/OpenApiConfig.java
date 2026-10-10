package com.bunny.ticket_booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketBookingApi() {
        return new OpenAPI().info(new Info()
                .title("Ticket Booking API")
                .version("1.0")
                .description("Seat booking backend that prevents double booking "
                        + "using pessimistic locking and expiring seat holds"));
    }
}