/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.appspot.apprtc;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
// import org.appspot.apprtc.util.AppRTCUtils;
import org.webrtc.ThreadUtils;

/**
 * AppRTCAudioManager manages all audio related parts of the AppRTC demo.
 */
public class AppRTCAudioManager {
  private static final String TAG = "AppRTCAudioManager";
  private static final String SPEAKERPHONE_AUTO = "auto";
  private static final String SPEAKERPHONE_TRUE = "true";
  private static final String SPEAKERPHONE_FALSE = "false";

  /**
   * AudioDevice is the names of possible audio devices that we currently
   * support.
   */
  public enum AudioDevice { SPEAKER_PHONE, WIRED_HEADSET, EARPIECE, BLUETOOTH, NONE }

  /** AudioManager state. */
  public enum AudioManagerState {
    UNINITIALIZED,
    PREINITIALIZED,
    RUNNING,
  }

  /** Selected audio device change event. */
  public interface AudioManagerEvents {
    // Callback fired once audio device is changed or list of available audio devices changed.
    void onAudioDeviceChanged(
        AudioDevice selectedAudioDevice, Set<AudioDevice> availableAudioDevices);
  }

  private final Context apprtcContext;
  @Nullable
  private AudioManager audioManager;

  @Nullable
  private AudioManagerEvents audioManagerEvents;
  private AudioManagerState amState;
  private int savedAudioMode = AudioManager.MODE_INVALID;
  private boolean savedIsSpeakerPhoneOn;
  private boolean savedIsMicrophoneMute;
  private boolean hasWiredHeadset;

  // Default audio device; speaker phone for video calls or earpiece for audio
  // only calls.
  private AudioDevice defaultAudioDevice;

  // Contains the currently selected audio device.
  // This device is changed automatically using a certain scheme where e.g.
  // a wired headset "wins" over speaker phone. It is also possible for a
  // user to explicitly select a device (and overrid any predefined scheme).
  // See |userSelectedAudioDevice| for details.
  private AudioDevice selectedAudioDevice;

  // Contains the user-selected audio device which overrides the predefined
  // selection scheme.
  // TODO(henrika): always set to AudioDevice.NONE today. Add support for
  // explicit selection based on choice by userSelectedAudioDevice.
  private AudioDevice userSelectedAudioDevice;

  // Contains speakerphone setting: auto, true or false
  private final String useSpeakerphone;

  // Proximity sensor object. It measures the proximity of an object in cm
  // relative to the view screen of a device and can therefore be used to
  // assist device switching (close to ear <=> use headset earpiece if
  // available, far from ear <=> use speaker phone).
  // @Nullable private AppRTCProximitySensor proximitySensor;

  // Handles all tasks related to Bluetooth headset devices.
  private final AppRTCBluetoothManager bluetoothManager;

  // Contains a list of available audio devices. A Set collection is used to
  // avoid duplicate elements.
  private Set<AudioDevice> audioDevices = new HashSet<>();

  // Broadcast receiver for wired headset intent broadcasts.
  private BroadcastReceiver wiredHeadsetReceiver;

  // Callback method for changes in audio focus.
  @Nullable
  private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

  /**
   * This method is called when the proximity sensor reports a state change,
   * e.g. from "NEAR to FAR" or from "FAR to NEAR".
   */
  private void onProximitySensorChangedState() {
    if (!useSpeakerphone.equals(SPEAKERPHONE_AUTO)) {
      return;
    }

    // The proximity sensor should only be activated when there are exactly two
    // available audio devices.
    if (audioDevices.size() == 2 && audioDevices.contains(AppRTCAudioManager.AudioDevice.EARPIECE)
        && audioDevices.contains(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)) {
//      if (proximitySensor.sensorReportsNearState()) {
        // Sensor reports that a "handset is being held up to a person's ear",
        // or "something is covering the light sensor".
//        setAudioDeviceInternal(AppRTCAudioManager.AudioDevice.EARPIECE);
//      } else {
        // Sensor reports that a "handset is removed from a person's ear", or
        // "the light sensor is no longer covered".
        setAudioDeviceInternal(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
//      }
    }
  }

  /* Receiver which handles changes in wired headset availability. */
  private class WiredHeadsetReceiver extends BroadcastReceiver {
    private static final int STATE_UNPLUGGED = 0;
    private static final int STATE_PLUGGED = 1;
    private static final int HAS_NO_MIC = 0;
    private static final int HAS_MIC = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
      int state = intent.getIntExtra("state", STATE_UNPLUGGED);
      int microphone = intent.getIntExtra("microphone", HAS_NO_MIC);
      String name = intent.getStringExtra("name");
      Log.d(TAG, "WiredHeadsetReceiver.onReceive" + ": "
              + "a=" + intent.getAction() + ", s="
              + (state == STATE_UNPLUGGED ? "unplugged" : "plugged") + ", m="
              + (microphone == HAS_MIC ? "mic" : "no mic") + ", n=" + name + ", sb="
              + isInitialStickyBroadcast());
      hasWiredHeadset = (state == STATE_PLUGGED);
      updateAudioDeviceState();
    }
  }

  /** Construction. */
  public static AppRTCAudioManager create(Context context) {
    return new AppRTCAudioManager(context);
  }

