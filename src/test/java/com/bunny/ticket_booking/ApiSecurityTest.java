package com.bunny.ticket_booking;

import com.bunny.ticket_booking.entity.Role;
import com.bunny.ticket_booking.entity.User;
import com.bunny.ticket_booking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiSecurityTest {

    private static final String EVENT_BODY =
            "{\"name\":\"Test Event\",\"venue\":\"Test Hall\","
                    + "\"showTime\":\"2030-01-01T19:00:00\",\"rows\":1,\"seatsPerRow\":3}";

    @Value("${local.server.port}")
    int port;

    @Autowired
    UserRepository userRepository;

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private record Account(String email, String token) {
    }

    // ---------- 401: no token or a bad token ----------

    @Test
    void protectedEndpoints_withoutToken_return401() throws Exception {
        assertEquals(401, call("POST", "/api/events", null, EVENT_BODY).statusCode());
        assertEquals(401, call("POST", "/api/seats/1/hold", null, null).statusCode());
        assertEquals(401, call("POST", "/api/seats/1/confirm", null, null).statusCode());
        assertEquals(401, call("POST", "/api/bookings/1/cancel", null, null).statusCode());
        assertEquals(401, call("GET", "/api/bookings/me", null, null).statusCode());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        assertEquals(401, call("GET", "/api/bookings/me", "not.a.jwt", null).statusCode());
        assertEquals(401, call("POST", "/api/seats/1/hold", "abc", null).statusCode());
    }

    // ---------- 403 and admin-only event creation ----------

    @Test
    void createEvent_asNormalUser_returns403() throws Exception {
        String user = registerAccount("plain").token();

        assertEquals(403, call("POST", "/api/events", user, EVENT_BODY).statusCode());
    }

    @Test
    void createEvent_asAdmin_returns201() throws Exception {
        String admin = registerAdmin();

        HttpResponse<String> response = call("POST", "/api/events", admin, EVENT_BODY);

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"name\":\"Test Event\""), response.body());
    }

    @Test
    void browsingEventsAndSeats_worksWithoutToken() throws Exception {
        long eventId = createEvent();

        assertEquals(200, call("GET", "/api/events", null, null).statusCode());
        assertEquals(200, call("GET", "/api/events/" + eventId + "/seats", null, null).statusCode());
    }

    // ---------- owner-only cancel ----------

    @Test
    void cancel_byAnotherUser_returns403_ownerCanCancel_secondCancelReturns409() throws Exception {
        long seatId = firstSeatOfNewEvent();
        String owner = registerAccount("owner").token();
        String other = registerAccount("other").token();

        assertEquals(200, call("POST", "/api/seats/" + seatId + "/hold", owner, null).statusCode());
        HttpResponse<String> booked = call("POST", "/api/seats/" + seatId + "/confirm", owner, null);
        assertEquals(200, booked.statusCode());
        String bookingId = firstId(booked.body());

        assertEquals(403, call("POST", "/api/bookings/" + bookingId + "/cancel", other, null).statusCode());
        assertEquals(200, call("POST", "/api/bookings/" + bookingId + "/cancel", owner, null).statusCode());
        assertEquals(409, call("POST", "/api/bookings/" + bookingId + "/cancel", owner, null).statusCode());
    }

    // ---------- the token decides who is acting ----------

    @Test
    void userIdInRequestBody_isIgnored_theTokenDecidesWhoHolds() throws Exception {
        long seatId = firstSeatOfNewEvent();
        Account holder = registerAccount("holder");
        Account intruder = registerAccount("intruder");
        long holderId = userRepository.findByEmail(holder.email()).orElseThrow().getId();
        long intruderId = userRepository.findByEmail(intruder.email()).orElseThrow().getId();

        // the holder sends the intruder's id in the body; it must be ignored
        assertEquals(200, call("POST", "/api/seats/" + seatId + "/hold", holder.token(),
                "{\"userId\": " + intruderId + "}").statusCode());

        assertEquals(409, call("POST", "/api/seats/" + seatId + "/confirm",
                intruder.token(), null).statusCode());

        HttpResponse<String> confirmed = call("POST", "/api/seats/" + seatId + "/confirm",
                holder.token(), null);
        assertEquals(200, confirmed.statusCode());
        assertTrue(confirmed.body().contains("\"userId\":" + holderId), confirmed.body());
    }

    @Test
    void myBookings_listsOnlyTheCallersBookings() throws Exception {
        long seatId = firstSeatOfNewEvent();
        String owner = registerAccount("mine").token();
        String other = registerAccount("notmine").token();

        assertEquals(200, call("POST", "/api/seats/" + seatId + "/hold", owner, null).statusCode());
        assertEquals(200, call("POST", "/api/seats/" + seatId + "/confirm", owner, null).statusCode());

        HttpResponse<String> mine = call("GET", "/api/bookings/me", owner, null);
        assertEquals(200, mine.statusCode());
        assertEquals(1, count(mine.body(), "\"seatNumber\""));

        HttpResponse<String> theirs = call("GET", "/api/bookings/me", other, null);
        assertEquals(200, theirs.statusCode());
        assertEquals("[]", theirs.body());
    }

    // ---------- register and login ----------

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        Account first = registerAccount("dup");

        HttpResponse<String> duplicate = call("POST", "/api/auth/register", null,
                "{\"name\":\"Dup\",\"email\":\"" + first.email() + "\",\"password\":\"password123\"}");

        assertEquals(409, duplicate.statusCode());
    }

    @Test
    void register_invalidInput_returns400() throws Exception {
        HttpResponse<String> response = call("POST", "/api/auth/register", null,
                "{\"name\":\"\",\"email\":\"bad\",\"password\":\"123\"}");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("fields"), response.body());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        Account account = registerAccount("login");

        HttpResponse<String> response = call("POST", "/api/auth/login", null,
                "{\"email\":\"" + account.email() + "\",\"password\":\"wrongpassword\"}");

        assertEquals(401, response.statusCode());
    }

    @Test
    void login_correctPassword_returnsToken() throws Exception {
        Account account = registerAccount("login-ok");

        HttpResponse<String> response = call("POST", "/api/auth/login", null,
                "{\"email\":\"" + account.email() + "\",\"password\":\"password123\"}");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"token\""), response.body());
    }

    // ---------- helpers ----------

    private HttpResponse<String> call(String method, String path, String token, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        builder.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private Account registerAccount(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@test.com";
        HttpResponse<String> response = call("POST", "/api/auth/register", null,
                "{\"name\":\"" + prefix + "\",\"email\":\"" + email + "\",\"password\":\"password123\"}");
        assertEquals(201, response.statusCode());
        return new Account(email, extractToken(response.body()));
    }

    private String registerAdmin() throws Exception {
        Account account = registerAccount("admin");
        User user = userRepository.findByEmail(account.email()).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.save(user);

        // the role is inside the token, so log in again to get an ADMIN token
        HttpResponse<String> login = call("POST", "/api/auth/login", null,
                "{\"email\":\"" + account.email() + "\",\"password\":\"password123\"}");
        assertEquals(200, login.statusCode());
        return extractToken(login.body());
    }

    private long createEvent() throws Exception {
        HttpResponse<String> response = call("POST", "/api/events", registerAdmin(), EVENT_BODY);
        assertEquals(201, response.statusCode());
        return Long.parseLong(firstId(response.body()));
    }

    private long firstSeatOfNewEvent() throws Exception {
        long eventId = createEvent();
        HttpResponse<String> seats = call("GET", "/api/events/" + eventId + "/seats", null, null);
        assertEquals(200, seats.statusCode());
        return Long.parseLong(firstId(seats.body()));
    }

    private String extractToken(String body) {
        return find(body, "\"token\"\\s*:\\s*\"([^\"]+)\"");
    }

    private String firstId(String body) {
        return find(body, "\"id\"\\s*:\\s*(\\d+)");
    }

    private String find(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        assertTrue(matcher.find(), "Pattern not found in: " + text);
        return matcher.group(1);
    }

    private int count(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }
}