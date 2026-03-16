#![cfg(target_os = "android")]
#![allow(non_snake_case)]

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use log::{error, info};
use std::panic;
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jboolean, jfloat, jint};
use std::sync::atomic::{AtomicI32, Ordering};


/// This initializes the Rust panic hook and Android logger correctly.
/// This should be called exactly once from the Android application main activity startup.
#[no_mangle]
pub extern "system" fn Java_com_im_a_hero_daemon_DaemonCore_init(
    mut _env: JNIEnv,
    _class: JClass,
) {
    // Setup Android-specific logger. We use a macro level filter to debug everything in dev.
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Debug)
            .with_tag("daemon_core"),
    );

    // Provide a panic hook to capture native backtraces in Logcat without crashing silently in JNI
    panic::set_hook(Box::new(|panic_info| {
        error!("daemon_core CRITICAL PANIC: {}", panic_info);
    }));

    info!("daemon_core successfully initialized! The JVM is secure, and The Upside Down is being monitored.");
}

/// A sample telemetry input interface. Shows string passing back and forth cleanly.
#[no_mangle]
pub extern "system" fn Java_com_im_a_hero_daemon_DaemonCore_analyzeTelemetry(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    let input_str: String = match env.get_string(&input) {
        Ok(s) => s.into(),
        Err(e) => {
            error!("Failed to read JString from JVM: {}", e);
            return env.new_string("ERROR_INVALID_STRING").unwrap().into_raw();
        }
    };
    
    info!("Analyzing raw telemetry from sensor bus: {}", input_str);
    
    // Core engine logic
    let analysis_result = format!("PROCESSED_OK: [{}]", input_str);
    
    match env.new_string(analysis_result) {
        Ok(j_str) => j_str.into_raw(),
        Err(e) => {
            error!("Failed to allocate new JString for JVM: {}", e);
            env.new_string("ERROR_OOM").unwrap().into_raw()
        }
    }

// Bezpieczny globalny stan (Threat Level)
static FRUSTRATION_LEVEL: AtomicI32 = AtomicI32::new(0);
const FRUSTRATION_THRESHOLD: i32 = 100;

#[no_mangle]
pub extern "system" fn Java_com_hero_TelemetryService_analyzeTouch(
    mut env: JNIEnv,
    _class: JClass,
    _x: jint,
    _y: jint,
    pressure: jfloat,
    velocity: jfloat,
) -> jboolean {
    
    // Pobieramy aktualny stan wkurzenia
    let mut current_frustration = FRUSTRATION_LEVEL.load(Ordering::Relaxed);

    // Heurystyka: Zbyt mocny nacisk + szybki ruch palca (doomscrolling / wściekłe tapnięcia)
    if pressure > 1.5 && velocity > 1000.0 {
        current_frustration += 15;
    } else if current_frustration > 0 {
        // Chłodzenie systemu, gdy użytkownik się uspokaja
        current_frustration -= 1;
    }

    // Zapisujemy nowy stan
    FRUSTRATION_LEVEL.store(current_frustration, Ordering::Relaxed);

    // Jeśli przebiliśmy sufit tolerancji – odpalamy protokół "The Hijack"
    if current_frustration >= FRUSTRATION_THRESHOLD {
        
        // Reset licznika przed uderzeniem
        FRUSTRATION_LEVEL.store(0, Ordering::Relaxed);

        // Zlecenie do Androida (Kotlina): Odpal asymetryczną wibrację i psuj ekran!
        // Wywołujemy metodę 'triggerAnomalyVibration' zdefiniowaną w klasie Javy/Kotlina
        let callback_result = env.call_method(
            &_class,
            "triggerAnomalyVibration",
            "()V", // Sygnatura metody: (brak argumentów) -> Void
            &[],
        );

        if let Err(e) = callback_result {
            println!("Rust Error: Nie udalo sie wywolac callbacku w Kotlinie: {:?}", e);
        }

        return true as jboolean;
    }

    false as jboolean
}
}
