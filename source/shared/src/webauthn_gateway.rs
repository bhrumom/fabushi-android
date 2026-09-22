use url::Url;

pub const GATEWAY_WEBAUTHN_REQUESTS_PATH: &str = "/webauthn/requests";
pub const GATEWAY_WEBAUTHN_RESPONSES_PATH: &str = "/webauthn/responses";
pub const SAND_WEBAUTHN_HEARTBEAT_INTERVAL_MS: u64 = 10_000;
pub const SAND_WEBAUTHN_LIVENESS_WINDOW_MS: u64 = 30_000;
pub const SAND_WEBAUTHN_CEREMONY_TIMEOUT_MS: u64 = 120_000;

pub const SAND_NO_WEBAUTHN_MACHINE_MESSAGE: &str =
    "Your computer isn't connected right now, so the security key can't be reached. Open Grok Bot on the machine your key is plugged into and try again.";
pub const SAND_WEBAUTHN_MACHINE_UNAVAILABLE_MESSAGE: &str =
    "Your computer looks disconnected, so the security key can't be reached. Reconnect it and try again.";

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum SandWebAuthnOriginClass {
    CursorCom,
    Subdomain,
    External,
}

pub fn sand_webauthn_origin_class(origin: &str) -> SandWebAuthnOriginClass {
    let hostname = match Url::parse(origin).ok().and_then(|url| url.host_str().map(str::to_owned)) {
        Some(hostname) => hostname,
        None => return SandWebAuthnOriginClass::External,
    };
    if hostname == "cursor.com" {
        SandWebAuthnOriginClass::CursorCom
    } else if hostname.ends_with(".cursor.com") {
        SandWebAuthnOriginClass::Subdomain
    } else {
        SandWebAuthnOriginClass::External
    }
}

#[derive(Clone, Debug, PartialEq)]
pub struct WebAuthnCeremony {
    pub kind: String,
    pub origin: String,
    pub payload_json: String,
}

#[derive(Clone, Debug, PartialEq)]
pub enum WebAuthnRequestFrame {
    Welcome { provider_id: String },
    Ceremony {
        request_id: String,
        ceremony: WebAuthnCeremony,
    },
    Cancel { request_id: String },
}

#[derive(Clone, Debug, PartialEq)]
pub enum WebAuthnStage {
    Grant,
    Sign,
}

#[derive(Clone, Debug, PartialEq)]
pub enum WebAuthnStageOutcome {
    Ok,
    Declined,
    Failed,
}

#[derive(Clone, Debug, PartialEq)]
pub enum WebAuthnResponseFrame {
    Hello {
        computer_id: Option<String>,
        label: Option<String>,
    },
    Ping,
    Stage {
        request_id: String,
        stage: WebAuthnStage,
        outcome: WebAuthnStageOutcome,
    },
    Result {
        request_id: String,
        credential_json: String,
    },
    Error {
        request_id: String,
        name: String,
        message: String,
        code: Option<String>,
    },
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn origin_classification_is_fail_closed() {
        assert_eq!(
            sand_webauthn_origin_class("https://cursor.com"),
            SandWebAuthnOriginClass::CursorCom
        );
        assert_eq!(
            sand_webauthn_origin_class("https://agent.cursor.com/path"),
            SandWebAuthnOriginClass::Subdomain
        );
        assert_eq!(
            sand_webauthn_origin_class("https://cursor.com.example.test"),
            SandWebAuthnOriginClass::External
        );
        assert_eq!(
            sand_webauthn_origin_class("not a url"),
            SandWebAuthnOriginClass::External
        );
    }
}
