use fabushi_android_shared::{
    CoordinatorFailure, CoordinatorFailureCode, CoordinatorRequest, COORDINATOR_PROTOCOL_VERSION,
};
use fabushi_mahayana_agent_coordinator::{HostPort, MahayanaCoordinator};
use fabushi_mahayana_host::android_json_runtime::{AndroidHostMode, AndroidJsonHost};
use serde_json::{json, Value};
use std::path::PathBuf;

struct CoordinatorHost {
    runtime: AndroidJsonHost,
}

impl HostPort for CoordinatorHost {
    fn execute(&mut self, request: &CoordinatorRequest) -> Result<String, CoordinatorFailure> {
        let params: Value = serde_json::from_str(&request.params_json).map_err(|error| {
            CoordinatorFailure::new(
                CoordinatorFailureCode::MalformedRequest,
                format!("invalid Host params JSON: {error}"),
            )
        })?;
        self.runtime
            .dispatch(&request.method, &params)
            .map(|value| value.to_string())
            .map_err(|message| CoordinatorFailure::new(CoordinatorFailureCode::HostUnavailable, message))
    }

    fn cancel(&mut self, request_id: &str, reason: Option<&str>) -> Result<(), CoordinatorFailure> {
        self.runtime
            .cancel_operation(request_id, reason)
            .map_err(|message| CoordinatorFailure::new(CoordinatorFailureCode::Internal, message))
    }
}

pub struct AndroidNativeRuntime {
    coordinator: MahayanaCoordinator<CoordinatorHost>,
    next_request_id: u64,
}

impl AndroidNativeRuntime {
    pub fn new(app_data_dir: impl Into<PathBuf>, mode: AndroidHostMode) -> Self {
        Self {
            coordinator: MahayanaCoordinator::new(CoordinatorHost {
                runtime: AndroidJsonHost::new(app_data_dir, mode),
            }),
            next_request_id: 0,
        }
    }

    pub fn dispatch_legacy_json(&mut self, input: &str) -> String {
        let envelope: Value = match serde_json::from_str(input) {
            Ok(value) => value,
            Err(error) => return error_response(None, format!("invalid request JSON: {error}")),
        };
        let method = match envelope.get("method").and_then(Value::as_str) {
            Some(method) if !method.trim().is_empty() => method,
            _ => return error_response(envelope.get("id").cloned(), "method is required".into()),
        };
        let params = envelope.get("params").cloned().unwrap_or_else(|| json!({}));
        self.next_request_id = self.next_request_id.saturating_add(1);
        let request_id = envelope
            .get("id")
            .and_then(Value::as_str)
            .filter(|value| !value.trim().is_empty())
            .map(str::to_string)
            .unwrap_or_else(|| format!("jni-{:016}", self.next_request_id));

        let reply = self.coordinator.request(CoordinatorRequest {
            protocol_version: COORDINATOR_PROTOCOL_VERSION,
            request_id: request_id.clone(),
            session_id: "android-process".into(),
            method: method.to_string(),
            params_json: params.to_string(),
            deadline_ms: None,
        });

        match reply.result_json {
            Ok(result_json) => {
                let result = serde_json::from_str::<Value>(&result_json)
                    .unwrap_or_else(|_| Value::String(result_json));
                json!({
                    "id":envelope.get("id").cloned().unwrap_or(Value::Null),
                    "ok":true,
                    "result":result
                })
                .to_string()
            }
            Err(failure) => error_response(
                envelope.get("id").cloned(),
                format!("{}: {}", failure.code, failure.message),
            ),
        }
    }

    pub fn generation(&self) -> u64 {
        self.coordinator.generation()
    }
}

fn error_response(id: Option<Value>, error: String) -> String {
    json!({
        "id":id.unwrap_or(Value::Null),
        "ok":false,
        "error":error
    })
    .to_string()
}

#[cfg(target_os = "android")]
mod android_jni {
    use super::*;
    use jni::objects::{JObject, JString};
    use jni::sys::{jlong, jstring};
    use jni::JNIEnv;

    fn create(mut env: JNIEnv, app_data_dir: JString, mode: AndroidHostMode) -> jlong {
        let path = match env.get_string(&app_data_dir) {
            Ok(value) => PathBuf::from(value.to_string_lossy().into_owned()),
            Err(_) => return 0,
        };
        Box::into_raw(Box::new(AndroidNativeRuntime::new(path, mode))) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_com_ombhrum_fabushi_core_MahayanaHost_nativeCreate(
        env: JNIEnv,
        _object: JObject,
        app_data_dir: JString,
    ) -> jlong {
        create(env, app_data_dir, AndroidHostMode::Production)
    }

    #[no_mangle]
    pub extern "system" fn Java_com_ombhrum_fabushi_core_MahayanaHost_nativeCreateTest(
        env: JNIEnv,
        _object: JObject,
        app_data_dir: JString,
    ) -> jlong {
        create(env, app_data_dir, AndroidHostMode::Test)
    }

    #[no_mangle]
    pub extern "system" fn Java_com_ombhrum_fabushi_core_MahayanaHost_nativeDispatch(
        mut env: JNIEnv,
        _object: JObject,
        handle: jlong,
        request_json: JString,
    ) -> jstring {
        if handle == 0 {
            return env
                .new_string("{\"ok\":false,\"error\":\"native runtime is not initialized\"}")
                .map(|value| value.into_raw())
                .unwrap_or(std::ptr::null_mut());
        }
        let input = match env.get_string(&request_json) {
            Ok(value) => value.to_string_lossy().into_owned(),
            Err(error) => {
                return env
                    .new_string(error_response(None, format!("invalid request string: {error}")))
                    .map(|value| value.into_raw())
                    .unwrap_or(std::ptr::null_mut())
            }
        };
        let runtime = unsafe { &mut *(handle as *mut AndroidNativeRuntime) };
        env.new_string(runtime.dispatch_legacy_json(&input))
            .map(|value| value.into_raw())
            .unwrap_or(std::ptr::null_mut())
    }

    #[no_mangle]
    pub extern "system" fn Java_com_ombhrum_fabushi_core_MahayanaHost_nativeDestroy(
        _env: JNIEnv,
        _object: JObject,
        handle: jlong,
    ) {
        if handle != 0 {
            unsafe {
                drop(Box::from_raw(handle as *mut AndroidNativeRuntime));
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn legacy_jni_envelope_routes_through_coordinator_into_test_host() {
        let mut runtime = AndroidNativeRuntime::new("/tmp/fabushi-jni-test", AndroidHostMode::Test);
        let response: Value = serde_json::from_str(&runtime.dispatch_legacy_json(
            r#"{"method":"feature.info","params":{}}"#,
        ))
        .unwrap();
        assert_eq!(response["ok"], true);
        assert_eq!(response["result"]["platform"], "android");
        assert!(response["result"]["runtimeVersion"].as_str().unwrap().contains("test"));
        assert_eq!(runtime.generation(), 1);
    }

    #[test]
    fn unknown_renderer_method_fails_closed() {
        let mut runtime = AndroidNativeRuntime::new("/tmp/fabushi-jni-prod", AndroidHostMode::Production);
        let response: Value = serde_json::from_str(&runtime.dispatch_legacy_json(
            r#"{"method":"renderer.execAnything","params":{}}"#,
        ))
        .unwrap();
        assert_eq!(response["ok"], false);
        assert!(response["error"].as_str().unwrap().contains("unknown host method"));
    }
}
