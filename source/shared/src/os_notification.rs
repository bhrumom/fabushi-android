use std::collections::BTreeMap;

pub const SAND_OS_NOTIFICATION_THROTTLE_MS: u64 = 5_000;
pub const MAX_NOTIFICATION_BODY_LENGTH: usize = 140;

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct NotificationSnapshot {
    pub id: String,
    pub name: String,
    pub is_running: bool,
    pub awaiting_reason: Option<String>,
    pub notify_enabled: bool,
    pub is_hidden_from_sidebar: bool,
    pub last_message_id: Option<String>,
    pub last_message_preview: Option<String>,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord)]
pub enum NotificationTransitionKind {
    AgentNeedsInput,
    AgentDone,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct NotificationTransition {
    pub agent_id: String,
    pub agent_name: String,
    pub kind: NotificationTransitionKind,
    pub reason: Option<String>,
    pub notify_enabled: bool,
    pub is_hidden_from_sidebar: bool,
    pub last_message_id: Option<String>,
    pub last_message_preview: Option<String>,
}

pub fn diff_agent_notification_transitions(
    previous: &BTreeMap<String, NotificationSnapshot>,
    next: &[NotificationSnapshot],
) -> Vec<NotificationTransition> {
    let mut transitions = Vec::new();
    for agent in next {
        let Some(before) = previous.get(&agent.id) else {
            continue;
        };
        let became_awaiting = agent.awaiting_reason.is_some() && before.awaiting_reason.is_none();
        let finished_turn =
            before.is_running && !agent.is_running && agent.awaiting_reason.is_none();
        if !became_awaiting && !finished_turn {
            continue;
        }
        transitions.push(NotificationTransition {
            agent_id: agent.id.clone(),
            agent_name: agent.name.clone(),
            kind: if became_awaiting {
                NotificationTransitionKind::AgentNeedsInput
            } else {
                NotificationTransitionKind::AgentDone
            },
            reason: if became_awaiting {
                agent.awaiting_reason.clone()
            } else {
                None
            },
            notify_enabled: agent.notify_enabled,
            is_hidden_from_sidebar: agent.is_hidden_from_sidebar,
            last_message_id: agent.last_message_id.clone(),
            last_message_preview: agent.last_message_preview.clone(),
        });
    }
    transitions
}

pub fn should_notify(
    is_hidden: bool,
    notify_enabled: bool,
    is_window_focused: bool,
    last_notified_at_ms: Option<u64>,
    now_ms: u64,
    throttle_window_ms: u64,
) -> bool {
    !is_hidden
        && notify_enabled
        && !is_window_focused
        && last_notified_at_ms
            .is_none_or(|last| now_ms.saturating_sub(last) >= throttle_window_ms)
}

fn truncate(text: &str) -> String {
    let collapsed = text.split_whitespace().collect::<Vec<_>>().join(" ");
    if collapsed.chars().count() <= MAX_NOTIFICATION_BODY_LENGTH {
        return collapsed;
    }
    let head = collapsed
        .chars()
        .take(MAX_NOTIFICATION_BODY_LENGTH - 1)
        .collect::<String>()
        .trim_end()
        .to_string();
    format!("{head}…")
}

pub fn build_notification_content(transition: &NotificationTransition) -> (String, String) {
    let name = if transition.agent_name.trim().is_empty() {
        "Your agent"
    } else {
        transition.agent_name.trim()
    };
    match transition.kind {
        NotificationTransitionKind::AgentNeedsInput => {
            let reason = transition.reason.as_deref().unwrap_or("").trim();
            (
                format!("{name} needs you"),
                if reason.is_empty() {
                    "Waiting for your input.".to_string()
                } else {
                    truncate(reason)
                },
            )
        }
        NotificationTransitionKind::AgentDone => {
            let preview = transition
                .last_message_preview
                .as_deref()
                .unwrap_or("")
                .trim();
            (
                name.to_string(),
                if preview.is_empty() {
                    "Open Grok Bot to see what it did.".to_string()
                } else {
                    truncate(preview)
                },
            )
        }
    }
}

#[derive(Clone, Debug)]
pub struct SandOsNotificationDecider {
    previous: BTreeMap<String, NotificationSnapshot>,
    last_notified_at_ms: BTreeMap<(String, NotificationTransitionKind), u64>,
    accounted_message_id: BTreeMap<String, Option<String>>,
    throttle_window_ms: u64,
}

impl SandOsNotificationDecider {
    pub fn new(throttle_window_ms: u64) -> Self {
        Self {
            previous: BTreeMap::new(),
            last_notified_at_ms: BTreeMap::new(),
            accounted_message_id: BTreeMap::new(),
            throttle_window_ms,
        }
    }

