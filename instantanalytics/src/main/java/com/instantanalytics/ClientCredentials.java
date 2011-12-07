package com.instantanalytics;
public class ClientCredentials {

  /** Value of the "API key" shown under "Simple API Access". */
  public static final String KEY = "AIzaSyBL-o_14njP2mgQ-Xu0KQoxiCS2Gpuc9LY";

  public static void errorIfNotSpecified() {
    if (KEY == null) {
      System.err.println("Please enter your API key in " + ClientCredentials.class);
      System.exit(1);
    }
  }
}
