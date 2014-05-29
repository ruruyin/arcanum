package org.arcanum.field.z;

import org.arcanum.Field;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * @author Angelo De Caro (arcanumlib@gmail.com)
 */
public class ZFieldSelector {

    private static ZFieldSelector INSTANCE = new ZFieldSelector();

    public static ZFieldSelector getInstance() {
        return INSTANCE;
    }

    private ZFieldSelector() {
    }


    public Field getSymmetricZrFieldPowerOfTwo(SecureRandom random, int k) {
        if (k <= 30) {
            return new SymmetricLongZrField(random, 1 << k);
        } else
            return new SymmetricZrField(random, BigInteger.ONE.shiftLeft(k));
    }
}