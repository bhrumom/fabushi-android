use serde_json::{json, Value};
use std::collections::{BTreeMap, BTreeSet, VecDeque};
use std::path::PathBuf;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum AndroidHostMode {
    Production,
    Test,
}

pub struct AndroidJsonHost {
    mode: AndroidHostMode,
    #[allow(dead_code)]
    app_data_dir: PathBuf,
    logged_in: bool,
    next_attempt: u64,
    next_operation: u64,
    oauth_attempts: BTreeSet<String>,
    browser_attempts: BTreeMap<String, String>,
    events: VecDeque<Value>,
    active_operations: BTreeSet<String>,
    installed_plugins: BTreeSet<String>,
}

impl AndroidJsonHost {
    pub fn new(app_data_dir: impl Into<PathBuf>, mode: AndroidHostMode) -> Self {
        Self {
            mode,
            app_data_dir: app_data_dir.into(),
            logged_in: false,
            next_attempt: 0,
            next_operation: 0,
            oauth_attempts: BTreeSet::new(),
            browser_attempts: BTreeMap::new(),
            events: VecDeque::new(),
            active_operations: BTreeSet::new(),
            installed_plugins: BTreeSet::new(),
        }
    }

    pub fn dispatch(&mut self, method: &str, params: &Value) -> Result<Value, String> {
        match method {
            "host.platform" => Ok(json!({"platform":"android"})),
            "feature.info" => Ok(json!({
                "platform": "android",
                "protocolVersion": "fabushi.feature.v1",
                "runtimeVersion": match self.mode {
                    AndroidHostMode::Production => "android-production",
                    AndroidHostMode::Test => "android-test",
                }
            })),
            "feature.auth.status" => Ok(self.auth_status()),
            "feature.auth.deviceAgentSession" => Ok(json!({
                "loggedIn": self.logged_in,
                "session": if self.logged_in { Value::String("android-device-session".into()) } else { Value::Null }
            })),
            "feature.auth.providers" => Ok(json!([
                {"id":"google","displayName":"Google"},
                {"id":"github","displayName":"GitHub"}
            ])),
            "feature.auth.browserStart" => self.browser_start(),
            "feature.auth.browserReopen" => self.browser_reopen(params),
            "feature.auth.browserCancel" => self.browser_cancel(params),
            "feature.auth.browserPoll" => self.browser_poll(params),
            "feature.auth.oauthStart" => self.oauth_start(params),
            "feature.auth.oauthPoll" => self.oauth_poll(params),
            "feature.auth.logout" => {
                self.logged_in = false;
                Ok(self.auth_status())
            }
            "feature.execute" => self.feature_execute(params),
            "feature.receive" => Ok(self.events.pop_front().unwrap_or_else(|| json!({}))),
            "feature.interrupt" => self.feature_interrupt(params),
            "feature.approval.resolve" => Ok(json!({"status":"resolved"})),
            "feature.marketplace.browse" => self.marketplace_browse(params),
            "feature.marketplace.release" => self.marketplace_release(params),
            "feature.plugin.install" => self.plugin_install(params),
            "feature.plugin.uiDocument" => self.plugin_ui_document(params),
            "plugin.compatibility" => Ok(json!({"portableCompatible":true})),
            "plugin.permission.grant" => Ok(json!({"granted":true})),
            "plugin.permission.revoke" => Ok(json!({"granted":false})),
            "runtime.start" => Ok(json!({"status":"running"})),
            "runtime.stop" => Ok(json!({"status":"stopped"})),
            "runtime.tools" => Ok(json!([])),
            "runtime.call" => Ok(json!({"ok":true,"result":params.get("arguments").cloned().unwrap_or(Value::Null)})),
            "feature.messaging.access.issue" => Ok(json!({"status":"available"})),
            "feature.messaging.blob.read" => Ok(json!({"data":Value::Null})),
            "feature.messaging.execute" => Ok(json!({"ok":true})),
            "platform.request" => Ok(json!({"ok":true})),
            other => Err(format!("unknown host method {other}")),
        }
    }

    pub fn cancel_operation(&mut self, operation_id: &str, reason: Option<&str>) -> Result<(), String> {
        if operation_id.trim().is_empty() {
            return Err("operation id is required".into());
        }
        self.active_operations.remove(operation_id);
        self.events.push_back(json!({
            "type":"operation.interrupted",
            "operationId":operation_id,
            "reason":reason.unwrap_or("cancelled")
        }));
        Ok(())
    }

    fn auth_status(&self) -> Value {
        if self.logged_in {
            json!({
                "loggedIn":true,
                "user":{
                    "nickname":"Fabushi",
                    "username":"fabushi",
                    "email":"fabushi@example.invalid"
                }
            })
        } else {
            json!({"loggedIn":false,"user":Value::Null})
        }
    }

    fn next_attempt_id(&mut self, prefix: &str) -> String {
        self.next_attempt = self.next_attempt.saturating_add(1);
        format!("{prefix}_{:08}", self.next_attempt)
    }

