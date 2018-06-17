package de.redsix.dmncheck.feel;

import de.redsix.dmncheck.result.ValidationResult;
import de.redsix.dmncheck.util.Either;
import de.redsix.dmncheck.util.Eithers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static de.redsix.dmncheck.util.Eithers.makeLeft;

public final class FeelTypecheck {

    private FeelTypecheck() {

    }

    public final static class Context extends HashMap<String, ExpressionType> { }

    public static Either<ExpressionType, ValidationResult.Builder.ElementStep> typecheck(final FeelExpression expression) {
        return typecheck(new Context(), expression);
    }

    public static Either<ExpressionType, ValidationResult.Builder.ElementStep> typecheck(final Context context, final FeelExpression expression) {
        return FeelExpressions.caseOf(expression)
                // FIXME: 12/10/17 The explicit type is needed as otherwise the type of 'right' is lost.
                .<Either<ExpressionType, ValidationResult.Builder.ElementStep>>Empty(() -> makeLeft(ExpressionTypes.TOP()))
                .BooleanLiteral(bool -> makeLeft(ExpressionTypes.BOOLEAN()))
                .DateLiteral(dateTime -> makeLeft(ExpressionTypes.DATE()))
                .DoubleLiteral(aDouble -> makeLeft(ExpressionTypes.DOUBLE()))
                .IntegerLiteral(integer -> makeLeft(ExpressionTypes.INTEGER()))
                .StringLiteral(string -> makeLeft(ExpressionTypes.STRING()))
                .VariableLiteral(name ->
                    check(context.containsKey(name), "Variable '" + name + "' has no type.")
                    .orElse(makeLeft(context.get(name))))
                .RangeExpression((__, lowerBound, upperBound, ___) -> typecheckRangeExpression(context, lowerBound, upperBound))
                .UnaryExpression((operator, operand) -> typecheckUnaryExpression(context, operator, operand))
                .BinaryExpression((left, operator, right) -> typecheckBinaryExpression(context, left, operator, right))
                .DisjunctionExpression((head, tail) -> typecheckDisjunctionExpression(context, head, tail)
                );
    }

    private static Either<ExpressionType, ValidationResult.Builder.ElementStep> typecheckDisjunctionExpression(final Context context, final FeelExpression head, final FeelExpression tail) {
        return typecheck(context, head).bind(headType ->
                typecheck(context, tail).bind(tailType ->
                    check(headType.equals(tailType), "Types of head and tail do not match.")
                            .orElse(makeLeft(headType))
                ));
    }

    private static Either<ExpressionType, ValidationResult.Builder.ElementStep> typecheckBinaryExpression(final Context context, final FeelExpression left, final Operator operator, final FeelExpression right) {
        return typecheck(context, left).bind(leftType ->
                typecheck(context, right).bind(rightType ->
                    check(leftType.equals(rightType), "Types of left and right operand do not match.")
                    .orElse(checkOperatorCompatibility(leftType, operator))
                ));
    }

    private static Either<ExpressionType, ValidationResult.Builder.ElementStep> typecheckUnaryExpression(final Context context, final Operator operator, final FeelExpression operand) {
        final Stream<Operator> allowedOperators = Stream.of(Operator.GT, Operator.GE, Operator.LT, Operator.LE);
        return typecheck(context, operand).bind(type ->
                    check(allowedOperators.anyMatch(operator::equals), "Operator is not supported in UnaryExpression.")
                    .orElse(checkOperatorCompatibility(type, operator))
                );
    }

    private static Either<ExpressionType, ValidationResult.Builder.ElementStep> checkOperatorCompatibility(final ExpressionType type, final Operator operator) {
        switch (operator) {
            case GE:
            case GT:
            case LE:
            case LT:
            case DIV:
            case EXP:
            case MUL:
            case ADD:
            case SUB:
                return check(ExpressionType.isNumeric(type),
                        "Operator " + operator + " expects numeric type but got " + type).orElse(makeLeft(type));
            case OR:
            case AND:
            case NOT:
                return check(ExpressionTypes.BOOLEAN().equals(type),
                        "Operator " + operator + "expects boolean but got " + type).orElse(makeLeft(type));
            default:
                return Eithers.makeRight(ValidationResult.init.message("Unexpected operand " + operator));

        }
    }

    private static Either<ExpressionType, ValidationResult.Builder.ElementStep> typecheckRangeExpression(final Context context, final FeelExpression lowerBound, final FeelExpression upperBound) {
        final List<ExpressionType> allowedTypes = Arrays
                .asList(ExpressionTypes.INTEGER(), ExpressionTypes.DOUBLE(), ExpressionTypes.LONG(), ExpressionTypes.DATE());
        return typecheck(context, lowerBound).bind(lowerBoundType ->
                typecheck(context, upperBound).bind(upperBoundType ->
                        check(lowerBoundType.equals(upperBoundType), "Types of lower and upper bound do not match.").map(Optional::of)
                        .orElseGet(() -> check(allowedTypes.contains(lowerBoundType), "Type is unsupported for RangeExpressions."))
                        .orElse(makeLeft(lowerBoundType))
                ));
    }

    private static Optional<Either<ExpressionType, ValidationResult.Builder.ElementStep>> check(final Boolean condition, final String errorMessage) {
        if (!condition) {
            return Optional.of(Eithers.makeRight(ValidationResult.init.message(errorMessage)));
        } else {
            return Optional.empty();
        }
    }
}
