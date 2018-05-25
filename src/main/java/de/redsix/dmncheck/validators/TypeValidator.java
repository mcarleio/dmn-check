package de.redsix.dmncheck.validators;

import de.redsix.dmncheck.feel.FeelParser;
import de.redsix.dmncheck.feel.FeelTypecheck;
import de.redsix.dmncheck.feel.ExpressionType;
import de.redsix.dmncheck.result.ValidationResult;
import de.redsix.dmncheck.util.Either;
import de.redsix.dmncheck.util.Eithers;
import de.redsix.dmncheck.util.Util;
import de.redsix.dmncheck.validators.core.SimpleValidator;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.DmnElement;
import org.camunda.bpm.model.dmn.instance.Rule;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public abstract class TypeValidator extends SimpleValidator<DecisionTable> {

    abstract String errorMessage();

    abstract boolean isEmptyAllowed();

    Stream<ValidationResult> typecheck(final Rule rule, final Stream<? extends DmnElement> expressions, final Stream<String> variables,
            final Stream<ExpressionType> types) {
        return Util.zip(expressions, variables, types, (expression, variable, type) -> {
            final FeelTypecheck.Context context = new FeelTypecheck.Context();

            context.put(variable, type);

            return typecheckExpression(rule, expression, context, type);
        }).flatMap(List::stream).map(ValidationResult.Builder.BuildStep::build);
    }

    Stream<ValidationResult> typecheck(final Rule rule, final Stream<? extends DmnElement> expressions,
            final Stream<ExpressionType> types) {
        return Util.zip(expressions, types, (expression, type) -> {
            final FeelTypecheck.Context emptyContext = new FeelTypecheck.Context();

            return typecheckExpression(rule, expression, emptyContext, type);
        }).flatMap(List::stream).map(ValidationResult.Builder.BuildStep::build);
    }

    private List<ValidationResult.Builder.BuildStep> typecheckExpression(Rule rule, DmnElement inputEntry, FeelTypecheck.Context context,
            ExpressionType expectedType) {
        final Either<ExpressionType, ValidationResult.Builder.ElementStep> typedcheckResult = FeelParser.parse(inputEntry.getTextContent())
                .bind(feelExpression -> FeelTypecheck.typecheck(context, feelExpression));

        return Eithers.caseOf(typedcheckResult).makeLeft(type -> {
            if (type.isSubtypeOf(expectedType) || isEmptyAllowed() && ExpressionType.TOP.equals(type)) {
                return Collections.<ValidationResult.Builder.BuildStep>emptyList();
            } else {
                return Collections.singletonList(ValidationResult.Builder.init.message(errorMessage()).element(rule));
            }
        }).makeRight(validationResultBuilder -> Collections.singletonList(validationResultBuilder.element(rule)));
    }

    @Override
    public Class<DecisionTable> getClassUnderValidation() {
        return DecisionTable.class;
    }
}
