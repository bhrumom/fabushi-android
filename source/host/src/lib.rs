//! Android-owned Mahayana Host boundary.
//!
//! The Host owns domain execution behind the Coordinator. It must not depend on Compose,
//! Activity, ViewModel, or Android screen state.

use std::sync::{
    mpsc::{self, Receiver, Sender},
    Arc, Mutex,
};

use fabushi_android_shared::{CoordinatorFailure, CoordinatorRequest};

pub trait HostRuntime {
    fn execute(&mut self, request: &CoordinatorRequest) -> Result<String, CoordinatorFailure>;
    fn cancel(&mut self, request_id: &str, reason: Option<&str>) -> Result<(), CoordinatorFailure>;
}

#[derive(Clone, Default)]
pub struct HostEventBus {
    subscribers: Arc<Mutex<Vec<Sender<HostEvent>>>>,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct HostEvent {
    pub family: String,
    pub session_id: String,
    pub payload_json: String,
}

impl HostEventBus {
    pub fn subscribe(&self) -> Receiver<HostEvent> {
        let (sender, receiver) = mpsc::channel();
        self.subscribers
            .lock()
            .expect("host event subscribers lock poisoned")
            .push(sender);
        receiver
    }

    pub fn emit(&self, event: HostEvent) {
        let mut subscribers = self
            .subscribers
            .lock()
            .expect("host event subscribers lock poisoned");
        subscribers.retain(|subscriber| subscriber.send(event.clone()).is_ok());
    }

    pub fn subscriber_count(&self) -> usize {
        self.subscribers
            .lock()
            .expect("host event subscribers lock poisoned")
            .len()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn event_bus_fans_out_without_competing_consumers() {
        let bus = HostEventBus::default();
        let first = bus.subscribe();
        let second = bus.subscribe();
        let event = HostEvent {
            family: "transcript".into(),
            session_id: "s".into(),
            payload_json: "{}".into(),
        };
        bus.emit(event.clone());
        assert_eq!(first.recv().unwrap(), event);
        assert_eq!(second.recv().unwrap(), event);
    }

    #[test]
    fn dropped_subscriber_is_removed_on_next_emit() {
        let bus = HostEventBus::default();
        let receiver = bus.subscribe();
        assert_eq!(bus.subscriber_count(), 1);
        drop(receiver);
        bus.emit(HostEvent {
            family: "health".into(),
            session_id: "s".into(),
            payload_json: "{}".into(),
        });
        assert_eq!(bus.subscriber_count(), 0);
    }
}
