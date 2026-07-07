package com.accolie.lib.lib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.accolie.lib.lib.entity.*;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    // Slow query: pg_sleep(5) makes PostgreSQL actually sleep for 5 seconds at DB level
    @Query(value = "SELECT b.* FROM book b, pg_sleep(5) WHERE 1=1", nativeQuery = true)
    List<Book> slowQuery();

    // Error query: queries a table that doesn't exist — causes DB-level error
    @Query(value = "SELECT * FROM non_existent_table_xyz", nativeQuery = true)
    List<Book> errorQuery();
}
