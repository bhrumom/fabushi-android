use std::error::Error;

pub const CLOUD_AGENT_STORAGE_DISABLED: &str = "CLOUD_AGENT_STORAGE_DISABLED";
pub const GATEWAY_NO_STORAGE_MESSAGE_MARKER: &str =
    "sand box access blocked by privacy mode (no_storage)";
pub const GATEWAY_ACCESS_DENIED_MESSAGE_MARKER: &str =
    "sand box access refused by backend access gate (access_denied)";
pub const SAND_BOX_BLOCKED: &str = "SAND_BOX_BLOCKED";
pub const SAND_BOX_BLOCK_REASON_KEY: &str = "sandBoxBlockReason";
pub const GATEWAY_BOX_BLOCKED_PREFIX: &str = "sand box blocked by kill switch: ";
pub const SAND_CLIENT_PAUSE_REASON: &str = "SAND_CLIENT_PAUSE";

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct SandBoxBlockedInfo {
    pub reason: String,
    pub title: String,
    pub detail: String,
}

pub fn encode_sand_box_blocked_message(info: &SandBoxBlockedInfo) -> String {
    format!(
        "{GATEWAY_BOX_BLOCKED_PREFIX}{}\u{001f}{}\u{001f}{}",
        info.reason, info.title, info.detail
    )
}

pub fn has_sand_box_blocked_marker(message: &str) -> bool {
    message.contains(GATEWAY_BOX_BLOCKED_PREFIX)
}

pub fn sand_client_pause_blocked_message() -> String {
    encode_sand_box_blocked_message(&SandBoxBlockedInfo {
        reason: SAND_CLIENT_PAUSE_REASON.to_string(),
        title: String::new(),
        detail: String::new(),
    })
}

pub fn find_sand_box_blocked_message(error: &(dyn Error + 'static)) -> Option<String> {
    let mut current: Option<&(dyn Error + 'static)> = Some(error);
    while let Some(node) = current {
        let message = node.to_string();
        if let Some(start) = message.find(GATEWAY_BOX_BLOCKED_PREFIX) {
            return Some(message[start..].to_string());
        }
        current = node.source();
    }
    None
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::{error::Error, fmt};

    #[derive(Debug)]
    struct Wrapped {
        message: &'static str,
        source: Option<Box<dyn Error + Send + Sync>>,
    }
    impl fmt::Display for Wrapped {
        fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
            f.write_str(self.message)
        }
    }
    impl Error for Wrapped {
        fn source(&self) -> Option<&(dyn Error + 'static)> {
            self.source
                .as_deref()
                .map(|value| value as &(dyn Error + 'static))
        }
    }

    #[test]
    fn marker_round_trip_and_nested_error_search_are_stable() {
        let encoded = encode_sand_box_blocked_message(&SandBoxBlockedInfo {
            reason: "maintenance".into(),
            title: "Paused".into(),
            detail: "Try later".into(),
        });
        assert!(has_sand_box_blocked_marker(&encoded));
        let error = Wrapped {
            message: "outer",
            source: Some(Box::new(Wrapped {
                message: Box::leak(format!("prefix {encoded} suffix").into_boxed_str()),
                source: None,
            })),
        };
        assert_eq!(
            find_sand_box_blocked_message(&error).as_deref(),
            Some(format!("{encoded} suffix").as_str())
        );
        assert!(sand_client_pause_blocked_message().contains(SAND_CLIENT_PAUSE_REASON));
    }
}
