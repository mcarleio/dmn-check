package de.redsix.dmncheck.validators;

import de.redsix.dmncheck.result.ValidationResult;
import de.redsix.dmncheck.result.Severity;
import de.redsix.dmncheck.validators.util.WithDecisionTable;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.OutputEntry;
import org.camunda.bpm.model.dmn.instance.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputEntryTypeValidatorTest extends WithDecisionTable {

    private final OutputEntryTypeValidator testee = new OutputEntryTypeValidator();

    @Test
    void shouldAcceptWellTypedInputExpression() {
        final Output output = modelInstance.newInstance(Output.class);
        output.setTypeRef("integer");
        decisionTable.getOutputs().add(output);

        final Rule rule = modelInstance.newInstance(Rule.class);
        final OutputEntry outputEntry = modelInstance.newInstance(OutputEntry.class);
        outputEntry.setTextContent("42");
        rule.getOutputEntries().add(outputEntry);
        decisionTable.getRules().add(rule);

        final List<ValidationResult> validationResults = testee.apply(modelInstance);

        assertTrue(validationResults.isEmpty());
    }

    @Test
    void shouldAcceptIntegersAsLong() {
        final Output output = modelInstance.newInstance(Output.class);
        output.setTypeRef("long");
        decisionTable.getOutputs().add(output);

        final Rule rule = modelInstance.newInstance(Rule.class);
        final OutputEntry outputEntry = modelInstance.newInstance(OutputEntry.class);
        outputEntry.setTextContent("42");
        rule.getOutputEntries().add(outputEntry);
        decisionTable.getRules().add(rule);

        final List<ValidationResult> validationResults = testee.apply(modelInstance);

        assertTrue(validationResults.isEmpty());
    }

    @Test
    void shouldAcceptIntegersAsDouble() {
        final Output output = modelInstance.newInstance(Output.class);
        output.setTypeRef("double");
        decisionTable.getOutputs().add(output);

        final Rule rule = modelInstance.newInstance(Rule.class);
        final OutputEntry outputEntry = modelInstance.newInstance(OutputEntry.class);
        outputEntry.setTextContent("42");
        rule.getOutputEntries().add(outputEntry);
        decisionTable.getRules().add(rule);

        final List<ValidationResult> validationResults = testee.apply(modelInstance);

        assertTrue(validationResults.isEmpty());
    }

    @Test
    void shouldAcceptWellTypedInputExpressionWithoutTypeDeclaration() {
        final Output output = modelInstance.newInstance(Output.class);
        decisionTable.getOutputs().add(output);

        final Rule rule = modelInstance.newInstance(Rule.class);
        final OutputEntry outputEntry = modelInstance.newInstance(OutputEntry.class);
        outputEntry.setTextContent("42");
        rule.getOutputEntries().add(outputEntry);
        decisionTable.getRules().add(rule);

        final List<ValidationResult> validationResults = testee.apply(modelInstance);

        assertTrue(validationResults.isEmpty());
    }

    @Test
    void shouldAcceptEmptyExpression() {
        final Output output = modelInstance.newInstance(Output.class);
        output.setTypeRef("integer");
        decisionTable.getOutputs().add(output);

        final Rule rule = modelInstance.newInstance(Rule.class);
        final OutputEntry outputEntry = modelInstance.newInstance(OutputEntry.class);
        outputEntry.setTextContent(null);
        rule.getOutputEntries().add(outputEntry);
        decisionTable.getRules().add(rule);

        final List<ValidationResult> validationResults = testee.apply(modelInstance);

        assertTrue(validationResults.isEmpty());
    }

    @Test
    void shouldRejectIllTypedInputExpression() {
        final Output output = modelInstance.newInstance(Output.class);
        output.setTypeRef("integer");
        decisionTable.getOutputs().add(output);

        final Rule rule = modelInstance.newInstance(Rule.class);
        final OutputEntry outputEntry = modelInstance.newInstance(OutputEntry.class);
        outputEntry.setTextContent("\"Steak\"");
        rule.getOutputEntries().add(outputEntry);
        decisionTable.getRules().add(rule);

        final List<ValidationResult> validationResults = testee.apply(modelInstance);

        assertEquals(1, validationResults.size());
        final ValidationResult validationResult = validationResults.get(0);
        assertAll(
                () -> assertEquals("Type of output entry does not match type of output expression", validationResult.getMessage()),
                () -> assertEquals(rule, validationResult.getElement()),
                () -> assertEquals(Severity.ERROR, validationResult.getSeverity())
        );
    }

    @Test
    void shouldRejectIllTypedInputExpressionWithoutTypeDeclaration() {
        final Output output = modelInstance.newInstance(Output.class);
        decisionTable.getOutputs().add(output);

        final Rule rule = modelInstance.newInstance(Rule.class);
        final OutputEntry outputEntry = modelInstance.newInstance(OutputEntry.class);
        outputEntry.setTextContent("[1..true]");
        rule.getOutputEntries().add(outputEntry);
        decisionTable.getRules().add(rule);

        final List<ValidationResult> validationResults = testee.apply(modelInstance);

        assertEquals(1, validationResults.size());
        final ValidationResult validationResult = validationResults.get(0);
        assertAll(
                () -> assertEquals("Types of lower and upper bound do not match.", validationResult.getMessage()),
                () -> assertEquals(rule, validationResult.getElement()),
                () -> assertEquals(Severity.ERROR, validationResult.getSeverity())
        );
    }
}