  private AppRTCAudioManager(Context context) {
    Log.d(TAG, "ctor");
    ThreadUtils.checkIsOnMainThread();
    apprtcContext = context;
    audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
    bluetoothManager = AppRTCBluetoothManager.create(context, this);
    wiredHeadsetReceiver = new WiredHeadsetReceiver();
    amState = AudioManagerState.UNINITIALIZED;

    useSpeakerphone = SPEAKERPHONE_AUTO;

    Log.d(TAG, "useSpeakerphone: " + useSpeakerphone);
//    if (useSpeakerphone.equals(SPEAKERPHONE_FALSE)) {
//      defaultAudioDevice = AudioDevice.EARPIECE;
//    } else {
      defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
//    }

    // Create and initialize the proximity sensor.
    // Tablet devices (e.g. Nexus 7) does not support proximity sensors.
    // Note that, the sensor will not be active until start() has been called.
//    proximitySensor = AppRTCProximitySensor.create(context,
//        // This method will be called each time a state change is detected.
//        // Example: user holds his hand over the device (closer than ~5 cm),
//        // or removes his hand from the device.
//        this ::onProximitySensorChangedState);

    Log.d(TAG, "defaultAudioDevice: " + defaultAudioDevice);
//    AppRTCUtils.logDeviceInfo(TAG);
  }

  @SuppressWarnings("deprecation") // TODO(henrika): audioManager.requestAudioFocus() is deprecated.
  public void start(AudioManagerEvents audioManagerEvents) {
    Log.d(TAG, "start");
    ThreadUtils.checkIsOnMainThread();
    if (amState == AudioManagerState.RUNNING) {
      Log.e(TAG, "AudioManager is already active");
      return;
    }
    // TODO(henrika): perhaps call new method called preInitAudio() here if UNINITIALIZED.

    Log.d(TAG, "AudioManager starts...");
    this.audioManagerEvents = audioManagerEvents;
    amState = AudioManagerState.RUNNING;

    // Store current audio state so we can restore it when stop() is called.
    savedAudioMode = audioManager.getMode();
    savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
    savedIsMicrophoneMute = audioManager.isMicrophoneMute();
    hasWiredHeadset = hasWiredHeadset();

    // Create an AudioManager.OnAudioFocusChangeListener instance.
    audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
      // Called on the listener to notify if the audio focus for this listener has been changed.
      // The |focusChange| value indicates whether the focus was gained, whether the focus was lost,
      // and whether that loss is transient, or whether the new focus holder will hold it for an
      // unknown amount of time.
      // TODO(henrika): possibly extend support of handling audio-focus changes. Only contains
      // logging for now.
      @Override
      public void onAudioFocusChange(int focusChange) {
        final String typeOfChange;
        switch (focusChange) {
          case AudioManager.AUDIOFOCUS_GAIN:
            typeOfChange = "AUDIOFOCUS_GAIN";
            break;
          case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
            typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT";
            break;
          case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
            typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE";
            break;
          case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
            typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK";
            break;
          case AudioManager.AUDIOFOCUS_LOSS:
            typeOfChange = "AUDIOFOCUS_LOSS";
            break;
          case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            typeOfChange = "AUDIOFOCUS_LOSS_TRANSIENT";
            break;
          case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            typeOfChange = "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK";
            break;
          default:
            typeOfChange = "AUDIOFOCUS_INVALID";
            break;
        }
        Log.d(TAG, "onAudioFocusChange: " + typeOfChange);
      }
    };

    // Request audio playout focus (without ducking) and install listener for changes in focus.
    int result = audioManager.requestAudioFocus(audioFocusChangeListener,
        AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      Log.d(TAG, "Audio focus request granted for VOICE_CALL streams");
    } else {
      Log.e(TAG, "Audio focus request failed");
    }

    // Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
    // required to be in this mode when playout and/or recording starts for
    // best possible VoIP performance.
    // audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    // Using MODE_IN_COMMUNICATION, a mic of a bluetooth headset does not
    // get active when android is upstream with no other upstreams.
    // Maybe strange pattern but one of use cases with SFU.
    // Use MODE_IN_CALL instead.
    audioManager.setMode(AudioManager.MODE_IN_CALL);

    // Always disable microphone mute during a WebRTC call.
    setMicrophoneMute(false);

    // Set initial device states.
    userSelectedAudioDevice = AudioDevice.NONE;
    selectedAudioDevice = AudioDevice.NONE;
    audioDevices.clear();

    // Initialize and start Bluetooth if a BT device is available or initiate
    // detection of new (enabled) BT devices.
    bluetoothManager.start();

    // Do initial selection of audio device. This setting can later be changed
    // either by adding/removing a BT or wired headset or by covering/uncovering
    // the proximity sensor.
    updateAudioDeviceState();

    // Register receiver for broadcast intents related to adding/removing a
    // wired headset.
    registerReceiver(wiredHeadsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    Log.d(TAG, "AudioManager started");
  }

