package uk.gov.hmcts.opal.common.repository.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;


public abstract class EntitySpecs<E> {

    @SafeVarargs
    protected final List<Specification<E>> specificationList(Optional<Specification<E>>... optionalSpecs) {
        return Arrays.stream(optionalSpecs)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    @SafeVarargs
    protected final List<Specification<E>> specificationList(List<Optional<Specification<E>>> specsList,
                                                          Optional<Specification<E>>... optionalSpecs) {
        return combine(specsList, optionalSpecs)
            .stream().filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    @SafeVarargs
    protected final List<Specification<E>> specificationList(List<Optional<Specification<E>>> specsList,
                                                          Specification<E>... specs) {
        List<Specification<E>> filteredList = specsList
            .stream().filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        Collections.addAll(filteredList, specs);
        return filteredList;
    }

    @SafeVarargs
    protected final List<Optional<Specification<E>>> combine(List<Optional<Specification<E>>> specsList,
                                                          Optional<Specification<E>>... optionalSpecs) {
        Collections.addAll(specsList, optionalSpecs);
        return specsList;
    }

    @SafeVarargs
    protected final List<Predicate> predicateList(Optional<Predicate>... optionalPredicates) {
        return Arrays.stream(optionalPredicates)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    protected final List<Predicate> predicateList(List<Predicate> predicates) {
        return predicates.stream()
            .filter(Objects::nonNull)
            .toList();
    }

    @SafeVarargs
    protected final Predicate[] predicateArray(Optional<Predicate>... optionalPredicates) {
        return predicateList(optionalPredicates).toArray(new Predicate[] {});
    }

    protected final Predicate[] predicateArray(List<Predicate> predicates) {
        return predicateList(predicates).toArray(new Predicate[] {});
    }

    protected Optional<String> notBlank(String candidate) {
        return Optional.ofNullable(candidate).filter(s -> !s.isBlank());
    }

    protected Optional<String> numeric(String candidate) {
        return notBlank(candidate).filter(s -> s.matches("\\d+"));
    }

    protected Optional<Long> numericLong(String candidate) {
        return numeric(candidate).map(Long::parseLong);
    }

    protected Optional<Integer> numericInteger(String candidate) {
        return numeric(candidate).map(Integer::parseInt);
    }

    protected Optional<Short> numericShort(String candidate) {
        return numeric(candidate).map(Short::parseShort);
    }

    protected Optional<Boolean> trueFalse(String candidate) {
        return notBlank(candidate).map(Boolean::parseBoolean);
    }

    protected <T> Optional<T> notNullObject(T candidate) {
        return Optional.ofNullable(candidate);
    }

    protected <T> Optional<Collection<T>> notEmpty(Collection<T> collection) {
        return collection.isEmpty() ? Optional.empty() : Optional.of(collection);
    }

    protected static Predicate likeWildcardPredicate(
        Expression<String> path, CriteriaBuilder cb, String candidate) {
        return likeLowerCaseBothPredicate(path, cb, "%" + candidate + "%");
    }

    protected static Predicate likeLowerCaseWildcardPredicate(
        Expression<String> path, CriteriaBuilder cb, String candidate) {
        return likeLowerCasePredicate(path, cb, "%" + candidate + "%");
    }

    protected static Predicate likeLowerCaseBothStartsWithPredicate(
        Expression<String> path, CriteriaBuilder cb, String candidate) {
        return likeLowerCaseBothPredicate(path, cb, candidate + "%");
    }

    protected static Predicate likeLowerCaseBothPredicate(Expression<String> path, CriteriaBuilder cb,
        String candidate) {
        return cb.like(cb.lower(path), candidate.toLowerCase());
    }

    protected static Predicate equalsLowerCaseBothPredicate(
        Expression<String> path, CriteriaBuilder cb, String candidate) {
        return cb.equal(cb.lower(path), candidate.toLowerCase());
    }

    protected static Predicate likeLowerCaseStartsWithPredicate(
        Expression<String> path, CriteriaBuilder cb, String candidate) {
        return likeLowerCasePredicate(path, cb, candidate + "%");
    }

    protected static Predicate likeLowerCaseEndsWithPredicate(
        Expression<String> path, CriteriaBuilder cb, String candidate) {
        return likeLowerCasePredicate(path, cb, "%" + candidate);
    }

    protected static Predicate likeLowerCasePredicate(Expression<String> path, CriteriaBuilder cb, String candidate) {
        return cb.like(cb.lower(path), candidate);
    }

    protected static Predicate equalsLowerCasePredicate(Expression<String> path, CriteriaBuilder cb, String candidate) {
        return cb.equal(cb.lower(path), candidate);
    }

    protected static Optional<LocalDateTime> notNullOffsetDateTime(OffsetDateTime value) {
        return Optional.ofNullable(value).map(OffsetDateTime::toLocalDateTime);
    }

    protected static Predicate andAll(CriteriaBuilder cb, List<Predicate> predicates) {
        predicates.removeIf(Objects::isNull);
        return predicates.isEmpty() ? null : cb.and(predicates.toArray(Predicate[]::new));
    }

    protected static Predicate orAll(CriteriaBuilder cb, List<Predicate> predicates) {
        predicates.removeIf(Objects::isNull);
        return predicates.isEmpty() ? null : cb.or(predicates.toArray(Predicate[]::new));
    }
}
