package net.nhatanhn.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import net.nhatanhn.models.File;

@Component
public interface FileRepository extends JpaRepository<File, Long>{
    
}
