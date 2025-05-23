package com.example.casestudy_g2_m4.service.booking;

import com.example.casestudy_g2_m4.model.Booking;
import com.example.casestudy_g2_m4.model.BookingDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IBookingService {
    List<Booking> findAllBooking();

    void addBooking(Booking booking);

    void addBooking(BookingDTO bookingDTO);

    Optional<Booking> findBookingById(Integer id);

    void updateBooking(BookingDTO bookingDTO);

    void deleteBooking(Integer id);

    List<BookingDTO> search(String keyword,LocalDateTime checkIn, LocalDateTime checkOut, LocalDateTime createAt);
}
