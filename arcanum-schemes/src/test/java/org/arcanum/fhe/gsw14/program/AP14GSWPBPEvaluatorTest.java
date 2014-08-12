package org.arcanum.fhe.gsw14.program;

import org.arcanum.fhe.gsw14.field.AP14GSW14Field;
import org.arcanum.program.assignment.BooleanAssignment;
import org.arcanum.program.assignment.BooleanAssignmentGenerator;
import org.arcanum.program.assignment.ElementAssignment;
import org.arcanum.program.circuit.BooleanCircuit;
import org.arcanum.program.circuit.BooleanCircuitEvaluator;
import org.arcanum.program.circuit.smart.SmartBooleanCircuitLoader;
import org.arcanum.program.pbp.BooleanCircuitToBooleanPBP;
import org.arcanum.program.pbp.PermutationBranchingProgram;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertSame;

@RunWith(value = Parameterized.class)
public class AP14GSWPBPEvaluatorTest {

    @Parameterized.Parameters
    public static Collection parameters() {
        Object[][] data = {
                {"org/arcanum/program/circuit/bool/gates/or.txt"},
                {"org/arcanum/program/circuit/bool/gates/and.txt"},
                {"org/arcanum/program/circuit/bool/gates/nand.txt"},
                {"org/arcanum/program/circuit/bool/gates/mod2.txt"},
//                {"org/arcanum/circuit/circuit.txt"},
//                {"org/arcanum/circuit/circuit2.txt"},
//                {"org/arcanum/circuit/circuit3.txt"},
//                {"org/arcanum/circuit/parity4inputs.txt"},
        };

        return Arrays.asList(data);
    }


    protected String circuitPath;

    private SecureRandom random;
    private AP14GSW14Field field;



    public AP14GSWPBPEvaluatorTest(String circuitPath) {
        this.circuitPath = circuitPath;
    }


    @Before
    public void setUp() throws Exception {
        random = SecureRandom.getInstance("SHA1PRNG");
        field = new AP14GSW14Field(random, 4, 30);
    }


    @Test
    public void testCircuitEvaluation() {
        BooleanCircuit circuit =new SmartBooleanCircuitLoader().load(circuitPath);

        // Convert it to a PBP
        PermutationBranchingProgram pbp = new BooleanCircuitToBooleanPBP().convert(circuit);
        System.out.println("pbp = " + pbp);

        // Verify that they evaluates to the same value.

        AP14GSWPBPEvaluator pbpEvaluator = new AP14GSWPBPEvaluator();
        BooleanCircuitEvaluator circuitEvaluator = new BooleanCircuitEvaluator();

        BooleanAssignmentGenerator assignmentGenerator = new BooleanAssignmentGenerator(circuit.getNumInputs());
        for (BooleanAssignment assignment : assignmentGenerator) {
            System.out.println("assignment = " + assignment);
            assertSame(
                    circuitEvaluator.evaluate(circuit, assignment),
                    !BigInteger.ONE.equals(pbpEvaluator.evaluate(
                            pbp,new ElementAssignment(field, assignment)).toBigInteger())
            );
        }
    }

}