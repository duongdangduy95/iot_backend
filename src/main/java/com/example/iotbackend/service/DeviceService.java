package com.example.iotbackend.service;

import com.example.iotbackend.dto.*;
import com.example.iotbackend.entity.*;
import com.example.iotbackend.repository.*;
import com.example.iotbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    private final DeviceGroupRepository groupRepository;

    private final UserDeviceRepository userDeviceRepository;

    private final UserRepository userRepository;

    private final SecurityUtils securityUtils;

    private final MqttService mqttService;

    //
    // PAIR DEVICE
    //
    public void pairDevice(
            PairDeviceRequest req
    ) {

        User user =
                securityUtils.getCurrentUser();

        //
        // DEVICE EXISTS?
        //
        Device device =
                deviceRepository
                        .findByDeviceCode(
                                req.getDeviceCode()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Device not found"
                                ));

        //
        // ALREADY OWNED?
        //
        boolean owned =
                userDeviceRepository
                        .existsByDeviceId(
                                device.getId()
                        );

        if (owned) {

            throw new RuntimeException(
                    "Device already paired"
            );
        }

        //
        // DISPLAY NAME
        //
        device.setName(req.getName());

        //
        // GROUP
        //
        if (req.getGroupId() != null) {

            DeviceGroup group =
                    groupRepository
                            .findById(
                                    req.getGroupId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Group not found"
                                    ));

            //
            // CHECK GROUP OWNER
            //
            if (!group.getUser()
                    .getId()
                    .equals(user.getId())) {

                throw new RuntimeException(
                        "Không có quyền group"
                );
            }

            device.setGroup(group);
        }

        //
        // SAVE DEVICE
        //
        deviceRepository.save(device);

        //
        // CREATE OWNER
        //
        UserDevice ud =
                new UserDevice();

        ud.setUser(user);

        ud.setDevice(device);

        ud.setRole("OWNER");

        userDeviceRepository.save(ud);
    }

    //
    // GET MY DEVICES
    //
    public List<Device> myDevices() {

        User user =
                securityUtils.getCurrentUser();

        List<UserDevice> list =
                userDeviceRepository
                        .findByUserId(
                                user.getId()
                        );

        return list.stream()
                .map(UserDevice::getDevice)
                .toList();
    }

    public List<DeviceResponse> getMyDevices() {

        User user =
                securityUtils.getCurrentUser();

        List<UserDevice> userDevices =
                userDeviceRepository.findByUserId(
                        user.getId()
                );

        return userDevices.stream()
                .map(UserDevice::getDevice)
                .map(device -> {

                    DeviceResponse res =
                            new DeviceResponse();

                    res.setId(device.getId());
                    res.setDeviceCode(device.getDeviceCode());
                    res.setName(device.getName());
                    res.setType(device.getType());
                    res.setStatus(device.getStatus());
                    res.setOnline(device.getOnline());

                    return res;
                })
                .toList();
    }

    public void shareDevice(
            ShareDeviceRequest req
    ) {

        User currentUser =
                securityUtils.getCurrentUser();

        //
        // FIND DEVICE
        //
        Device device =
                deviceRepository
                        .findById(req.getDeviceId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Device not found"
                                ));

        //
        // CHỈ OWNER ĐƯỢC SHARE
        //
        if (!device.getOwner()
                .getId()
                .equals(currentUser.getId())) {

            throw new RuntimeException(
                    "Không có quyền share"
            );
        }

        //
        // FIND USER BY EMAIL
        //
        User guestUser =
                userRepository
                        .findByEmail(req.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                ));

        //
        // ĐÃ ĐƯỢC SHARE?
        //
        boolean exists =
                userDeviceRepository
                        .existsByUserIdAndDeviceId(
                                guestUser.getId(),
                                device.getId()
                        );

        if (exists) {

            throw new RuntimeException(
                    "User already has device"
            );
        }

        //
        // CREATE GUEST
        //
        UserDevice ud =
                new UserDevice();

        ud.setUser(guestUser);

        ud.setDevice(device);

        ud.setRole("GUEST");

        userDeviceRepository.save(ud);
    }

    public void controlDevice(
            ControlDeviceRequest req
    ) {

        User user =
                securityUtils.getCurrentUser();

        //
        // CHECK PERMISSION
        //
        UserDevice ud =
                userDeviceRepository
                        .findByUserIdAndDeviceId(
                                user.getId(),
                                req.getDeviceId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không có quyền"
                                ));

        Device device =
                ud.getDevice();

        //
        // MQTT TOPIC
        //
        String topic =
                "devices/"
                        + device.getDeviceCode()
                        + "/set";

        //
        // PAYLOAD
        //
        String payload =
                """
                {
                  "state": %s
                }
                """.formatted(req.getState());

        //
        // PUBLISH MQTT
        //
        mqttService.publish(
                topic,
                payload
        );

        //
        // UPDATE DB
        //
        device.setStatus(
                req.getState().toString()
        );

        deviceRepository.save(device);
    }

    public List<DeviceGuestResponse> getGuests(
            Long deviceId
    ) {

        User currentUser =
                securityUtils.getCurrentUser();

        //
        // FIND DEVICE
        //
        Device device =
                deviceRepository
                        .findById(deviceId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Device not found"
                                ));

        //
        // ONLY OWNER
        //
        if (!device.getOwner()
                .getId()
                .equals(currentUser.getId())) {

            throw new RuntimeException(
                    "Không có quyền"
            );
        }

        //
        // GET USERS
        //
        List<UserDevice> list =
                userDeviceRepository
                        .findByDeviceId(deviceId);

        //
        // REMOVE OWNER
        //
        return list.stream()

                .filter(x ->
                        !x.getRole()
                                .equals("OWNER")
                )

                .map(x ->
                        new DeviceGuestResponse(
                                x.getUser().getId(),
                                x.getUser().getUsername(),
                                x.getUser().getEmail(),
                                x.getRole()
                        )
                )

                .toList();
    }
     @Transactional
    public void removeGuest(
            Long deviceId,
            Long guestUserId
    ) {

        User currentUser =
                securityUtils.getCurrentUser();

        //
        // FIND DEVICE
        //
        Device device =
                deviceRepository
                        .findById(deviceId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Device not found"
                                ));

        //
        // ONLY OWNER
        //
        if (!device.getOwner()
                .getId()
                .equals(currentUser.getId())) {

            throw new RuntimeException(
                    "Không có quyền"
            );
        }

        //
        // FIND USER DEVICE
        //
        UserDevice ud =
                userDeviceRepository
                        .findByUserIdAndDeviceId(
                                guestUserId,
                                deviceId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Guest not found"
                                ));

        //
        // KHÔNG CHO XÓA OWNER
        //
        if (ud.getRole().equals("OWNER")) {

            throw new RuntimeException(
                    "Không thể xóa owner"
            );
        }

        //
        // DELETE
        //
        userDeviceRepository
                .deleteByUserIdAndDeviceId(
                        guestUserId,
                        deviceId
                );
    }

    public void renameDevice(
            RenameDeviceRequest req
    ) {

        User currentUser =
                securityUtils.getCurrentUser();

        Device device =
                deviceRepository
                        .findById(
                                req.getDeviceId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Device not found"
                                ));

        //
        // ONLY OWNER
        //
        if (!device.getOwner()
                .getId()
                .equals(currentUser.getId())) {

            throw new RuntimeException(
                    "Không có quyền đổi tên thiết bị"
            );
        }

        device.setName(
                req.getName()
        );

        deviceRepository.save(
                device
        );
    }
}