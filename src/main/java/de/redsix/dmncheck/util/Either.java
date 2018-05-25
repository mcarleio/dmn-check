package de.redsix.dmncheck.util;

import org.derive4j.Data;
import org.derive4j.Derive;
import org.derive4j.FieldNames;
import org.derive4j.Make;

import java.util.function.Function;

@Data(value = @Derive(make = {Make.constructors, Make.caseOfMatching, Make.getters}))
public abstract class Either<A, B> {
    public abstract <X> X match(@FieldNames("left") Function<A, X> makeLeft, @FieldNames("right") Function<B, X> makeRight);

    public <C> Either<C, B> bind(Function<A, Either<C, B>> function) {
        return this.match(function, Eithers::makeRight);
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();
}
