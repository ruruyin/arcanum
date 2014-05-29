package org.arcanum.trapdoor.mp12.engines;

import org.apfloat.Apfloat;
import org.arcanum.*;
import org.arcanum.field.floating.ApfloatUtils;
import org.arcanum.field.floating.FloatingField;
import org.arcanum.field.vector.MatrixField;
import org.arcanum.sampler.DiscreteGaussianCOVSampler;
import org.arcanum.trapdoor.mp12.params.MP12HLP2PrivateKeyParameters;
import org.arcanum.trapdoor.mp12.params.MP12HLP2PublicKeyParameters;
import org.arcanum.trapdoor.mp12.params.MP12HLP2SampleParameters;
import org.arcanum.util.cipher.params.ElementKeyPairParameters;
import org.arcanum.util.concurrent.PoolExecutor;
import org.arcanum.util.math.Cholesky;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import static org.apfloat.ApfloatMath.sqrt;
import static org.arcanum.field.floating.ApfloatUtils.*;

/**
 * @author Angelo De Caro (arcanumlib@gmail.com)
 */
public class MP12HLP2Sampler extends MP12PLP2Sampler {

    protected static Map<ElementCipherParameters, Matrix> covs = new HashMap<ElementCipherParameters, Matrix>();


    protected MP12HLP2PublicKeyParameters pk;
    protected MP12HLP2PrivateKeyParameters sk;

    protected Sampler<? extends Element> perturbationSampler;


    public ElementCipher init(ElementCipherParameters param) {
        ElementKeyPairParameters keyPair = ((MP12HLP2SampleParameters) param).getKeyPair();

        pk = (MP12HLP2PublicKeyParameters) keyPair.getPublic();
        sk = (MP12HLP2PrivateKeyParameters) keyPair.getPrivate();

        // Init the Primitive Lattice Sampler
        super.init(pk);

        // Init offline sampler
        perturbationSampler = new DiscreteGaussianCOVSampler(
                sk.getParameters().getRandom(),
                computeCoviarianceMatrix(),
                sk.getR().getTargetField(),
                pk.getRandomizedRoundingParameter()
        );

        return this;
    }

    public Element processElements(Element... input) {
        // Offline phase
        // sample perturbation
        Element[] perturbation = samplePerturbation();
        Element p = perturbation[0];
        Element offset = perturbation[1];

        // Online phase
        Element u = input[0];

        // Compute syndrome w
        // offset.negateThenAdd(u)
        Element v = u.duplicate().sub(offset);

        // Compute x
        Element z2 = super.processElements(v);
        Element z1 = sk.getR().mul(z2);

        return ((Vector) p).add(z1, z2);
    }


    protected Element[] samplePerturbation() {
        Element p = perturbationSampler.sample();
        Element offset = pk.getA().mul(p);

        return new Element[]{p, offset};
    }


    protected Matrix computeCoviarianceMatrix() {
        Matrix cov = covs.get(sk);
        if (cov == null) {
            cov = computeCovarianceMatrixInternal();
            covs.put(sk, cov);
        }
        return cov;
    }

    protected Matrix computeCovarianceMatrixInternal() {
        // Setup parameters: compute gaussian parameter s
        int n = sk.getR().getN(); final int m = sk.getR().getM();
        SecureRandom random = sk.getParameters().getRandom();

        Apfloat rrp = pk.getRandomizedRoundingParameter();
        Apfloat rrpSquare = square(rrp);
        Apfloat tworrpSquare = rrpSquare.multiply(IFOUR);

        Apfloat lweNoisParameter = SQRT_TWO.multiply(
                ITWO.multiply(sqrt(newApfloat(n)))
        ).multiply(rrpSquare).multiply(rrp);

        Apfloat sq = square(
                lweNoisParameter.multiply(
                        ApfloatUtils.sqrt(n).add(ApfloatUtils.sqrt(m)).add(ApfloatUtils.IONE)
                ).divide(SQRT_TWO_PI)
        ).add(IONE).multiply(ISIX).multiply(rrpSquare);

//        n+=n;
        FloatingField ff = new FloatingField(random);
        MatrixField<FloatingField> mff = new MatrixField<FloatingField>(random, ff, n + m);

        Element sSquare = ff.newElement(sq);
        Element rSquare = ff.newElement(tworrpSquare);
        Element aSquare = ff.newElement(rrpSquare);

        Element b = sSquare.duplicate().sub(rSquare).sub(aSquare);
        final Element sqrtB = b.duplicate().sqrt();
        final Element rSquarePlusOneOverB = rSquare.duplicate().add(b.duplicate().invert());
        final Element sSquareMinusASquare = sSquare.duplicate().sub(aSquare);
        final Element sqrtBInverse = sqrtB.duplicate().invert();

        // Compute covariance matrix COV
        final Matrix cov = mff.newElement();
        new PoolExecutor().submit(new Runnable() {
            public void run() {
                cov.setSubMatrixToIdentityAt(0, 0, m, sqrtB);
            }
        }).submit(new Runnable() {
            public void run() {
                cov.setSubMatrixFromMatrixAt(m, 0, sk.getR(), new Matrix.Transformer() {
                    public void transform(int row, int col, Element e) {
                        e.mul(sqrtBInverse);
                    }
                });
            }
        }).submit(new Runnable() {
                      public void run() {
                          sk.getR().mulByTransposeTo(cov, m, m, new Matrix.Transformer() {
                              public void transform(int row, int col, Element e) {
                                  e.mul(rSquarePlusOneOverB).negate();
                                  if (row == col)
                                      e.add(sSquareMinusASquare);
                              }
                          });
                      }
                  }
        ).awaitTermination();

//        cov.transform(new Matrix.Transformer() {
//            public void transform(int row, int col, Element e) {
//                if (row >= m && col >=m){
//                    if (row == col)
//                        e.add(sSquareMinusASquare);
//                }
//
//            }
//        });

//        System.out.println("cov = " + cov);

        // Compute Cholesky decomposition
        Cholesky.choleskyAt(cov, m, m);

        return cov;
    }


}