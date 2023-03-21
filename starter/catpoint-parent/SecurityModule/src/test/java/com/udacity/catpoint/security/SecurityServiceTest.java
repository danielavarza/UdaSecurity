package com.udacity.catpoint.security;

import com.udacity.catpoint.data.*;
import com.udacity.catpoint.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;
    private List<Sensor> sensorList;

    //Mock the objects needed to test the "real" instance of SecurityService
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensorList = List.of(new Sensor(UUID.randomUUID().toString(), SensorType.DOOR),
                new Sensor(UUID.randomUUID().toString(), SensorType.MOTION),
                new Sensor(UUID.randomUUID().toString(), SensorType.WINDOW));
    }

    // Req 1: If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @Test
    void handleSensorActivated_setToPendingStatus() {
        // Arm to ARMED_AWAY or ARMED_HOME
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        //Then the alarm status must have the NO_ALARM state to switch to PENDING_ALARM
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensorList.get(0), true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Req 2: If alarm is armed and a sensor becomes activated and the system is already pending alarm, set off the alarm.
    @Test
    void handleSensorActivated_setToAlarm() {
        // Arm to ARMED_AWAY or ARMED_HOME
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        //Then the alarm status must have the PENDING_ALARM state to switch to ALARM
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensorList.get(0), true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Req 3: If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    void handleSensorDeactivated_pendingAlarmToNoAlarm() {
        // set to PENDING_ALARM state
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        //Sensor is deactivated by default (in C-tor)
        securityService.changeSensorActivationStatus(sensorList.get(0), false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Req 4: If alarm is active, change in sensor state should not affect the alarm state.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void whenAlarmIsActivated_NoRequiredChanges(boolean sensorStatus) {
        // Activate the alarm
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        // Check with both Sensor states to verify the if the method which is responsible to change the alarm state is called
        securityService.changeSensorActivationStatus(sensorList.get(0), sensorStatus);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // Req 5: If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    void sensorActivated_setTheAlarmOn() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        // Activate the sensor (by default is deactivated)
        sensorList.get(0).setActive(true);
        securityService.changeSensorActivationStatus(sensorList.get(0), true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Req 6: If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @Test
    void deactivateSensorWhileActive_NoChangesToAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        // Sensor is inactive by default due to creation after each test case (init() method)
        // De-activated as stated in the requirement,
        sensorList.get(0).setActive(false);
        securityService.changeSensorActivationStatus(sensorList.get(0), false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // Req 7: If the camera image contains a cat while the system is armed-home, put the system into alarm status.
    @Test
    void ifCatIsPresent_startTheAlarm () {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(new BufferedImage(50, 50, 1));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Req 8: If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    void ifNoCatNoSensorsActive_stopTheAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        sensorList.get(0).setActive(false);
        securityService.processImage(new BufferedImage(50, 50, 1));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Req 9: If the system is disarmed, set the status to no alarm.
    @Test
    void systemDisarmed_NoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Req 10: If the system is armed, reset all sensors to inactive.
    @Test
    void systemArmed_resetSensors() {
        // Activate the sensors
        sensorList.forEach(sensor -> sensor.setActive(true));
        when(securityRepository.getSensors()).thenReturn(sensorList.stream().collect(Collectors.toSet()));
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.getSensors().forEach(sensor -> assertFalse(sensor.getActive()));
    }

    // Req 11: If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    void systemArmedHomeAndCatDetected_setAlarmON() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(new BufferedImage(50, 50, 1));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}
