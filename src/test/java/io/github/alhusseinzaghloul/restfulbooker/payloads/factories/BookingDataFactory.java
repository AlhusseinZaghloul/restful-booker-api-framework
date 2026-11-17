package io.github.alhusseinzaghloul.restfulbooker.payloads.factories;

import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingDates;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.BookingPayload;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.PartialBookingPayload;
import io.github.alhusseinzaghloul.restfulbooker.payloads.pojos.TextPriceBookingPayload;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Dynamic booking data (Rule 3.9). Every booking carries the automation marker (Rule 3.24, O3). */
public final class BookingDataFactory {

    /** Marker for automation data; the pre-suite sweep searches for it. */
    public static final String MARKER_LASTNAME = "QA-AUTO";

    /** Far above the live id range (about 5 100 on 2026-11-17). */
    public static final int NON_EXISTENT_ID = 999_999_999;

    private static final List<String> NEEDS = List.of("Breakfast", "Dinner", "Late checkout");
    private static final ThreadLocal<Faker> FAKER = ThreadLocal.withInitial(Faker::new); // Faker is not thread-safe

    private BookingDataFactory() {}

    public static BookingPayload valid() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        LocalDate checkin = LocalDate.now().plusDays(random.nextInt(30, 366));
        LocalDate checkout = checkin.plusDays(random.nextInt(1, 15));
        return BookingPayload.builder()
                .firstname(uniqueFirstname())
                .lastname(MARKER_LASTNAME)
                .totalprice(random.nextInt(50, 2001))
                .depositpaid(random.nextBoolean())
                .bookingdates(new BookingDates(checkin.toString(), checkout.toString()))
                .additionalneeds(NEEDS.get(random.nextInt(NEEDS.size())))
                .build();
    }

    /** Every field differs from the original except the marker lastname. */
    public static BookingPayload updated(BookingPayload original) {
        BookingDates dates = original.bookingdates();
        String nextNeed = NEEDS.get((NEEDS.indexOf(original.additionalneeds()) + 1) % NEEDS.size());
        return original.toBuilder()
                .firstname(uniqueFirstname())
                .totalprice(original.totalprice() + 100)
                .depositpaid(!original.depositpaid())
                .bookingdates(new BookingDates(shiftDays(dates.checkin(), 7), shiftDays(dates.checkout(), 7)))
                .additionalneeds(nextNeed)
                .build();
    }

    /** New firstname and totalprice only. The price range (2001–3000) never matches valid() (50–2000). */
    public static PartialBookingPayload partialUpdate() {
        return PartialBookingPayload.builder()
                .firstname(uniqueFirstname())
                .totalprice(ThreadLocalRandom.current().nextInt(2001, 3001))
                .build();
    }

    /** Missing required field (O6). firstname is left out of the JSON through NON_NULL. */
    public static BookingPayload missingFirstname() {
        return valid().toBuilder().firstname(null).build();
    }

    /** Wrong data type (O6): totalprice as text. */
    public static TextPriceBookingPayload textPrice() {
        BookingPayload valid = valid();
        return new TextPriceBookingPayload(valid.firstname(), valid.lastname(), "not-a-number",
                valid.depositpaid(), valid.bookingdates(), valid.additionalneeds());
    }

    public static String shiftDays(String isoDate, int days) {
        return LocalDate.parse(isoDate).plusDays(days).toString();
    }

    private static String uniqueFirstname() {
        return FAKER.get().name().firstName() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
