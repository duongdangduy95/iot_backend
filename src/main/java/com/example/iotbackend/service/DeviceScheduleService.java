package com.example.iotbackend.service;

import com.example.iotbackend.dto.DeviceScheduleRequest;
import com.example.iotbackend.entity.DeviceSchedule;
import com.example.iotbackend.entity.User;
import com.example.iotbackend.mqtt.DeviceScheduleRunner;
import com.example.iotbackend.repository.DeviceScheduleRepository;
import com.example.iotbackend.repository.UserDeviceRepository;
import com.example.iotbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;


@Service
@RequiredArgsConstructor
public class DeviceScheduleService {

    private final DeviceScheduleRepository repo;
    private final DeviceScheduleRunner runner;
    private final SecurityUtils securityUtils;
    private final AuthService authService;
    private final UserDeviceRepository userDeviceRepository;

    // CREATE SCHEDULE
    public DeviceSchedule create(DeviceScheduleRequest req) {

        User user = securityUtils.getCurrentUser();

        DeviceSchedule s = new DeviceSchedule();

        s.setDeviceId(req.getDeviceId());
        s.setStartTime(req.getStartTime());
        s.setEndTime(req.getEndTime());
        s.setType(req.getType());
        s.setDaysOfWeek(req.getDaysOfWeek());
        s.setExecuteDate(req.getExecuteDate());
        s.setEnabled(req.getEnabled());
        s.setUserId(user.getId());

        return repo.save(s);
    }

    // GET MY SCHEDULES
    public List<DeviceSchedule> getSchedulesByDevice(Long deviceId) {

        User user = securityUtils.getCurrentUser();

        // kiểm tra quyền sở hữu hoặc được share
        boolean hasPermission =
                userDeviceRepository.existsByUserIdAndDeviceId(
                        user.getId(),
                        deviceId
                );

        if (!hasPermission) {
            throw new RuntimeException("Bạn không có quyền truy cập thiết bị này");
        }

        return repo.findByDeviceId(deviceId);
    }

    // CORE LOGIC
    public void processSchedules() {

        LocalDate today = LocalDate.now();

        LocalTime now = LocalTime.now()
                .withSecond(0)
                .withNano(0);

        for (DeviceSchedule s : repo.findByEnabledTrue()) {

            // kiểm tra lịch có hợp lệ hôm nay không
            if (!isValidDay(s, today)) {
                continue;
            }

            // ================= BẬT =================

            if (s.getStartTime() != null
                    && !today.equals(s.getLastRunStart())
                    && now.equals(
                    s.getStartTime()
                            .withSecond(0)
                            .withNano(0))) {

                runner.turnOn(s.getDeviceId());

                s.setLastRunStart(today);

                // nếu là ONCE và không có giờ tắt
                if ("ONCE".equals(s.getType())
                        && s.getEndTime() == null) {

                    s.setEnabled(false);
                }

                repo.save(s);
            }

            // ================= TẮT =================

            if (s.getEndTime() != null
                    && !today.equals(s.getLastRunEnd())
                    && now.equals(
                    s.getEndTime()
                            .withSecond(0)
                            .withNano(0))) {

                runner.turnOff(s.getDeviceId());

                s.setLastRunEnd(today);

                // lịch chạy 1 lần
                if ("ONCE".equals(s.getType())) {

                    s.setEnabled(false);
                }

                repo.save(s);
            }
        }
    }
    private boolean isValidDay(DeviceSchedule s, LocalDate date) {

        if ("DAILY".equals(s.getType())) return true;

        if ("WEEKLY".equals(s.getType()) && s.getDaysOfWeek() != null) {

            DayOfWeek dow = date.getDayOfWeek();

            return s.getDaysOfWeek()
                    .contains(dow.name().substring(0, 3));
        }

        return true;
    }
}