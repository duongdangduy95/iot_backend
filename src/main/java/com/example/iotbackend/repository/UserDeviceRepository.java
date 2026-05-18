package com.example.iotbackend.repository;

import com.example.iotbackend.entity.Device;
import com.example.iotbackend.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    void deleteByDeviceId(Long id);

    boolean existsByUserIdAndDeviceId(Long id, Long id1);

    List<UserDevice> findByUserId(Long id);

    boolean existsByDeviceId(Long id);

//    List<Device> findByUserId(Long id);
Optional<UserDevice> findByUserIdAndDeviceId(
        Long userId,
        Long deviceId
);

}
