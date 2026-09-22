use std::collections::{BTreeMap, BTreeSet};
use fabushi_android_shared::rpc::client_side_tool_v2_transport::{ClientToolTransportUpdate, ClientToolUpdateKind};

#[derive(Default)]
struct AgentFence {
    epoch: String,
    sequence: u64,
    retired_epochs: BTreeSet<String>,
    updates_by_call: BTreeMap<String, Vec<ClientToolTransportUpdate>>,
}

#[derive(Default)]
pub struct ClientSideToolV2Relay {
    agents: BTreeMap<String, AgentFence>,
}

impl ClientSideToolV2Relay {
    pub fn accept(&mut self, update: ClientToolTransportUpdate) -> bool {
        if !update.validate() { return false; }
        let fence = self.agents.entry(update.agent_id.clone()).or_default();
        if fence.epoch.is_empty() {
            fence.epoch = update.epoch.clone();
        } else if fence.epoch != update.epoch {
            if fence.retired_epochs.contains(&update.epoch) { return false; }
            fence.retired_epochs.insert(std::mem::replace(&mut fence.epoch, update.epoch.clone()));
            fence.sequence = 0;
            fence.updates_by_call.clear();
        }
        if update.sequence <= fence.sequence { return false; }
        fence.sequence = update.sequence;
        match update.kind {
            ClientToolUpdateKind::Reset => {
                fence.updates_by_call.clear();
                true
            }
            ClientToolUpdateKind::Call => {
                fence.updates_by_call.insert(update.tool_call_id.clone().unwrap(), vec![update]);
                true
            }
            ClientToolUpdateKind::Result => {
                let id = update.tool_call_id.clone().unwrap();
                let Some(lifecycle) = fence.updates_by_call.get_mut(&id) else { return false; };
                if !matches!(lifecycle.first().map(|u| &u.kind), Some(ClientToolUpdateKind::Call)) { return false; }
                lifecycle.push(update);
                true
            }
        }
    }

    pub fn replay(&self) -> Vec<ClientToolTransportUpdate> {
        let mut updates: Vec<_> = self.agents.values().flat_map(|fence| fence.updates_by_call.values().flatten().cloned()).collect();
        updates.sort_by_key(|update| update.sequence);
        updates
    }

    pub fn clear(&mut self) { self.agents.clear(); }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn update(sequence: u64, kind: ClientToolUpdateKind) -> ClientToolTransportUpdate {
        ClientToolTransportUpdate {
            account_slot: "account".into(), agent_id: "a".into(), epoch: "e1".into(),
            sequence, tool_call_id: (!matches!(kind, ClientToolUpdateKind::Reset)).then(|| "t".into()),
            kind, payload: vec![],
        }
    }

    #[test]
    fn rejects_out_of_order_and_result_without_call() {
        let mut relay = ClientSideToolV2Relay::default();
        assert!(!relay.accept(update(1, ClientToolUpdateKind::Result)));
        assert!(relay.accept(update(2, ClientToolUpdateKind::Call)));
        assert!(!relay.accept(update(2, ClientToolUpdateKind::Result)));
        assert!(relay.accept(update(3, ClientToolUpdateKind::Result)));
        assert_eq!(relay.replay().len(), 2);
    }
}