  @SuppressWarnings("deprecation") // TODO(henrika): audioManager.abandonAudioFocus() is deprecated.
  public void stop() {
    Log.d(TAG, "stop");
    ThreadUtils.checkIsOnMainThread();
    if (amState != AudioManagerState.RUNNING) {
      Log.e(TAG, "Trying to stop AudioManager in incorrect state: " + amState);
      return;
    }
    amState = AudioManagerState.UNINITIALIZED;

    unregisterReceiver(wiredHeadsetReceiver);

    bluetoothManager.stop();

    // Restore previously stored audio states.
    setSpeakerphoneOn(savedIsSpeakerPhoneOn);
    setMicrophoneMute(savedIsMicrophoneMute);
    audioManager.setMode(savedAudioMode);

    // Abandon audio focus. Gives the previous focus owner, if any, focus.
    audioManager.abandonAudioFocus(audioFocusChangeListener);
    audioFocusChangeListener = null;
    Log.d(TAG, "Abandoned audio focus for VOICE_CALL streams");

//    if (proximitySensor != null) {
//      proximitySensor.stop();
//      proximitySensor = null;
//    }

    audioManagerEvents = null;
    Log.d(TAG, "AudioManager stopped");
  }

  /** Changes selection of the currently active audio device. */
  private void setAudioDeviceInternal(AudioDevice device) {
    Log.d(TAG, "setAudioDeviceInternal(device=" + device + ")");
    // AppRTCUtils.assertIsTrue(audioDevices.contains(device));

    switch (device) {
      case SPEAKER_PHONE:
        setSpeakerphoneOn(true);
        break;
      case EARPIECE:
        setSpeakerphoneOn(false);
        break;
      case WIRED_HEADSET:
        setSpeakerphoneOn(false);
        break;
      case BLUETOOTH:
        setSpeakerphoneOn(false);
        break;
      default:
        Log.e(TAG, "Invalid audio device selection");
        break;
    }
    selectedAudioDevice = device;
  }

  /**
   * Changes default audio device.
   * TODO(henrika): add usage of this method in the AppRTCMobile client.
   */
  public void setDefaultAudioDevice(AudioDevice defaultDevice) {
    ThreadUtils.checkIsOnMainThread();
    switch (defaultDevice) {
      case SPEAKER_PHONE:
        defaultAudioDevice = defaultDevice;
        break;
      case EARPIECE:
        if (hasEarpiece()) {
          defaultAudioDevice = defaultDevice;
        } else {
          defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
        }
        break;
      default:
        Log.e(TAG, "Invalid default audio device selection");
        break;
    }
    Log.d(TAG, "setDefaultAudioDevice(device=" + defaultAudioDevice + ")");
    updateAudioDeviceState();
  }

  /** Changes selection of the currently active audio device. */
  public void selectAudioDevice(AudioDevice device) {
    ThreadUtils.checkIsOnMainThread();
    if (!audioDevices.contains(device)) {
      Log.e(TAG, "Can not select " + device + " from available " + audioDevices);
    }
    userSelectedAudioDevice = device;
    updateAudioDeviceState();
  }

  /** Returns current set of available/selectable audio devices. */
  public Set<AudioDevice> getAudioDevices() {
    ThreadUtils.checkIsOnMainThread();
    return Collections.unmodifiableSet(new HashSet<>(audioDevices));
  }

  /** Returns the currently selected audio device. */
  public AudioDevice getSelectedAudioDevice() {
    ThreadUtils.checkIsOnMainThread();
    return selectedAudioDevice;
  }

