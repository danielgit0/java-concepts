package org.example.hello_interview_lld;

public class Seat {
  private boolean isAvailable;
  private String occupant;

  public Seat(boolean isAvailable) {
    this.isAvailable = isAvailable;
  }

  public boolean isAvailable() {
    return isAvailable;
  }

  public void setAvailable(boolean available) {
    isAvailable = available;
  }

  public String getOcuppant() {
    return occupant;
  }

  public void setOcuppant(String ocuppant) {
    this.occupant = ocuppant;
  }
}
