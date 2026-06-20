package com.rominiki.waytohome.repository;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.enums.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);

}