package com.udacity.catpoint.security;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.security.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;
    private Sensor doorSensor;
    private Sensor windowSensor;

    private SecurityRepository securityRepository;

    private  ImageService imageService;

    private BufferedImage bufferedImage;

    public void init() {
        securityRepository = mock(SecurityRepository.class);
        imageService = mock(ImageService.class);
        bufferedImage = mock(BufferedImage.class);
        securityService = new SecurityService(securityRepository, imageService);

        doorSensor = new Sensor("Door Sensor", SensorType.DOOR);
        windowSensor = new Sensor("Window Sensor", SensorType.WINDOW);
    }

    //    1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @Test
    public void test_armed_sensorActivated_setToPendingAlarm() {
        init();
        // current arming status to be armed home, and alarm status to be no alarm
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        // activate the sensor
        securityService.changeSensorActivationStatus(doorSensor,true);

        // alarm status should be checked twice, once to confirm it's not already in alarm state,
        // then to check the current alarm state to modify while activating or deactivating
        verify(securityRepository, times(3)).getAlarmStatus();
        // arming status should be checked once to ensure it is not disarmed
        verify(securityRepository, times(1)).getArmingStatus();
        // alarm status should be set to pending alarm but never to alarm or no alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        // sensor status should be updated to active
        assertTrue(doorSensor.getActive());
        // sensor status should be updated in the repository
        verify(securityRepository, times(1)).updateSensor(doorSensor);
    }

    //    2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @Test
    public void test_armed_sensorActivated_inPendingAlarm_setToAlarm() {
        init();
        // current arming status to be armed home, and alarm status to be pending alarm
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // activate the sensor
        securityService.changeSensorActivationStatus(doorSensor, true);

        // alarm status should be checked twice, once to confirm it's not already in alarm state,
        // then to check the current alarm state to modify while activating or deactivating
        verify(securityRepository, times(3)).getAlarmStatus();
        // arming status should be checked once to ensure it is not disarmed
        verify(securityRepository, times(1)).getArmingStatus();
        // alarm status should be set to alarm but never to pending alarm or no alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        // sensor status should be updated to active
        assertTrue(doorSensor.getActive());
        // sensor status should be updated in the repository
        verify(securityRepository, times(1)).updateSensor(doorSensor);
    }

    //    3. If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    public void test_pendingAlarm_allSensorsInactive_setToNoAlarmState() {
        init();
        // activate all sensors, current alarm status to be pending alarm
        doorSensor.setActive(true);
        windowSensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // deactivate door sensor
        securityService.changeSensorActivationStatus(doorSensor, false);

        // deactivate window sensor
        securityService.changeSensorActivationStatus(windowSensor, false);

        // alarm status should be checked 4 times, twice (once for each sensor) to confirm it's not already in alarm state,
        // then twice (once for each sensor) to check the current alarm state to modify while activating or deactivating
        verify(securityRepository, times(4)).getAlarmStatus();
        // arming status should never be checked as deactivating
        verify(securityRepository, never()).getArmingStatus();
        // alarm status should be set to no alarm twice (once for each sensor)
        verify(securityRepository, times(2)).setAlarmStatus(AlarmStatus.NO_ALARM);
        // door sensor status should be updated to inactive
        assertFalse(doorSensor.getActive());
        // door sensor status should be updated in the repository
        verify(securityRepository, times(1)).updateSensor(doorSensor);
        // window sensor status should be updated to inactive
        assertFalse(windowSensor.getActive());
        // window sensor status should be updated in the repository
        verify(securityRepository, times(1)).updateSensor(windowSensor);
    }

    //    4. If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    public void test_alarmActive_changeSensorState_alarmStateNotAffected() {
        init();
        // activate door sensor, alarm status to be alarm
        doorSensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // deactivate door sensor
        securityService.changeSensorActivationStatus(doorSensor, false);

        // alarm status to be checked once to confirm it is in alarm state
        verify(securityRepository, times(1)).getAlarmStatus();
        // arming status should never be checked as it is in alarm state
        verify(securityRepository, never()).getArmingStatus();
        // alarm status should never be changed
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
        // door sensor status should be updated to inactive
        assertFalse(doorSensor.getActive());
        // door sensor status should be updated in the repository
        verify(securityRepository, times(1)).updateSensor(doorSensor);
    }

    //    5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    public void test_activateAlreadyActiveSensor_inPendingAlarm_setToAlarm() {
        init();
        // activate door sensor, alarm status to be pending alarm
        doorSensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // re-activate door sensor
        securityService.changeSensorActivationStatus(doorSensor, true);

        // alarm status should be checked twice, once to confirm it's not already in alarm state,
        // then to check the current alarm state to modify while activating or deactivating
        verify(securityRepository, times(3)).getAlarmStatus();
        // arming status should be checked once to ensure it is not disarmed
        verify(securityRepository, times(1)).getArmingStatus();
        // alarm status should not be affected
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
        // door sensor status should remain in active state
        assertTrue(doorSensor.getActive());
        // door sensor status should be updated in the repository
        verify(securityRepository, times(1)).updateSensor(doorSensor);
    }

    //    6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    public void test_deactivateAlreadyInactiveSensor_alarmStateNotAffected(AlarmStatus alarmStatus) {
        init();
        // set to one of the alarm statuses
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);

        // by default the sensor is inactive, deactivate door sensor
        securityService.changeSensorActivationStatus(doorSensor, false);

        // get alarm status should be called once
        verify(securityRepository, times(1)).getAlarmStatus();
        // get arming status should not be called
        verify(securityRepository, never()).getArmingStatus();
        // alarm status should never change
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
        // door sensor status should remain in inactive state
        assertFalse(doorSensor.getActive());
        // door sensor status should be updated in the repository
        verify(securityRepository, times(1)).updateSensor(doorSensor);
    }

    //    7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @Test
    public void test_armedHome_catDetected_setToAlarm() {
        init();
        // set armed status to armed home, make cat detected true
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        // process the image
        securityService.processImage(bufferedImage);

        // image service should be called once to check if it contains cat
        verify(imageService,times(1)).imageContainsCat(any(BufferedImage.class), anyFloat());
        // arming status should be checked once to ensure it is in armed home state
        verify(securityRepository, times(1)).getArmingStatus();
        // sensor status check should not happen as the cat is detected
        verify(securityRepository, never()).getSensors();
        // alarm status should be set to alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void test_armedAway_catDetected_alarmNotAffected() {
        init();
        // set armed status to armed away, make cat detected true
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        // process the image
        securityService.processImage(bufferedImage);

        // image service should be called once to check if the image contains cat
        verify(imageService,times(1)).imageContainsCat(any(BufferedImage.class), anyFloat());
        // arming status should be checked once to ensure it is in armed home state
        verify(securityRepository, times(1)).getArmingStatus();
        // sensor status check should not happen as the cat is detected
        verify(securityRepository, never()).getSensors();
        // alarm status should not be affected as it is in armed away state
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //    8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    public void test_catNotDetected_sensorsNotActive_setToNoAlarm() {
        init();
        // make cat detected false
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

        // process the image
        securityService.processImage(bufferedImage);

        // image service should be called once to check if the image contains cat
        verify(imageService,times(1)).imageContainsCat(any(BufferedImage.class), anyFloat());
        // arming status should never be checked as cat is not detected
        verify(securityRepository, never()).getArmingStatus();
        // no cat detected now, but all the sensors should be checked once to ensure none of them are active
        verify(securityRepository, times(1)).getSensors();
        // alarm status should be set to no alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void test_catNotDetected_atLeastOneSensorActive_alarmStateNotAffected() {
        init();
        // make cat detected false, activate one of the sensors
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        doorSensor.setActive(false);
        windowSensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn(Set.of(doorSensor, windowSensor));

        // process the image
        securityService.processImage(bufferedImage);

        // image service should be called once to check if the image contains cat
        verify(imageService,times(1)).imageContainsCat(any(BufferedImage.class), anyFloat());
        // arming status should never be checked as cat is not detected
        verify(securityRepository, never()).getArmingStatus();
        // no cat detected now, but all the sensors should be checked once to ensure none of them are active
        verify(securityRepository, times(1)).getSensors();
        // alarm status should not be affected as one of the sensors are active
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //    9. If the system is disarmed, set the status to no alarm.
    @Test
    public void test_disarm_setToNoAlarm() {
        init();
        // make the system disarmed
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        // arming status should be set to disarmed
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.DISARMED);
        // current arming status should not be checked
        verify(securityRepository, never()).getArmingStatus();
        // current alarm status should never be checked
        verify(securityRepository, never()).getAlarmStatus();
        // no sensors should be updated
        verify(securityRepository, never()).updateSensor(any(Sensor.class));
        // alarm status should be set to no alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(ArmingStatus.class)
    public void test_fromDifferentArmedStatus_disarm_setToNoAlarm(ArmingStatus armingStatus) {
        init();
        // set current arming status (to ensure current arming status doesn't affect the behavior)
        lenient().when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // make the system disarmed
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        // arming status should be set to disarmed
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.DISARMED);
        // current arming status should not be checked
        verify(securityRepository, never()).getArmingStatus();
        // current alarm status should never be checked
        verify(securityRepository, never()).getAlarmStatus();
        // no sensors should be updated
        verify(securityRepository, never()).updateSensor(any(Sensor.class));
        // alarm status should be set to no alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    public void test_withDifferentAlarmStatus_disarm_setToNoAlarm(AlarmStatus alarmStatus) {
        init();
        // set current alarm status (to ensure current alarm status doesn't affect the behavior)
        lenient().when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);

        // make the system disarmed
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        // arming status should be set to disarmed
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.DISARMED);
        // current arming status should not be checked
        verify(securityRepository, never()).getArmingStatus();
        // current alarm status should never be checked
        verify(securityRepository, never()).getAlarmStatus();
        // no sensors should be updated
        verify(securityRepository, never()).updateSensor(any(Sensor.class));
        // alarm status should be set to no alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //    10. If the system is armed, reset all sensors to inactive.
    @Test
    public void test_setArmedHome_setAllSensorsToInactive() {
        init();
        // current arming status: disarmed, all sensors active
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getSensors()).thenReturn(Set.of(doorSensor, windowSensor));
        securityRepository.getSensors().forEach(sensor -> sensor.setActive(true));

        // set arming status to armed home
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // arming status should be set to armed home
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_HOME);
        // all sensors should be updated
        verify(securityRepository, times(securityRepository.getSensors().size())).updateSensor(any(Sensor.class));
        // all sensors should be inactive
        assertFalse(securityRepository.getSensors().stream().anyMatch(Sensor::getActive));
        // alarm status should not be affected
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    public void test_setArmedAway_setAllSensorsToInactive() {
        init();
        // current arming status: disarmed, all sensors active
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getSensors()).thenReturn(Set.of(doorSensor, windowSensor));
        securityRepository.getSensors().forEach(sensor -> sensor.setActive(true));

        // set arming status to armed away
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        // arming status should be set to armed away
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_AWAY);
        // all sensors should be updated
        verify(securityRepository, times(securityRepository.getSensors().size())).updateSensor(any(Sensor.class));
        // all sensors should be inactive
        assertFalse(securityRepository.getSensors().stream().anyMatch(Sensor::getActive));
        // alarm status should not be affected
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    public void test_setArmedAway_oneSensorActive_setAllSensorsToInactive() {
        init();
        // current arming status: disarmed, door sensor active, window sensor inactive
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getSensors()).thenReturn(Set.of(doorSensor, windowSensor));
        doorSensor.setActive(false);
        windowSensor.setActive(true);

        // set arming status to armed away
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        // arming status should be set to armed away
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_AWAY);
        // all sensors should be updated
        verify(securityRepository, times(securityRepository.getSensors().size())).updateSensor(any(Sensor.class));
        // all sensors should be inactive
        assertFalse(securityRepository.getSensors().stream().anyMatch(Sensor::getActive));
        // alarm status should not be affected
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //    11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void test_setArmedHome_catDetected_setToAlarm() {
        init();
        // current arming status: disarmed, camera shows cat
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);

        // set arming status to armed home
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // arming status should be set to armed home
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_HOME);
        // all sensors should be updated
        verify(securityRepository, times(securityRepository.getSensors().size())).updateSensor(any(Sensor.class));
        // alarm status should be set to alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void test_setArmedAway_catDetected_alarmNotAffected() {
        init();
        // current arming status: disarmed, camera shows cat
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);

        // set arming status to armed away
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        // arming status should be set to armed away
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_AWAY);
        // all sensors should be updated
        verify(securityRepository, times(securityRepository.getSensors().size())).updateSensor(any(Sensor.class));
        // alarm status should not change
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void test_setArmedHomeFromAway_catDetected_setToAlarm() {
        init();
        // current arming status: armed away, camera shows cat
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);

        // set arming status to armed home
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // arming status should be set to armed home
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_HOME);
        // all sensors should be updated
        verify(securityRepository, times(securityRepository.getSensors().size())).updateSensor(any(Sensor.class));
        // alarm status should be set to alarm
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}