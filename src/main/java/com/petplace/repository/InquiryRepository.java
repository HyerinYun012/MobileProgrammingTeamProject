package com.petplace.repository;
import com.petplace.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {}
