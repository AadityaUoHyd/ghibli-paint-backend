package org.aadi.ghibli_paint.repository;

import org.aadi.ghibli_paint.entity.GeneratedImage;
import org.aadi.ghibli_paint.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedImageRepository extends JpaRepository<GeneratedImage, Long> {
    List<GeneratedImage> findByUserOrderByCreatedAtDesc(User user);
    List<GeneratedImage> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUser(User user);
}
