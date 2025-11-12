package com.securechat.webapi.repository;

import com.securechat.webapi.entity.LinkPreviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkPreviewRepository extends JpaRepository<LinkPreviewEntity, Long> {
    Optional<LinkPreviewEntity> findByUrl(String url);
}




