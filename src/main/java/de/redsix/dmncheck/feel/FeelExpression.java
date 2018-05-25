package de.redsix.dmncheck.feel;

import org.derive4j.Data;
import org.derive4j.Derive;
import org.derive4j.FieldNames;
import org.derive4j.Make;
import org.derive4j.Visibility;

import java.time.LocalDateTime;
import java.util.Optional;

@Data(value = @Derive(withVisibility = Visibility.Package, make = {Make.constructors, Make.caseOfMatching, Make.getters}))
public abstract class FeelExpression {

    public interface Cases<R> {
        R Empty();
        R BooleanLiteral(@FieldNames("makeBoolean") Boolean aBoolean);
        R DateLiteral(@FieldNames("makeDateTime") LocalDateTime dateTime);
        R DoubleLiteral(@FieldNames("makeDouble") Double aDouble);
        R IntegerLiteral(@FieldNames("makeInteger") Integer aInteger);
        R StringLiteral(@FieldNames("makeString") String string);
        R VariableLiteral(@FieldNames("makeName") String name);
        R RangeExpression(
                @FieldNames("isLeftInclusive") boolean makeLeftInclusive,
                @FieldNames("lowerBound") FeelExpression makeLowerBound,
                @FieldNames("upperBound") FeelExpression makeUpperBound,
                @FieldNames("isRightInclusive") boolean makeRightInclusive);
        R UnaryExpression(
                @FieldNames("operator") Operator makeOperator,
                @FieldNames("expression") FeelExpression makeExpression);
        R BinaryExpression(
                @FieldNames("left") FeelExpression makeLeft,
                @FieldNames("operator") Operator makeOperator,
                @FieldNames("right") FeelExpression makeRight);
        R DisjunctionExpression(
                @FieldNames("head") FeelExpression makeHead,
                @FieldNames("tail") FeelExpression makeTail);
    }

    public abstract <R> R match(Cases<R> cases);

    public Optional<Boolean> subsumes(final FeelExpression expression) {
        return Subsumption.subsumes(this, expression, Subsumption.eq);
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();
}
