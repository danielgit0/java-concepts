package org.example.hello_interview_lld;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SeatBookingService {

  Map<String, Seat> seats = new HashMap<>();
  Map<String, Seat> seatsConcurrent = new ConcurrentHashMap<>();

  public boolean bookSeat(String seatId, String occupantId) {
    Seat seat = seats.getOrDefault(seatId, new Seat(true));
    if (seat.isAvailable()) {
      seat.setOcuppant(occupantId);
      seat.setAvailable(false);
      seats.put(seatId, seat);
      return true;
    }
    return false;
  }

  public boolean bookSeatSynchronized(String seatId, String occupantId) {
    Seat seat = seatsConcurrent.computeIfAbsent(seatId, _ -> new Seat(true));

    synchronized (seat) {
      if (seat.isAvailable()) {
        seat.setOcuppant(occupantId);
        seat.setAvailable(false);
        seats.put(seatId, seat);
        return true;
      }
      return false;
    }
  }
}
