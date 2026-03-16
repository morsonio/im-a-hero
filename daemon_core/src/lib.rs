#![cfg(target_os = "android")]
#![allow(non_snake_case)]

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use log::{error, info};
use std::panic;

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
}