    fn next_operation_id(&mut self, request_id: &str) -> String {
        self.next_operation = self.next_operation.saturating_add(1);
        if request_id.trim().is_empty() {
            format!("android-operation-{:08}", self.next_operation)
        } else {
            request_id.to_string()
        }
    }

    fn browser_start(&mut self) -> Result<Value, String> {
        let attempt_id = self.next_attempt_id("browser");
        let url = format!("https://auth.fabushi.invalid/android?attemptId={attempt_id}");
        self.browser_attempts.insert(attempt_id.clone(), "pending".into());
        Ok(json!({"attemptId":attempt_id,"url":url,"status":"pending"}))
    }

    fn browser_reopen(&mut self, params: &Value) -> Result<Value, String> {
        let attempt_id = required_string(params, "attemptId")?;
        if !self.browser_attempts.contains_key(attempt_id) {
            return Err("browser login attempt is unknown".into());
        }
        Ok(json!({
            "attemptId":attempt_id,
            "url":format!("https://auth.fabushi.invalid/android?attemptId={attempt_id}")
        }))
    }

    fn browser_cancel(&mut self, params: &Value) -> Result<Value, String> {
        let attempt_id = required_string(params, "attemptId")?.to_string();
        self.browser_attempts.insert(attempt_id.clone(), "cancelled".into());
        Ok(json!({"attemptId":attempt_id,"status":"cancelled"}))
    }

    fn browser_poll(&mut self, params: &Value) -> Result<Value, String> {
        let attempt_id = required_string(params, "attemptId")?.to_string();
        let status = self.browser_attempts.get(&attempt_id).cloned().unwrap_or_else(|| "unknown".into());
        if self.mode == AndroidHostMode::Test && status == "pending" {
            self.logged_in = true;
            self.browser_attempts.insert(attempt_id.clone(), "completed".into());
            return Ok(json!({"attemptId":attempt_id,"status":"completed","auth":self.auth_status()}));
        }
        Ok(json!({"attemptId":attempt_id,"status":status}))
    }

    fn oauth_start(&mut self, params: &Value) -> Result<Value, String> {
        let provider = required_string(params, "provider")?;
        let attempt_id = self.next_attempt_id("oauth");
        self.oauth_attempts.insert(attempt_id.clone());
        Ok(json!({
            "attemptId":attempt_id,
            "provider":provider,
            "url":format!("https://auth.fabushi.invalid/oauth/{provider}?attemptId={attempt_id}")
        }))
    }

    fn oauth_poll(&mut self, params: &Value) -> Result<Value, String> {
        let attempt_id = required_string(params, "attemptId")?;
        if !self.oauth_attempts.remove(attempt_id) {
            return Err("OAuth attempt is unknown or already consumed".into());
        }
        if self.mode == AndroidHostMode::Test {
            self.logged_in = true;
            Ok(json!({"attemptId":attempt_id,"status":"completed","auth":self.auth_status()}))
        } else {
            Ok(json!({"attemptId":attempt_id,"status":"pending"}))
        }
    }

    fn feature_execute(&mut self, params: &Value) -> Result<Value, String> {
        let command = params.get("command").and_then(Value::as_object).ok_or("feature.execute requires command")?;
        let command = Value::Object(command.clone());
        let kind = required_string(&command, "type")?;
        let request_id = command.get("requestId").and_then(Value::as_str).unwrap_or("");
        let operation_id = self.next_operation_id(request_id);
        self.active_operations.insert(operation_id.clone());
        self.events.push_back(json!({
            "type":"operation.started",
            "operationId":operation_id,
            "requestId":request_id
        }));

        match kind {
            "chat.send" => {
                let text = command.get("text").and_then(Value::as_str).unwrap_or("");
                self.events.push_back(json!({
                    "type":"chat.message",
                    "operationId":operation_id,
                    "role":"assistant",
                    "text": if self.mode == AndroidHostMode::Test {
                        "自动化测试状态正常。"
                    } else if text.is_empty() {
                        "Fabushi Android Host is ready."
                    } else {
                        "Fabushi Android Host accepted the message."
                    }
                }));
                self.finish_operation(&operation_id);
            }
            "marketplace.install" => {
                if let Some(id) = command.get("miniAppId").and_then(Value::as_str) {
                    self.installed_plugins.insert(id.to_string());
                }
                self.finish_operation(&operation_id);
            }
            "miniapp.open" | "session.clear" => {
                self.finish_operation(&operation_id);
            }
            "capability.request" => {
                self.events.push_back(json!({
                    "type":"approval.requested",
                    "operationId":operation_id,
                    "approvalId":format!("approval-{operation_id}"),
                    "capability":command.get("capability").cloned().unwrap_or(Value::Null),
                    "reason":command.get("reason").cloned().unwrap_or(Value::Null)
                }));
            }
            "runtime.longTask" => {}
            _ => {
                self.finish_operation(&operation_id);
            }
        }

        Ok(json!({"requestId":request_id,"operationId":operation_id,"accepted":true}))
    }

