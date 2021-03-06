package org.arcanum.fe.ip.lostw10.params;

import org.arcanum.Element;

/**
 * @author Angelo De Caro (arcanumlib@gmail.com)
 */
public class IPLOSTW10SecretKeyParameters extends IPLOSTW10KeyParameters {
    private Element K;

    public IPLOSTW10SecretKeyParameters(IPLOSTW10Parameters parameters, Element k) {
        super(true, parameters);
        K = k;
    }

    public Element getK() {
        return K;
    }
}