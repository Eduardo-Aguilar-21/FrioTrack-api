package com.mt.friotrackapi.persistence.repository;

import com.mt.friotrackapi.persistence.entity.AppStoreEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppStoreRepository extends JpaRepository<AppStoreEntry, String> {
}