    fn finish_operation(&mut self, operation_id: &str) {
        self.active_operations.remove(operation_id);
        self.events.push_back(json!({
            "type":"operation.completed",
            "operationId":operation_id
        }));
    }

    fn feature_interrupt(&mut self, params: &Value) -> Result<Value, String> {
        let operation_id = required_string(params, "operationId")?.to_string();
        self.cancel_operation(&operation_id, Some("user"))?;
        Ok(json!({"operationId":operation_id,"status":"interrupted"}))
    }

    fn marketplace_browse(&self, _params: &Value) -> Result<Value, String> {
        let plugins = if self.mode == AndroidHostMode::Test {
            json!([{
                "pluginId":"global-dharma",
                "displayName":"全球法布施",
                "description":"Android deterministic Mini App",
                "latestVersion":"1.0.0",
                "commands":[]
            }])
        } else {
            json!([])
        };
        Ok(json!({"plugins":plugins}))
    }

    fn marketplace_release(&self, params: &Value) -> Result<Value, String> {
        let plugin_id = required_string(params, "pluginId")?;
        let version = params.get("version").and_then(Value::as_str).unwrap_or("1.0.0");
        let install = json!({
            "protocol":"fabushi.marketplace.install.v1",
            "strategy":"github-immutable",
            "source":{
                "sourceRef":"android-clean-room",
                "marketplaceHostsPackage":false
            }
        });
        Ok(json!({
            "pluginId":plugin_id,
            "version":version,
            "install":install,
            "releaseManifest":{
                "pluginId":plugin_id,
                "version":version,
                "install":install
            }
        }))
    }

    fn plugin_install(&mut self, params: &Value) -> Result<Value, String> {
        let release = params.get("release").ok_or("feature.plugin.install requires release")?;
        let plugin_id = release
            .get("pluginId")
            .and_then(Value::as_str)
            .unwrap_or("global-dharma")
            .to_string();
        self.installed_plugins.insert(plugin_id.clone());
        Ok(json!({
            "pluginId":plugin_id,
            "runtime":"deepseek-js",
            "requestedPermissions":[]
        }))
    }

    fn plugin_ui_document(&self, params: &Value) -> Result<Value, String> {
        let plugin_id = required_string(params, "pluginId")?;
        if !self.installed_plugins.contains(plugin_id) && self.mode != AndroidHostMode::Test {
            return Err("plugin is not installed".into());
        }
        Ok(json!({
            "pluginId":plugin_id,
            "html":"<!doctype html><html><body><main id=\"app\">Fabushi Mini App</main></body></html>"
        }))
    }
}

fn required_string<'a>(value: &'a Value, key: &str) -> Result<&'a str, String> {
    value
        .get(key)
        .and_then(Value::as_str)
        .filter(|value| !value.trim().is_empty())
        .ok_or_else(|| format!("{key} is required"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn deterministic_test_journey_covers_auth_stream_approval_and_interrupt() {
        let mut host = AndroidJsonHost::new("/tmp/fabushi-host-test", AndroidHostMode::Test);
        assert_eq!(host.dispatch("feature.info", &json!({})).unwrap()["platform"], "android");
        assert!(host.dispatch("feature.auth.providers", &json!({})).unwrap().as_array().unwrap().iter().any(|p| p["id"] == "google"));

        let oauth = host.dispatch("feature.auth.oauthStart", &json!({"provider":"google"})).unwrap();
        let completed = host.dispatch("feature.auth.oauthPoll", &json!({"attemptId":oauth["attemptId"]})).unwrap();
        assert_eq!(completed["status"], "completed");

        let accepted = host.dispatch("feature.execute", &json!({"command":{
            "type":"capability.request",
            "requestId":"capability-1",
            "capability":"camera"
        }})).unwrap();
        assert_eq!(accepted["requestId"], "capability-1");
        let mut saw_approval = false;
        for _ in 0..8 {
            let event = host.dispatch("feature.receive", &json!({})).unwrap();
            if event["type"] == "approval.requested" {
                saw_approval = true;
                break;
            }
        }
        assert!(saw_approval);

        let long_task = host.dispatch("feature.execute", &json!({"command":{
            "type":"runtime.longTask",
            "requestId":"long-1"
        }})).unwrap();
        let operation_id = long_task["operationId"].as_str().unwrap();
        let interrupted = host.dispatch("feature.interrupt", &json!({"operationId":operation_id})).unwrap();
        assert_eq!(interrupted["status"], "interrupted");
    }

    #[test]
    fn production_unknown_methods_fail_closed() {
        let mut host = AndroidJsonHost::new("/tmp/fabushi-host-prod", AndroidHostMode::Production);
        assert!(host.dispatch("arbitrary.renderer.method", &json!({})).is_err());
    }
}
