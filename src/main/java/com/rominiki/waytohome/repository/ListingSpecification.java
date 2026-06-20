package com.rominiki.waytohome.repository;
import com.rominiki.waytohome.dto.ListingSearchCriteria;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.enums.ListingStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class ListingSpecification {

    public static Specification<Listing> build(ListingSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ListingStatus.APPROVED));
            if (criteria.minPrice() != null) {
                predicates.add(cb.ge(root.get("price"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.le(root.get("price"), criteria.maxPrice()));
            }
            if (criteria.location() != null) {
                predicates.add(cb.like(cb.lower(root.get("location")),
                        "%" + criteria.location().toLowerCase() + "%"));
            }
            if (criteria.petFriendly() != null) {
                predicates.add(cb.equal(root.get("petFriendly"), criteria.petFriendly()));
            }
            if (criteria.bedrooms() != null) {
                predicates.add(cb.equal(root.get("bedrooms"), criteria.bedrooms()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
