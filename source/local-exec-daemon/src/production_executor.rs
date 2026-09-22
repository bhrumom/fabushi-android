use std::collections::BTreeMap;
use fabushi_android_shared::{ExecutionError, ExecutionRequest, ExecutionResult};

pub trait ProductionAction: Send {
    fn execute(&mut self, payload_json: &str) -> Result<String, ExecutionError>;
}

#[derive(Default)]
pub struct ProductionExecutor { actions: BTreeMap<String, Box<dyn ProductionAction>> }

impl ProductionExecutor {
    pub fn register(&mut self, capability_id: impl Into<String>, action: Box<dyn ProductionAction>) -> Result<(), ExecutionError> {
        let id=capability_id.into();
        if id.trim().is_empty() { return Err(ExecutionError::InvalidRequest("capability id is required".into())); }
        if self.actions.insert(id.clone(), action).is_some() {
            return Err(ExecutionError::InvalidRequest(format!("duplicate production capability: {id}")));
        }
        Ok(())
    }

    pub fn execute(&mut self, request: &ExecutionRequest) -> Result<ExecutionResult, ExecutionError> {
        request.validate()?;
        let action=self.actions.get_mut(&request.capability_id)
            .ok_or_else(|| ExecutionError::CapabilityUnavailable(request.capability_id.clone()))?;
        let payload_json=action.execute(&request.payload_json)?;
        Ok(ExecutionResult { operation_id: request.operation_id.clone(), payload_json })
    }
}
