use crate::android_agent_roster::AndroidAgentRoster;
use crate::extensions::webauthn_proxy::{
    WebAuthnBridgeError, WebAuthnProxyExtension, WebAuthnProxyExtensionConfig,
};
use fabushi_android_shared::webauthn_gateway::{
    WebAuthnCeremony, WebAuthnRequestFrame, WebAuthnResponseFrame, WebAuthnStage,
    WebAuthnStageOutcome,
};
use serde_json::{json, Value};
use std::collections::{BTreeMap, BTreeSet, VecDeque};
use std::path::PathBuf;
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum AndroidHostMode {
    Production,
    Test,
}

pub struct AndroidJsonHost {
    mode: AndroidHostMode,
    agents: AndroidAgentRoster,
    logged_in: bool,
    next_attempt: u64,
    next_operation: u64,
    oauth_attempts: BTreeSet<String>,
    browser_attempts: BTreeMap<String, String>,
    events: VecDeque<Value>,
    active_operations: BTreeSet<String>,
    installed_plugins: BTreeSet<String>,
    webauthn: WebAuthnProxyExtension,
    webauthn_provider_queues: BTreeMap<String, VecDeque<WebAuthnRequestFrame>>,
}

impl AndroidJsonHost {
    pub fn new(app_data_dir: impl Into<PathBuf>, mode: AndroidHostMode) -> Self {
        let app_data_dir = app_data_dir.into();
        let agents = AndroidAgentRoster::open(app_data_dir.join("agents.json"))
            .unwrap_or_else(|error| panic!("failed to open canonical Android agent roster: {error}"));
        Self {
            mode,
            agents,
            logged_in: false,
            next_attempt: 0,
            next_operation: 0,
            oauth_attempts: BTreeSet::new(),
            browser_attempts: BTreeMap::new(),
            events: VecDeque::new(),
            active_operations: BTreeSet::new(),
            installed_plugins: BTreeSet::new(),
            webauthn: WebAuthnProxyExtension::new(WebAuthnProxyExtensionConfig::default()),
            webauthn_provider_queues: BTreeMap::new(),
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
            "listAgents" => Ok(Value::Array(
                self.agents.list().into_iter().map(|agent| agent.as_json()).collect()
            )),
            "countAgents" => Ok(json!(self.agents.count())),
            "createAgent" => {
                let name = required_string(params, "name")?;
                let description = params.get("description").and_then(Value::as_str).unwrap_or("");
                let agent = self.agents.create(name, description).map_err(|error| error.to_string())?;
                Ok(json!({"agent": agent.as_json()}))
            }
            "updateAgent" => {
                let id = required_string(params, "id")?;
                let current = self.agents.get(id).ok_or_else(|| "agent not found".to_string())?;
                let profile = params.get("profile").and_then(Value::as_object).ok_or("profile is required")?;
                let name = profile.get("name").and_then(Value::as_str).unwrap_or(&current.name);
                let description = profile.get("description").and_then(Value::as_str).unwrap_or(&current.description);
                let agent = self.agents.update_profile(id, name, description).map_err(|error| error.to_string())?;
                Ok(agent.as_json())
            }
            "setAgentHiddenFromSidebar" => {
                let id = required_string(params, "id")?;
                let is_hidden = params.get("isHidden").and_then(Value::as_bool).ok_or("isHidden is required")?;
                let agent = self.agents.set_hidden(id, is_hidden).map_err(|error| error.to_string())?;
                Ok(agent.as_json())
            }
            "setAgentUnread" => {
                let id = required_string(params, "id")?;
                let is_unread = params.get("isUnread").and_then(Value::as_bool).ok_or("isUnread is required")?;
                let agent = self.agents.set_unread(id, is_unread).map_err(|error| error.to_string())?;
                Ok(agent.as_json())
            }
            "duplicateAgent" => {
                let id = required_string(params, "id")?;
                let agent = self.agents.duplicate(id).map_err(|error| error.to_string())?;
                Ok(json!({"agent": agent.as_json()}))
            }
            "deleteAgents" => {
                let ids = params.get("ids").and_then(Value::as_array).ok_or("ids array is required")?
                    .iter().filter_map(Value::as_str).map(str::to_string).collect::<Vec<_>>();
                let deleted = self.agents.delete(&ids).map_err(|error| error.to_string())?;
                Ok(json!({"deletedIds": deleted}))
            }
            "getPinnedAgents" => Ok(json!(self.agents.pinned_agent_ids())),
            "setPinnedAgents" => {
                let ids = params.get("ids").and_then(Value::as_array).ok_or("ids array is required")?
                    .iter().filter_map(Value::as_str).map(str::to_string).collect::<Vec<_>>();
                let ids = self.agents.set_pinned_agents(&ids).map_err(|error| error.to_string())?;
                Ok(json!(ids))
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
            "feature.webauthn.registerProvider" => self.webauthn_register_provider(),
            "feature.webauthn.unregisterProvider" => self.webauthn_unregister_provider(params),
            "feature.webauthn.pollRequest" => self.webauthn_poll_request(params),
            "feature.webauthn.submitResponses" => self.webauthn_submit_responses(params),
            "feature.webauthn.requestCeremony" => self.webauthn_request_ceremony(params),
            "platform.request" => Ok(json!({"ok":true})),
            other => Err(format!("unknown host method {other}")),
        }
    }


    fn webauthn_register_provider(&mut self) -> Result<Value, String> {
        let now = now_ms();
        let (provider_id, welcome) = self.webauthn.register_provider(now);
        self.webauthn_provider_queues
            .entry(provider_id.clone())
            .or_default()
            .push_back(welcome);
        Ok(json!({"providerId": provider_id}))
    }

    fn webauthn_unregister_provider(&mut self, params: &Value) -> Result<Value, String> {
        let provider_id = required_string(params, "providerId")?.to_string();
        self.webauthn.bridge_mut().unregister_provider(&provider_id);
        self.webauthn_provider_queues.remove(&provider_id);
        Ok(json!({"providerId":provider_id,"status":"unregistered"}))
    }

    fn webauthn_poll_request(&mut self, params: &Value) -> Result<Value, String> {
        let provider_id = required_string(params, "providerId")?.to_string();
        for (target_provider, _, frame) in self.webauthn.expire(now_ms()) {
            self.webauthn_provider_queues
                .entry(target_provider)
                .or_default()
                .push_back(frame);
        }
        let frame = self
            .webauthn_provider_queues
            .entry(provider_id.clone())
            .or_default()
            .pop_front();
        Ok(json!({
            "providerId": provider_id,
            "frame": frame.map(webauthn_request_frame_json).unwrap_or(Value::Null),
        }))
    }

    fn webauthn_submit_responses(&mut self, params: &Value) -> Result<Value, String> {
        let provider_id = params
            .get("providerId")
            .and_then(Value::as_str)
            .filter(|value| !value.trim().is_empty());
        let raw_frames = params
            .get("frames")
            .and_then(Value::as_array)
            .ok_or("frames array is required")?;
        let frames = raw_frames
            .iter()
            .map(parse_webauthn_response_frame)
            .collect::<Result<Vec<_>, _>>()?;
        let settlements = self
            .webauthn
            .submit_responses(now_ms(), provider_id, &frames)
            .into_iter()
            .map(|(request_id, settlement)| match settlement {
                crate::extensions::webauthn_proxy::WebAuthnBridgeSettlement::CredentialJson(value) => {
                    json!({"requestId":request_id,"kind":"result","credentialJson":value})
                }
                crate::extensions::webauthn_proxy::WebAuthnBridgeSettlement::Error { name, message, code } => {
                    json!({"requestId":request_id,"kind":"error","name":name,"message":message,"code":code})
                }
            })
            .collect::<Vec<_>>();
        Ok(json!({"accepted":true,"settlements":settlements}))
    }

    fn webauthn_request_ceremony(&mut self, params: &Value) -> Result<Value, String> {
        let kind = required_string(params, "kind")?;
        if kind != "create" && kind != "get" {
            return Err("kind must be create or get".into());
        }
        let origin = required_string(params, "origin")?;
        let payload_json = params
            .get("payloadJson")
            .and_then(Value::as_str)
            .filter(|value| !value.trim().is_empty())
            .ok_or("payloadJson is required")?;
        serde_json::from_str::<Value>(payload_json)
            .map_err(|error| format!("payloadJson must be valid JSON: {error}"))?;

        let request = self
            .webauthn
            .request_ceremony(
                now_ms(),
                WebAuthnCeremony {
                    kind: kind.to_string(),
                    origin: origin.to_string(),
                    payload_json: payload_json.to_string(),
                },
            )
            .map_err(webauthn_bridge_error_message)?;
        self.webauthn_provider_queues
            .entry(request.provider_id.clone())
            .or_default()
            .push_back(request.frame);
        Ok(json!({
            "providerId":request.provider_id,
            "requestId":request.request_id,
            "deadlineAtMs":request.deadline_at_ms,
        }))
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
            "bot.list" => {
                let bots = self.agents.list().into_iter().map(|agent| agent.as_json()).collect::<Vec<_>>();
                self.events.push_back(json!({
                    "type":"bot.listed",
                    "operationId":operation_id,
                    "requestId":request_id,
                    "bots":bots,
                }));
                self.finish_operation(&operation_id);
            }
            "bot.create" => {
                let name = required_string(&command, "name")?;
                let description = command.get("description").and_then(Value::as_str).unwrap_or("");
                let agent = self.agents.create(name, description).map_err(|error| error.to_string())?;
                self.events.push_back(json!({
                    "type":"bot.created",
                    "operationId":operation_id,
                    "requestId":request_id,
                    "bot":agent.as_json(),
                }));
                self.finish_operation(&operation_id);
            }
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


fn now_ms() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis()
        .try_into()
        .unwrap_or(u64::MAX)
}

fn webauthn_bridge_error_message(error: WebAuthnBridgeError) -> String {
    match error {
        WebAuthnBridgeError::NoProvider { message }
        | WebAuthnBridgeError::ProviderStale { message } => message.to_string(),
        WebAuthnBridgeError::DispatchFailed(message) => message,
        WebAuthnBridgeError::UnknownRequest => "unknown WebAuthn request".into(),
        WebAuthnBridgeError::TimedOut => "WebAuthn request timed out".into(),
    }
}

fn webauthn_request_frame_json(frame: WebAuthnRequestFrame) -> Value {
    match frame {
        WebAuthnRequestFrame::Welcome { provider_id } => {
            json!({"kind":"welcome","providerId":provider_id})
        }
        WebAuthnRequestFrame::Ceremony { request_id, ceremony } => json!({
            "kind":"ceremony",
            "requestId":request_id,
            "ceremony":{
                "kind":ceremony.kind,
                "origin":ceremony.origin,
                "payloadJson":ceremony.payload_json,
            }
        }),
        WebAuthnRequestFrame::Cancel { request_id } => {
            json!({"kind":"cancel","requestId":request_id})
        }
    }
}

fn parse_webauthn_response_frame(value: &Value) -> Result<WebAuthnResponseFrame, String> {
    let kind = required_string(value, "kind")?;
    match kind {
        "hello" => Ok(WebAuthnResponseFrame::Hello {
            computer_id: value.get("computerId").and_then(Value::as_str).map(str::to_string),
            label: value.get("label").and_then(Value::as_str).map(str::to_string),
        }),
        "ping" => Ok(WebAuthnResponseFrame::Ping),
        "stage" => {
            let request_id = required_string(value, "requestId")?.to_string();
            let stage = match required_string(value, "stage")? {
                "grant" => WebAuthnStage::Grant,
                "sign" => WebAuthnStage::Sign,
                _ => return Err("stage must be grant or sign".into()),
            };
            let outcome = match required_string(value, "outcome")? {
                "ok" => WebAuthnStageOutcome::Ok,
                "declined" => WebAuthnStageOutcome::Declined,
                "failed" => WebAuthnStageOutcome::Failed,
                _ => return Err("outcome must be ok, declined, or failed".into()),
            };
            Ok(WebAuthnResponseFrame::Stage {
                request_id,
                stage,
                outcome,
            })
        }
        "result" => Ok(WebAuthnResponseFrame::Result {
            request_id: required_string(value, "requestId")?.to_string(),
            credential_json: required_string(value, "credentialJson")?.to_string(),
        }),
        "error" => Ok(WebAuthnResponseFrame::Error {
            request_id: required_string(value, "requestId")?.to_string(),
            name: required_string(value, "name")?.to_string(),
            message: required_string(value, "message")?.to_string(),
            code: value.get("code").and_then(Value::as_str).map(str::to_string),
        }),
        _ => Err(format!("unsupported WebAuthn response frame kind {kind}")),
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
    fn webauthn_provider_transport_queues_ceremony_and_settles_result_once() {
        let mut host = AndroidJsonHost::new("/tmp/fabushi-host-webauthn", AndroidHostMode::Test);
        let registered = host
            .dispatch("feature.webauthn.registerProvider", &json!({}))
            .unwrap();
        let provider_id = registered["providerId"].as_str().unwrap().to_string();

        let welcome = host
            .dispatch(
                "feature.webauthn.pollRequest",
                &json!({"providerId":provider_id}),
            )
            .unwrap();
        assert_eq!(welcome["frame"]["kind"], "welcome");

        host.dispatch(
            "feature.webauthn.submitResponses",
            &json!({"providerId":provider_id,"frames":[{"kind":"ping"}]}),
        )
        .unwrap();

        let requested = host
            .dispatch(
                "feature.webauthn.requestCeremony",
                &json!({
                    "kind":"get",
                    "origin":"https://cursor.com",
                    "payloadJson":"{\"challenge\":\"abc\"}"
                }),
            )
            .unwrap();
        let request_id = requested["requestId"].as_str().unwrap().to_string();

        let ceremony = host
            .dispatch(
                "feature.webauthn.pollRequest",
                &json!({"providerId":provider_id}),
            )
            .unwrap();
        assert_eq!(ceremony["frame"]["kind"], "ceremony");
        assert_eq!(ceremony["frame"]["requestId"], request_id);

        let settled = host
            .dispatch(
                "feature.webauthn.submitResponses",
                &json!({
                    "providerId":provider_id,
                    "frames":[{
                        "kind":"result",
                        "requestId":request_id,
                        "credentialJson":"{\"id\":\"cred-1\"}"
                    }]
                }),
            )
            .unwrap();
        assert_eq!(settled["settlements"].as_array().unwrap().len(), 1);

        let duplicate = host
            .dispatch(
                "feature.webauthn.submitResponses",
                &json!({
                    "providerId":provider_id,
                    "frames":[{
                        "kind":"result",
                        "requestId":request_id,
                        "credentialJson":"{}"
                    }]
                }),
            )
            .unwrap();
        assert!(duplicate["settlements"].as_array().unwrap().is_empty());
    }

    #[test]
    fn production_unknown_methods_fail_closed() {
        let mut host = AndroidJsonHost::new("/tmp/fabushi-host-prod", AndroidHostMode::Production);
        assert!(host.dispatch("arbitrary.renderer.method", &json!({})).is_err());
    }
    #[test]
    fn canonical_agent_roster_drives_direct_and_bot_surfaces() {
        let root = std::env::temp_dir().join(format!(
            "fabushi-json-host-roster-{}-{}",
            std::process::id(),
            now_ms()
        ));
        let mut host = AndroidJsonHost::new(&root, AndroidHostMode::Test);

        let created = host.dispatch("createAgent", &json!({
            "name":"First Agent",
            "description":"one",
            "origin":"user"
        })).unwrap();
        let id = created["agent"]["id"].as_str().unwrap().to_string();

        host.dispatch("updateAgent", &json!({
            "id":id,
            "profile":{"name":"Renamed Agent","description":"two"}
        })).unwrap();
        host.dispatch("setAgentHiddenFromSidebar", &json!({"id":id,"isHidden":true})).unwrap();
        host.dispatch("setAgentUnread", &json!({"id":id,"isUnread":true})).unwrap();
        host.dispatch("setPinnedAgents", &json!({"ids":[id]})).unwrap();

        let list = host.dispatch("listAgents", &json!({})).unwrap();
        assert_eq!(list.as_array().unwrap().len(), 1);
        assert_eq!(list[0]["name"], "Renamed Agent");
        assert_eq!(list[0]["isHiddenFromSidebar"], true);
        assert_eq!(list[0]["hasUnread"], true);
        assert_eq!(list[0]["isPinned"], true);

        host.dispatch("feature.execute", &json!({"command":{
            "type":"bot.list",
            "requestId":"bot-list-1"
        }})).unwrap();
        let mut listed = None;
        for _ in 0..4 {
            let event = host.dispatch("feature.receive", &json!({})).unwrap();
            if event["type"] == "bot.listed" {
                listed = Some(event);
                break;
            }
        }
        let listed = listed.expect("bot.listed event");
        assert_eq!(listed["bots"][0]["id"], id);

        let duplicate = host.dispatch("duplicateAgent", &json!({"id":id})).unwrap();
        let duplicate_id = duplicate["agent"]["id"].as_str().unwrap().to_string();
        assert_ne!(duplicate_id, id);
        assert_eq!(host.dispatch("countAgents", &json!({})).unwrap(), 2);

        host.dispatch("deleteAgents", &json!({"ids":[id]})).unwrap();
        assert_eq!(host.dispatch("countAgents", &json!({})).unwrap(), 1);

        drop(host);
        let reopened = AndroidJsonHost::new(&root, AndroidHostMode::Test);
        let reopened_list = reopened.agents.list();
        assert_eq!(reopened_list.len(), 1);
        assert_eq!(reopened_list[0].id, duplicate_id);
        let _ = std::fs::remove_dir_all(root);
    }

}
