package com.example.casestudy_g2_m4.service.booking;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.casestudy_g2_m4.model.Booking;
import com.example.casestudy_g2_m4.model.BookingDTO;
import com.example.casestudy_g2_m4.model.BookingInfo;
import com.example.casestudy_g2_m4.model.Payment;
import com.example.casestudy_g2_m4.model.Room;
import com.example.casestudy_g2_m4.model.RoomType;
import com.example.casestudy_g2_m4.repository.IBookingRepository;
import com.example.casestudy_g2_m4.repository.IPaymentRepository;
import com.example.casestudy_g2_m4.service.bookinginfo.IBookingInfoService;
import com.example.casestudy_g2_m4.service.room.IRoomService;
import com.example.casestudy_g2_m4.service.roomtype.IRoomTypeService;
import com.example.casestudy_g2_m4.service.user.IUserService;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService implements IBookingService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    @Autowired
    private IBookingRepository bookingRepository;
    @Autowired
    private IUserService userService;
    @Autowired
    private IRoomTypeService roomTypeService;
    @Autowired
    private IRoomService roomService;
    @Autowired
    private IBookingInfoService bookingInfoService;
    @Autowired
    private IPaymentRepository paymentRepository;

    @Override
    public List<Booking> findAllBooking() {
        return bookingRepository.findAll();
    }

    @Override
    public void addBooking(Booking booking) {
        bookingRepository.save(booking);
    }

    @Override
    public void addBooking(BookingDTO bookingDTO) {
        // Tạo BookingInfo mới
        BookingInfo bookingInfo = new BookingInfo();
        bookingInfo.setName(bookingDTO.getUserName().trim());
        bookingInfo.setEmail(bookingDTO.getEmail());
        bookingInfo = bookingInfoService.save(bookingInfo);
        
        // Lấy tên loại phòng từ BookingDTO
        String enteredRoomTypeName = bookingDTO.getRoomType();
        if (enteredRoomTypeName == null || enteredRoomTypeName.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập loại phòng");
        }
        enteredRoomTypeName = Stream.of(enteredRoomTypeName.trim().toLowerCase().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
        String finalEnteredRoomTypeName = enteredRoomTypeName;
        RoomType roomType = roomTypeService.findByName(enteredRoomTypeName)
                .orElseGet(() -> {
                    RoomType newRoomType = new RoomType();
                    newRoomType.setName(finalEnteredRoomTypeName);
                    newRoomType.setPrice(0.0);
                    newRoomType.setMaxPeople(1);
                    newRoomType.setDescription("User-entered RoomType");
                    return roomTypeService.save(newRoomType);
                });
        Room room = new Room();
        room.setRoomType(roomType);
        int randomFloor = new Random().nextInt(5) + 1;
        String randomRoomNumber = Room.generateRoomNumber(randomFloor);
        room.setRoomNumber(randomRoomNumber);
        room.setStatus(Room.Status.available);
        room = roomService.save(room);
        
        Booking booking = new Booking();
        booking.setBookingInfo(bookingInfo);
        booking.setRoom(room);
        booking.setCheckIn(bookingDTO.getCheckIn());
        booking.setCheckOut(bookingDTO.getCheckOut());
        booking.setStatus(Booking.Status.pending);
        booking.setPaymentStatus(Booking.PaymentStatus.unpaid);
        booking.setCreatedAt(LocalDateTime.now());
        addBooking(booking);

        // Tạo payment mới
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(bookingDTO.getPrice());
        if (bookingDTO.getPaymentMethod() != null) {
            switch (bookingDTO.getPaymentMethod()) {
                case "BANK_TRANSFER":
                    payment.setMethod(Payment.Method.bank);
                    break;
                case "CARD":
                    payment.setMethod(Payment.Method.card);
                    break;
                default:
                    payment.setMethod(Payment.Method.cash);
            }
        } else {
            payment.setMethod(Payment.Method.cash);
        }
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    @Override
    public Optional<Booking> findBookingById(Integer id) {
        return bookingRepository.findById(id);
    }

    @Override
    @Transactional
    public void updateBooking(BookingDTO bookingDTO) {
        Booking exitingBoooking = bookingRepository.findById(bookingDTO.getId()).orElseThrow(() -> new RuntimeException("Booking not found"));
        exitingBoooking.setCheckIn(bookingDTO.getCheckIn());
        exitingBoooking.setCheckOut(bookingDTO.getCheckOut());
        exitingBoooking.setStatus(Booking.Status.valueOf(bookingDTO.getStatus()));
        exitingBoooking.setPaymentStatus(Booking.PaymentStatus.valueOf(bookingDTO.getPaymentStatus()));
        // Cập nhật tên và email qua bookingInfo
        if (bookingDTO.getUserName() != null && !bookingDTO.getUserName().trim().isEmpty()) {
            BookingInfo bookingInfo = exitingBoooking.getBookingInfo();
            if (bookingInfo == null) {
                bookingInfo = new BookingInfo();
                exitingBoooking.setBookingInfo(bookingInfo);
            }
            bookingInfo.setName(bookingDTO.getUserName().trim());
            bookingInfo.setEmail(bookingDTO.getEmail());
            bookingInfoService.save(bookingInfo);
        }
        if (bookingDTO.getRoomType() != null && !bookingDTO.getRoomType().trim().isEmpty()) {
            Room room = exitingBoooking.getRoom();
            if (room != null) {
                String enteredRoomTypeName = bookingDTO.getRoomType();
                enteredRoomTypeName = Stream.of(enteredRoomTypeName.trim().toLowerCase().split("\\s+"))
                        .filter(word -> !word.isEmpty())
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                        .collect(Collectors.joining(" "));
                String finalEnteredRoomTypeName = enteredRoomTypeName;
                RoomType roomType = roomTypeService.findByName(enteredRoomTypeName)
                        .orElseGet(() -> {
                            RoomType newRoomType = new RoomType();
                            newRoomType.setName(finalEnteredRoomTypeName);
                            newRoomType.setPrice(0.0);
                            newRoomType.setMaxPeople(1);
                            newRoomType.setDescription("User-entered RoomType");
                            return roomTypeService.save(newRoomType);
                        });
                room.setRoomType(roomType);
                roomService.save(room);
            }
        }
    }

    @Override
    @Transactional
    public void deleteBooking(Integer id) {
        Optional<Booking> bookingOpt = bookingRepository.findById(id);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            paymentRepository.deleteByBookingId(id);

            if (booking.getBookingInfo() != null) {
                bookingInfoService.deleteById(booking.getBookingInfo().getId());
            }

            bookingRepository.deleteById(id);
        }
    }

    @Override
    public List<BookingDTO> search(String keyword, LocalDateTime checkIn, LocalDateTime checkOut, LocalDateTime createdAt) {
        return bookingRepository.searchByKeyword(keyword, checkIn, checkOut, createdAt)
                .stream()
                .map(BookingDTO::new)
                .collect(Collectors.toList());
    }


}
