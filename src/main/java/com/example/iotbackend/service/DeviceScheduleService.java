package com.example.iotbackend.service;

import com.example.iotbackend.dto.DeviceScheduleRequest;
import com.example.iotbackend.entity.Device;
import com.example.iotbackend.entity.DeviceSchedule;
import com.example.iotbackend.entity.User;
import com.example.iotbackend.entity.UserDevice;
import com.example.iotbackend.mqtt.DeviceScheduleRunner;
import com.example.iotbackend.repository.DeviceRepository;
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
    private final DeviceRepository deviceRepository;
    private final DeviceLogService deviceLogService;
    private final NotificationService notificationService;

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

        DeviceSchedule saved = repo.save(s);

        Device device = deviceRepository
                .findById(req.getDeviceId())
                .orElseThrow();

        deviceLogService.saveLog(
                device,
                user,
                null,
                "CREATE_SCHEDULE",
                user.getUsername()
                        + " đã tạo lịch cho "
                        + device.getName(),
                "MOBILE"
        );

        notificationService.sendNotification(
                user,
                user,
                "Tạo lịch thành công",
                "Đã tạo lịch cho "
                        + device.getName()
        );

        return saved;
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
            Device device = deviceRepository
                    .findById(s.getDeviceId())
                    .orElseThrow();
            if (s.getStartTime() != null
                    && !today.equals(s.getLastRunStart())
                    && now.equals(
                    s.getStartTime()
                            .withSecond(0)
                            .withNano(0))) {

                runner.turnOn(s.getDeviceId());


                String msg =
                        "Hệ thống tự động bật "
                                + device.getName();

                deviceLogService.saveLog(
                        device,
                        null,
                        null,
                        "AUTO_TURN_ON",
                        msg,
                        "SCHEDULE"
                );

                List<UserDevice> users =
                        userDeviceRepository.findByDeviceId(device.getId());

                for (UserDevice x : users) {

                    notificationService.sendNotification(
                            x.getUser(),
                            null,
                            "Lịch tự động",
                            msg
                    );
                }

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

                String msg =
                        "Hệ thống tự động tắt "
                                + device.getName();

                deviceLogService.saveLog(
                        device,
                        null,
                        null,
                        "AUTO_TURN_OFF",
                        msg,
                        "SCHEDULE"
                );

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