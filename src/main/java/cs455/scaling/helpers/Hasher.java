package cs455.scaling.helpers;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// * Class containing a static method that returns the SHA-1 hash for a given byte array

public class Hasher {
    public static String SHA1FromBytes(byte[] data) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            if (Constants.DEBUG) {
                System.out.println("SHA1 Algorithm not found in Hasher.java");
                e.printStackTrace();
            }
        }
        if (digest != null) {
            byte[] hash = digest.digest(data);
            BigInteger hashInt = new BigInteger(1, hash);
            return hashInt.toString(16);
        } else {
            return null;
        }

    }
}
