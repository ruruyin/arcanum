package org.arcanum.trapdoor.mp12.engines;

import org.arcanum.*;
import org.arcanum.sampler.SamplerFactory;
import org.arcanum.trapdoor.mp12.params.MP12PLP2PublicKeyParameters;
import org.arcanum.util.cipher.engine.AbstractElementCipher;

import java.math.BigInteger;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.arcanum.field.floating.ApfloatUtils.ITWO;

/**
 * @author Angelo De Caro (arcanumlib@gmail.com)
 */
public class MP12PLP2Sampler extends AbstractElementCipher {

    protected MP12PLP2PublicKeyParameters parameters;
    protected int n, k;

    protected Sampler<BigInteger> sampler;
    protected Queue<BigInteger> zero, one;


    public ElementCipher init(ElementCipherParameters param) {
        this.parameters = (MP12PLP2PublicKeyParameters) param;

        this.n = parameters.getParameters().getN();
        this.k = parameters.getK();
        this.sampler = SamplerFactory.getInstance().getDiscreteGaussianSampler(
                parameters.getParameters().getRandom(),
                parameters.getRandomizedRoundingParameter().multiply(ITWO)
        );
        this.zero = new ConcurrentLinkedDeque<BigInteger>();
        this.one = new ConcurrentLinkedDeque<BigInteger>();

        return this;
    }

    public Element processElements(Element... input) {
        Vector syndrome = (Vector) input[0];
        if (syndrome.getSize() != n)
            throw new IllegalArgumentException("Invalid syndrome length.");

        Vector r = parameters.getPreimageField().newElement();

        for (int i = 0, base = 0; i < n; i++) {

            BigInteger u = syndrome.getAt(i).toBigInteger();

            for (int j = 0; j < k; j++) {
                BigInteger xj = sampleZ(u);
                r.getAt(base + j).set(xj);

                u  = u.subtract(xj).shiftRight(1);
            }

            base += k;
        }

        return r;
    }

    public Element processElementsTo(Element to, Element... input) {
        Vector syndrome = (Vector) input[0];
        if (syndrome.getSize() != n)
            throw new IllegalArgumentException("Invalid syndrome length.");

        Vector r = (Vector) to;

        for (int i = 0, base = 0; i < n; i++) {

            BigInteger u = (syndrome.isZeroAt(i)) ? BigInteger.ZERO : syndrome.getAt(i).toBigInteger();

            for (int j = 0; j < k; j++) {
                BigInteger xj = sampleZ(u);
                r.getAt(base + j).set(xj);

                u  = u.subtract(xj).shiftRight(1);
            }

            base += k;
        }

        return r;
    }



    private BigInteger sampleZ(BigInteger u) {
        boolean uLSB = u.testBit(0);

        // TODO: store cache??
        BigInteger value = null;
//        if (uLSB)
//            value = one.poll();
//        else
//            value = zero.poll();

//        if (value == null) {
            while (true) {
                BigInteger x = sampler.sample();
                boolean xLSB = x.testBit(0);
                if (xLSB == uLSB) {
                    value = x;
                    break;
                } else {
//                    if (xLSB)
//                        one.add(x);
//                    else
//                        zero.add(x);
                }
            }
//        }

        return value;
    }


}