  /** Helper method for receiver registration. */
  private void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
    apprtcContext.registerReceiver(receiver, filter);
  }

  /** Helper method for unregistration of an existing receiver. */
  private void unregisterReceiver(BroadcastReceiver receiver) {
    apprtcContext.unregisterReceiver(receiver);
  }

  /** Sets the speaker phone mode. */
  private void setSpeakerphoneOn(boolean on) {
    boolean wasOn = audioManager.isSpeakerphoneOn();
    if (wasOn == on) {
      return;
    }
    audioManager.setSpeakerphoneOn(on);
  }

  /** Sets the microphone mute state. */
  private void setMicrophoneMute(boolean on) {
    boolean wasMuted = audioManager.isMicrophoneMute();
    if (wasMuted == on) {
      return;
    }
    audioManager.setMicrophoneMute(on);
  }

  /** Gets the current earpiece state. */
  private boolean hasEarpiece() {
    return apprtcContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
  }

  /**
   * Checks whether a wired headset is connected or not.
   * This is not a valid indication that audio playback is actually over
   * the wired headset as audio routing depends on other conditions. We
   * only use it as an early indicator (during initialization) of an attached
   * wired headset.
   */
  @Deprecated
  private boolean hasWiredHeadset() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return audioManager.isWiredHeadsetOn();
    } else {
      final AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
      for (AudioDeviceInfo device : devices) {
        final int type = device.getType();
        if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
          Log.d(TAG, "hasWiredHeadset: found wired headset");
          return true;
        } else if (type == AudioDeviceInfo.TYPE_USB_DEVICE) {
          Log.d(TAG, "hasWiredHeadset: found USB audio device");
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Updates list of possible audio devices and make new device selection.
   * TODO(henrika): add unit test to verify all state transitions.
   */
  public void updateAudioDeviceState() {
    ThreadUtils.checkIsOnMainThread();
    Log.d(TAG, "--- updateAudioDeviceState: "
            + "wired headset=" + hasWiredHeadset + ", "
            + "BT state=" + bluetoothManager.getState());
    Log.d(TAG, "Device status: "
            + "available=" + audioDevices + ", "
            + "selected=" + selectedAudioDevice + ", "
            + "user selected=" + userSelectedAudioDevice);

    // Check if any Bluetooth headset is connected. The internal BT state will
    // change accordingly.
    // TODO(henrika): perhaps wrap required state into BT manager.
    if (bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_UNAVAILABLE
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_DISCONNECTING) {
      bluetoothManager.updateDevice();
    }

    // Update the set of available audio devices.
    Set<AudioDevice> newAudioDevices = new HashSet<>();

    if (bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTING
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE) {
      newAudioDevices.add(AudioDevice.BLUETOOTH);
    }

    if (hasWiredHeadset) {
      // If a wired headset is connected, then it is the only possible option.
      newAudioDevices.add(AudioDevice.WIRED_HEADSET);
    } else {
      // No wired headset, hence the audio-device list can contain speaker
      // phone (on a tablet), or speaker phone and earpiece (on mobile phone).
      newAudioDevices.add(AudioDevice.SPEAKER_PHONE);
      if (hasEarpiece()) {
        newAudioDevices.add(AudioDevice.EARPIECE);
      }
    }
    // Store state which is set to true if the device list has changed.
    boolean audioDeviceSetUpdated = !audioDevices.equals(newAudioDevices);
    // Update the existing audio device set.
    audioDevices = newAudioDevices;
    // Correct user selected audio devices if needed.
    if (bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_UNAVAILABLE
        && userSelectedAudioDevice == AudioDevice.BLUETOOTH) {
      // If BT is not available, it can't be the user selection.
      userSelectedAudioDevice = AudioDevice.NONE;
    }
    if (hasWiredHeadset && userSelectedAudioDevice == AudioDevice.SPEAKER_PHONE) {
      // If user selected speaker phone, but then plugged wired headset then make
      // wired headset as user selected device.
      userSelectedAudioDevice = AudioDevice.WIRED_HEADSET;
    }
    if (!hasWiredHeadset && userSelectedAudioDevice == AudioDevice.WIRED_HEADSET) {
      // If user selected wired headset, but then unplugged wired headset then make
      // speaker phone as user selected device.
      userSelectedAudioDevice = AudioDevice.SPEAKER_PHONE;
    }

    // Need to start Bluetooth if it is available and user either selected it explicitly or
    // user did not select any output device.
    boolean needBluetoothAudioStart =
        bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
        && (userSelectedAudioDevice == AudioDevice.NONE
               || userSelectedAudioDevice == AudioDevice.BLUETOOTH);

    // Need to stop Bluetooth audio if user selected different device and
    // Bluetooth SCO connection is established or in the process.
    boolean needBluetoothAudioStop =
        (bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED
            || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTING)
        && (userSelectedAudioDevice != AudioDevice.NONE
               && userSelectedAudioDevice != AudioDevice.BLUETOOTH);

    if (bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTING
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED) {
      Log.d(TAG, "Need BT audio: start=" + needBluetoothAudioStart + ", "
              + "stop=" + needBluetoothAudioStop + ", "
              + "BT state=" + bluetoothManager.getState());
    }

    // Start or stop Bluetooth SCO connection given states set earlier.
    if (needBluetoothAudioStop) {
      bluetoothManager.stopScoAudio();
      bluetoothManager.updateDevice();
    }

    if (needBluetoothAudioStart && !needBluetoothAudioStop) {
      // Attempt to start Bluetooth SCO audio (takes a few second to start).
      if (!bluetoothManager.startScoAudio()) {
        // Remove BLUETOOTH from list of available devices since SCO failed.
        audioDevices.remove(AudioDevice.BLUETOOTH);
        audioDeviceSetUpdated = true;
      }
    }

    // Update selected audio device.
    final AudioDevice newAudioDevice;

    if (bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED) {
      // If a Bluetooth is connected, then it should be used as output audio
      // device. Note that it is not sufficient that a headset is available;
      // an active SCO channel must also be up and running.
      newAudioDevice = AudioDevice.BLUETOOTH;
    } else if (hasWiredHeadset) {
      // If a wired headset is connected, but Bluetooth is not, then wired headset is used as
      // audio device.
      newAudioDevice = AudioDevice.WIRED_HEADSET;
    } else {
      // No wired headset and no Bluetooth, hence the audio-device list can contain speaker
      // phone (on a tablet), or speaker phone and earpiece (on mobile phone).
      // |defaultAudioDevice| contains either AudioDevice.SPEAKER_PHONE or AudioDevice.EARPIECE
      // depending on the user's selection.
      newAudioDevice = defaultAudioDevice;
    }
    // Switch to new device but only if there has been any changes.
    if (newAudioDevice != selectedAudioDevice || audioDeviceSetUpdated) {
      // Do the required device switch.
      setAudioDeviceInternal(newAudioDevice);
      Log.d(TAG, "New device status: "
              + "available=" + audioDevices + ", "
              + "selected=" + newAudioDevice);
      if (audioManagerEvents != null) {
        // Notify a listening client that audio device has been changed.
        audioManagerEvents.onAudioDeviceChanged(selectedAudioDevice, audioDevices);
      }
    }
    Log.d(TAG, "--- updateAudioDeviceState done");
  }

  /**
   * AppRTCProximitySensor manages functions related to Bluetoth devices in the
   * AppRTC demo.
   */
  public static class AppRTCBluetoothManager {
    private static final String TAG = "AppRTCBluetoothManager";

    // Timeout interval for starting or stopping audio to a Bluetooth SCO device.
    private static final int BLUETOOTH_SCO_TIMEOUT_MS = 4000;
    // Maximum number of SCO connection attempts.
    private static final int MAX_SCO_CONNECTION_ATTEMPTS = 2;

    // Bluetooth connection state.
    public enum State {
      // Bluetooth is not available; no adapter or Bluetooth is off.
      UNINITIALIZED,
      // Bluetooth error happened when trying to start Bluetooth.
      ERROR,
      // Bluetooth proxy object for the Headset profile exists, but no connected headset devices,
      // SCO is not started or disconnected.
      HEADSET_UNAVAILABLE,
      // Bluetooth proxy object for the Headset profile connected, connected Bluetooth headset
      // present, but SCO is not started or disconnected.
      HEADSET_AVAILABLE,
      // Bluetooth audio SCO connection with remote device is closing.
      SCO_DISCONNECTING,
      // Bluetooth audio SCO connection with remote device is initiated.
      SCO_CONNECTING,
      // Bluetooth audio SCO connection with remote device is established.
      SCO_CONNECTED
    }

    private final Context apprtcContext;
    private final AppRTCAudioManager apprtcAudioManager;
    @Nullable
    private final AudioManager audioManager;
    private final Handler handler;

    int scoConnectionAttempts;
    private State bluetoothState;
    private final BluetoothProfile.ServiceListener bluetoothServiceListener;
    @Nullable
    private BluetoothAdapter bluetoothAdapter;
    @Nullable
    private BluetoothHeadset bluetoothHeadset;
    @Nullable
    private BluetoothDevice bluetoothDevice;
    private final BroadcastReceiver bluetoothHeadsetReceiver;

    // Runs when the Bluetooth timeout expires. We use that timeout after calling
    // startScoAudio() or stopScoAudio() because we're not guaranteed to get a
    // callback after those calls.
    private final Runnable bluetoothTimeoutRunnable = new Runnable() {
      @Override
      public void run() {
        bluetoothTimeout();
      }
    };

    /**
     * Implementation of an interface that notifies BluetoothProfile IPC clients when they have been
     * connected to or disconnected from the service.
     */
    private class BluetoothServiceListener implements BluetoothProfile.ServiceListener {
      @Override
      // Called to notify the client when the proxy object has been connected to the service.
      // Once we have the profile proxy object, we can use it to monitor the state of the
      // connection and perform other operations that are relevant to the headset profile.
      public void onServiceConnected(int profile, BluetoothProfile proxy) {
        if (profile != BluetoothProfile.HEADSET || bluetoothState == State.UNINITIALIZED) {
          return;
        }
        Log.d(TAG, "BluetoothServiceListener.onServiceConnected: BT state=" + bluetoothState);
        // Android only supports one connected Bluetooth Headset at a time.
        bluetoothHeadset = (BluetoothHeadset) proxy;
        updateAudioDeviceState();
        Log.d(TAG, "onServiceConnected done: BT state=" + bluetoothState);
      }

      @Override
      /** Notifies the client when the proxy object has been disconnected from the service. */
      public void onServiceDisconnected(int profile) {
        if (profile != BluetoothProfile.HEADSET || bluetoothState == State.UNINITIALIZED) {
          return;
        }
        Log.d(TAG, "BluetoothServiceListener.onServiceDisconnected: BT state=" + bluetoothState);
        stopScoAudio();
        bluetoothHeadset = null;
        bluetoothDevice = null;
        bluetoothState = State.HEADSET_UNAVAILABLE;
        updateAudioDeviceState();
        Log.d(TAG, "onServiceDisconnected done: BT state=" + bluetoothState);
      }
    }

    // Intent broadcast receiver which handles changes in Bluetooth device availability.
    // Detects headset changes and Bluetooth SCO state changes.
    private class BluetoothHeadsetBroadcastReceiver extends BroadcastReceiver {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (bluetoothState == State.UNINITIALIZED) {
          return;
        }
        final String action = intent.getAction();
        // Change in connection state of the Headset profile. Note that the
        // change does not tell us anything about whether we're streaming
        // audio to BT over SCO. Typically received when user turns on a BT
        // headset while audio is active using another audio device.
        if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
          final int state =
              intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
          Log.d(TAG, "BluetoothHeadsetBroadcastReceiver.onReceive: "
                  + "a=ACTION_CONNECTION_STATE_CHANGED, "
                  + "s=" + stateToString(state) + ", "
                  + "sb=" + isInitialStickyBroadcast() + ", "
                  + "BT state: " + bluetoothState);
          if (state == BluetoothHeadset.STATE_CONNECTED) {
            scoConnectionAttempts = 0;
            updateAudioDeviceState();
          } else if (state == BluetoothHeadset.STATE_CONNECTING) {
            // No action needed.
          } else if (state == BluetoothHeadset.STATE_DISCONNECTING) {
            // No action needed.
          } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
            // Bluetooth is probably powered off during the call.
            stopScoAudio();
            updateAudioDeviceState();
          }
          // Change in the audio (SCO) connection state of the Headset profile.
          // Typically received after call to startScoAudio() has finalized.
        } else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
          final int state = intent.getIntExtra(
              BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
          Log.d(TAG, "BluetoothHeadsetBroadcastReceiver.onReceive: "
                  + "a=ACTION_AUDIO_STATE_CHANGED, "
                  + "s=" + stateToString(state) + ", "
                  + "sb=" + isInitialStickyBroadcast() + ", "
                  + "BT state: " + bluetoothState);
          if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
            cancelTimer();
            if (bluetoothState == State.SCO_CONNECTING) {
              Log.d(TAG, "+++ Bluetooth audio SCO is now connected");
              bluetoothState = State.SCO_CONNECTED;
              scoConnectionAttempts = 0;
              updateAudioDeviceState();
            } else {
              Log.w(TAG, "Unexpected state BluetoothHeadset.STATE_AUDIO_CONNECTED");
            }
          } else if (state == BluetoothHeadset.STATE_AUDIO_CONNECTING) {
            Log.d(TAG, "+++ Bluetooth audio SCO is now connecting...");
          } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
            Log.d(TAG, "+++ Bluetooth audio SCO is now disconnected");
            if (isInitialStickyBroadcast()) {
              Log.d(TAG, "Ignore STATE_AUDIO_DISCONNECTED initial sticky broadcast.");
              return;
            }
            updateAudioDeviceState();
          }
        }
        Log.d(TAG, "onReceive done: BT state=" + bluetoothState);
      }
    }

    /** Construction. */
    static AppRTCBluetoothManager create(Context context, AppRTCAudioManager audioManager) {
      // Log.d(TAG, "create" + AppRTCUtils.getThreadInfo());
      return new AppRTCBluetoothManager(context, audioManager);
    }

    protected AppRTCBluetoothManager(Context context, AppRTCAudioManager audioManager) {
      Log.d(TAG, "ctor");
      ThreadUtils.checkIsOnMainThread();
      apprtcContext = context;
      apprtcAudioManager = audioManager;
      this.audioManager = getAudioManager(context);
      bluetoothState = State.UNINITIALIZED;
      bluetoothServiceListener = new BluetoothServiceListener();
      bluetoothHeadsetReceiver = new BluetoothHeadsetBroadcastReceiver();
      handler = new Handler(Looper.getMainLooper());
    }

    /** Returns the internal state. */
    public State getState() {
      ThreadUtils.checkIsOnMainThread();
      return bluetoothState;
    }

    /**
     * Activates components required to detect Bluetooth devices and to enable
     * BT SCO (audio is routed via BT SCO) for the headset profile. The end
     * state will be HEADSET_UNAVAILABLE but a state machine has started which
     * will start a state change sequence where the final outcome depends on
     * if/when the BT headset is enabled.
     * Example of state change sequence when start() is called while BT device
     * is connected and enabled:
     *   UNINITIALIZED --> HEADSET_UNAVAILABLE --> HEADSET_AVAILABLE -->
     *   SCO_CONNECTING --> SCO_CONNECTED <==> audio is now routed via BT SCO.
     * Note that the AppRTCAudioManager is also involved in driving this state
     * change.
     */
    public void start() {
      ThreadUtils.checkIsOnMainThread();
      Log.d(TAG, "start");
      if (!hasPermission(apprtcContext, android.Manifest.permission.BLUETOOTH)) {
        Log.w(TAG, "Process (pid=" + Process.myPid() + ") lacks BLUETOOTH permission");
        return;
      }
      if (bluetoothState != State.UNINITIALIZED) {
        Log.w(TAG, "Invalid BT state");
        return;
      }
      bluetoothHeadset = null;
      bluetoothDevice = null;
      scoConnectionAttempts = 0;
      // Get a handle to the default local Bluetooth adapter.
      bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if (bluetoothAdapter == null) {
        Log.w(TAG, "Device does not support Bluetooth");
        return;
      }
      // Ensure that the device supports use of BT SCO audio for off call use cases.
      if (!audioManager.isBluetoothScoAvailableOffCall()) {
        Log.e(TAG, "Bluetooth SCO audio is not available off call");
        return;
      }
      logBluetoothAdapterInfo(bluetoothAdapter);
      // Establish a connection to the HEADSET profile (includes both Bluetooth Headset and
      // Hands-Free) proxy object and install a listener.
      if (!getBluetoothProfileProxy(
              apprtcContext, bluetoothServiceListener, BluetoothProfile.HEADSET)) {
        Log.e(TAG, "BluetoothAdapter.getProfileProxy(HEADSET) failed");
        return;
      }
      // Register receivers for BluetoothHeadset change notifications.
      IntentFilter bluetoothHeadsetFilter = new IntentFilter();
      // Register receiver for change in connection state of the Headset profile.
      bluetoothHeadsetFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
      // Register receiver for change in audio connection state of the Headset profile.
      bluetoothHeadsetFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
      registerReceiver(bluetoothHeadsetReceiver, bluetoothHeadsetFilter);
      Log.d(TAG, "HEADSET profile state: "
              + stateToString(bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)));
      Log.d(TAG, "Bluetooth proxy for headset profile has started");
      bluetoothState = State.HEADSET_UNAVAILABLE;
      Log.d(TAG, "start done: BT state=" + bluetoothState);
    }

    /** Stops and closes all components related to Bluetooth audio. */
    public void stop() {
      ThreadUtils.checkIsOnMainThread();
      Log.d(TAG, "stop: BT state=" + bluetoothState);
      if (bluetoothAdapter == null) {
        return;
      }
      // Stop BT SCO connection with remote device if needed.
      stopScoAudio();
      // Close down remaining BT resources.
      if (bluetoothState == State.UNINITIALIZED) {
        return;
      }
      unregisterReceiver(bluetoothHeadsetReceiver);
      cancelTimer();
      if (bluetoothHeadset != null) {
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);
        bluetoothHeadset = null;
      }
      bluetoothAdapter = null;
      bluetoothDevice = null;
      bluetoothState = State.UNINITIALIZED;
      Log.d(TAG, "stop done: BT state=" + bluetoothState);
    }

    /**
     * Starts Bluetooth SCO connection with remote device.
     * Note that the phone application always has the priority on the usage of the SCO connection
     * for telephony. If this method is called while the phone is in call it will be ignored.
     * Similarly, if a call is received or sent while an application is using the SCO connection,
     * the connection will be lost for the application and NOT returned automatically when the call
     * ends. Also note that: up to and including API version JELLY_BEAN_MR1, this method initiates a
     * virtual voice call to the Bluetooth headset. After API version JELLY_BEAN_MR2 only a raw SCO
     * audio connection is established.
     * TODO(henrika): should we add support for virtual voice call to BT headset also for JBMR2 and
     * higher. It might be required to initiates a virtual voice call since many devices do not
     * accept SCO audio without a "call".
     */
    public boolean startScoAudio() {
      ThreadUtils.checkIsOnMainThread();
      Log.d(TAG, "startSco: BT state=" + bluetoothState + ", "
              + "attempts: " + scoConnectionAttempts + ", "
              + "SCO is on: " + isScoOn());
      if (scoConnectionAttempts >= MAX_SCO_CONNECTION_ATTEMPTS) {
        Log.e(TAG, "BT SCO connection fails - no more attempts");
        return false;
      }
      if (bluetoothState != State.HEADSET_AVAILABLE) {
        Log.e(TAG, "BT SCO connection fails - no headset available");
        return false;
      }
      // Start BT SCO channel and wait for ACTION_AUDIO_STATE_CHANGED.
      Log.d(TAG, "Starting Bluetooth SCO and waits for ACTION_AUDIO_STATE_CHANGED...");
      // The SCO connection establishment can take several seconds, hence we cannot rely on the
      // connection to be available when the method returns but instead register to receive the
      // intent ACTION_SCO_AUDIO_STATE_UPDATED and wait for the state to be SCO_AUDIO_STATE_CONNECTED.
      bluetoothState = State.SCO_CONNECTING;
      audioManager.startBluetoothSco();
      audioManager.setBluetoothScoOn(true);
      scoConnectionAttempts++;
      startTimer();
      Log.d(TAG, "startScoAudio done: BT state=" + bluetoothState + ", "
              + "SCO is on: " + isScoOn());
      return true;
    }

    /** Stops Bluetooth SCO connection with remote device. */
    public void stopScoAudio() {
      ThreadUtils.checkIsOnMainThread();
      Log.d(TAG, "stopScoAudio: BT state=" + bluetoothState + ", "
              + "SCO is on: " + isScoOn());
      if (bluetoothState != State.SCO_CONNECTING && bluetoothState != State.SCO_CONNECTED) {
        return;
      }
      cancelTimer();
      audioManager.stopBluetoothSco();
      audioManager.setBluetoothScoOn(false);
      bluetoothState = State.SCO_DISCONNECTING;
      Log.d(TAG, "stopScoAudio done: BT state=" + bluetoothState + ", "
              + "SCO is on: " + isScoOn());
    }

    /**
     * Use the BluetoothHeadset proxy object (controls the Bluetooth Headset
     * Service via IPC) to update the list of connected devices for the HEADSET
     * profile. The internal state will change to HEADSET_UNAVAILABLE or to
     * HEADSET_AVAILABLE and |bluetoothDevice| will be mapped to the connected
     * device if available.
     */
    public void updateDevice() {
      if (bluetoothState == State.UNINITIALIZED || bluetoothHeadset == null) {
        return;
      }
      Log.d(TAG, "updateDevice");
      // Get connected devices for the headset profile. Returns the set of
      // devices which are in state STATE_CONNECTED. The BluetoothDevice class
      // is just a thin wrapper for a Bluetooth hardware address.
      List<BluetoothDevice> devices = bluetoothHeadset.getConnectedDevices();
      if (devices.isEmpty()) {
        bluetoothDevice = null;
        bluetoothState = State.HEADSET_UNAVAILABLE;
        Log.d(TAG, "No connected bluetooth headset");
      } else {
        // Always use first device in list. Android only supports one device.
        bluetoothDevice = devices.get(0);
        bluetoothState = State.HEADSET_AVAILABLE;
        Log.d(TAG, "Connected bluetooth headset: "
                + "name=" + bluetoothDevice.getName() + ", "
                + "state=" + stateToString(bluetoothHeadset.getConnectionState(bluetoothDevice))
                + ", SCO audio=" + bluetoothHeadset.isAudioConnected(bluetoothDevice));
      }
      Log.d(TAG, "updateDevice done: BT state=" + bluetoothState);
    }

    /**
     * Stubs for test mocks.
     */
    @Nullable
    protected AudioManager getAudioManager(Context context) {
      return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    protected void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
      apprtcContext.registerReceiver(receiver, filter);
    }

    protected void unregisterReceiver(BroadcastReceiver receiver) {
      apprtcContext.unregisterReceiver(receiver);
    }

    protected boolean getBluetoothProfileProxy(
        Context context, BluetoothProfile.ServiceListener listener, int profile) {
      return bluetoothAdapter.getProfileProxy(context, listener, profile);
    }

    protected boolean hasPermission(Context context, String permission) {
      return apprtcContext.checkPermission(permission, Process.myPid(), Process.myUid())
          == PackageManager.PERMISSION_GRANTED;
    }

    /** Logs the state of the local Bluetooth adapter. */
    @SuppressLint("HardwareIds")
    protected void logBluetoothAdapterInfo(BluetoothAdapter localAdapter) {
      Log.d(TAG, "BluetoothAdapter: "
              + "enabled=" + localAdapter.isEnabled() + ", "
              + "state=" + stateToString(localAdapter.getState()) + ", "
              + "name=" + localAdapter.getName() + ", "
              + "address=" + localAdapter.getAddress());
      // Log the set of BluetoothDevice objects that are bonded (paired) to the local adapter.
      Set<BluetoothDevice> pairedDevices = localAdapter.getBondedDevices();
      if (!pairedDevices.isEmpty()) {
        Log.d(TAG, "paired devices:");
        for (BluetoothDevice device : pairedDevices) {
          Log.d(TAG, " name=" + device.getName() + ", address=" + device.getAddress());
        }
      }
    }

    /** Ensures that the audio manager updates its list of available audio devices. */
    private void updateAudioDeviceState() {
      ThreadUtils.checkIsOnMainThread();
      Log.d(TAG, "updateAudioDeviceState");
      apprtcAudioManager.updateAudioDeviceState();
    }

    /** Starts timer which times out after BLUETOOTH_SCO_TIMEOUT_MS milliseconds. */
    private void startTimer() {
      ThreadUtils.checkIsOnMainThread();
      Log.d(TAG, "startTimer");
      handler.postDelayed(bluetoothTimeoutRunnable, BLUETOOTH_SCO_TIMEOUT_MS);
    }

    /** Cancels any outstanding timer tasks. */
    private void cancelTimer() {
      ThreadUtils.checkIsOnMainThread();
      Log.d(TAG, "cancelTimer");
      handler.removeCallbacks(bluetoothTimeoutRunnable);
    }

    /**
     * Called when start of the BT SCO channel takes too long time. Usually
     * happens when the BT device has been turned on during an ongoing call.
     */
    private void bluetoothTimeout() {
      ThreadUtils.checkIsOnMainThread();
      if (bluetoothState == State.UNINITIALIZED || bluetoothHeadset == null) {
        return;
      }
      Log.d(TAG, "bluetoothTimeout: BT state=" + bluetoothState + ", "
              + "attempts: " + scoConnectionAttempts + ", "
              + "SCO is on: " + isScoOn());
      if (bluetoothState != State.SCO_CONNECTING) {
        return;
      }
      // Bluetooth SCO should be connecting; check the latest result.
      boolean scoConnected = false;
      List<BluetoothDevice> devices = bluetoothHeadset.getConnectedDevices();
      if (devices.size() > 0) {
        bluetoothDevice = devices.get(0);
        if (bluetoothHeadset.isAudioConnected(bluetoothDevice)) {
          Log.d(TAG, "SCO connected with " + bluetoothDevice.getName());
          scoConnected = true;
        } else {
          Log.d(TAG, "SCO is not connected with " + bluetoothDevice.getName());
        }
      }
      if (scoConnected) {
        // We thought BT had timed out, but it's actually on; updating state.
        bluetoothState = State.SCO_CONNECTED;
        scoConnectionAttempts = 0;
      } else {
        // Give up and "cancel" our request by calling stopBluetoothSco().
        Log.w(TAG, "BT failed to connect after timeout");
        stopScoAudio();
      }
      updateAudioDeviceState();
      Log.d(TAG, "bluetoothTimeout done: BT state=" + bluetoothState);
    }

    /** Checks whether audio uses Bluetooth SCO. */
    private boolean isScoOn() {
      return audioManager.isBluetoothScoOn();
    }

    /** Converts BluetoothAdapter states into local string representations. */
    private String stateToString(int state) {
      switch (state) {
        case BluetoothAdapter.STATE_DISCONNECTED:
          return "DISCONNECTED";
        case BluetoothAdapter.STATE_CONNECTED:
          return "CONNECTED";
        case BluetoothAdapter.STATE_CONNECTING:
          return "CONNECTING";
        case BluetoothAdapter.STATE_DISCONNECTING:
          return "DISCONNECTING";
        case BluetoothAdapter.STATE_OFF:
          return "OFF";
        case BluetoothAdapter.STATE_ON:
          return "ON";
        case BluetoothAdapter.STATE_TURNING_OFF:
          // Indicates the local Bluetooth adapter is turning off. Local clients should immediately
          // attempt graceful disconnection of any remote links.
          return "TURNING_OFF";
        case BluetoothAdapter.STATE_TURNING_ON:
          // Indicates the local Bluetooth adapter is turning on. However local clients should wait
          // for STATE_ON before attempting to use the adapter.
          return  "TURNING_ON";
        default:
          return "INVALID";
      }
    }
  }
}