    pub fn seed_baseline(&mut self, agents: &[NotificationSnapshot]) {
        for agent in agents {
            self.previous.entry(agent.id.clone()).or_insert_with(|| agent.clone());
            self.accounted_message_id
                .entry(agent.id.clone())
                .or_insert_with(|| agent.last_message_id.clone());
        }
    }

    pub fn decide(
        &mut self,
        agents: &[NotificationSnapshot],
        is_window_focused: bool,
        now_ms: u64,
    ) -> Vec<NotificationTransition> {
        let transitions = diff_agent_notification_transitions(&self.previous, agents);
        let out = self.gate(transitions, is_window_focused, now_ms);
        let mut next = BTreeMap::new();
        for agent in agents {
            self.accounted_message_id
                .entry(agent.id.clone())
                .or_insert_with(|| agent.last_message_id.clone());
            next.insert(agent.id.clone(), agent.clone());
        }
        self.previous = next;
        out
    }

    fn gate(
        &mut self,
        transitions: Vec<NotificationTransition>,
        is_window_focused: bool,
        now_ms: u64,
    ) -> Vec<NotificationTransition> {
        let mut out = Vec::new();
        for transition in transitions {
            let accounted = self
                .accounted_message_id
                .get(&transition.agent_id)
                .cloned()
                .flatten();
            if transition.kind == NotificationTransitionKind::AgentDone
                && (transition.last_message_id.is_none()
                    || transition.last_message_id == accounted)
            {
                continue;
            }
            self.accounted_message_id
                .insert(transition.agent_id.clone(), transition.last_message_id.clone());
            let key = (transition.agent_id.clone(), transition.kind);
            let last = self.last_notified_at_ms.get(&key).copied();
            if should_notify(
                transition.is_hidden_from_sidebar,
                transition.notify_enabled,
                is_window_focused,
                last,
                now_ms,
                self.throttle_window_ms,
            ) {
                self.last_notified_at_ms.insert(key, now_ms);
                out.push(transition);
            }
        }
        out
    }

    pub fn forget(&mut self, agent_id: &str) {
        self.previous.remove(agent_id);
        self.accounted_message_id.remove(agent_id);
        self.last_notified_at_ms
            .remove(&(agent_id.to_string(), NotificationTransitionKind::AgentDone));
        self.last_notified_at_ms.remove(&(
            agent_id.to_string(),
            NotificationTransitionKind::AgentNeedsInput,
        ));
    }
}

impl Default for SandOsNotificationDecider {
    fn default() -> Self {
        Self::new(SAND_OS_NOTIFICATION_THROTTLE_MS)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn agent(
        running: bool,
        awaiting: Option<&str>,
        message_id: Option<&str>,
    ) -> NotificationSnapshot {
        NotificationSnapshot {
            id: "a".into(),
            name: "Agent".into(),
            is_running: running,
            awaiting_reason: awaiting.map(str::to_string),
            notify_enabled: true,
            is_hidden_from_sidebar: false,
            last_message_id: message_id.map(str::to_string),
            last_message_preview: Some("done".into()),
        }
    }

    #[test]
    fn decider_requires_a_real_transition_and_new_done_message() {
        let mut decider = SandOsNotificationDecider::default();
        decider.seed_baseline(&[agent(true, None, Some("m1"))]);
        let done = decider.decide(&[agent(false, None, Some("m2"))], false, 10_000);
        assert_eq!(done.len(), 1);
        assert_eq!(done[0].kind, NotificationTransitionKind::AgentDone);

        decider.seed_baseline(&[agent(true, None, Some("m2"))]);
        let duplicate = decider.decide(&[agent(false, None, Some("m2"))], false, 20_000);
        assert!(duplicate.is_empty());
    }

    #[test]
    fn focus_hidden_and_throttle_gate_notifications() {
        assert!(!should_notify(false, true, true, None, 10_000, 5_000));
        assert!(!should_notify(true, true, false, None, 10_000, 5_000));
        assert!(!should_notify(false, false, false, None, 10_000, 5_000));
        assert!(!should_notify(false, true, false, Some(8_000), 10_000, 5_000));
        assert!(should_notify(false, true, false, Some(4_000), 10_000, 5_000));
    }

    #[test]
    fn notification_body_is_collapsed_and_bounded() {
        let transition = NotificationTransition {
            agent_id: "a".into(),
            agent_name: "Agent".into(),
            kind: NotificationTransitionKind::AgentNeedsInput,
            reason: Some("x ".repeat(200)),
            notify_enabled: true,
            is_hidden_from_sidebar: false,
            last_message_id: None,
            last_message_preview: None,
        };
        let (_, body) = build_notification_content(&transition);
        assert!(body.chars().count() <= MAX_NOTIFICATION_BODY_LENGTH);
        assert!(body.ends_with('…'));
    }
}
