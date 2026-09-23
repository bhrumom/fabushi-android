use super::{
    stream_attempt::{ProviderFailure, StreamAttemptHost, StreamAttemptInput, TurnStreamProvider},
    turn_settle::{prepare_checkpoint, persist_checkpoint, settle_completed_turn, TurnSettlement},
};
use std::collections::BTreeSet;

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct ProductionTurnInput {
    pub operation_id: String,
    pub request_id: String,
    pub agent_id: String,
    pub model: String,
    pub prompt: String,
    pub resume_checkpoint_available: bool,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum ProductionTurnEvent {
    Retrying {
        attempt: usize,
        delay_ms: u64,
        reason: String,
    },
    Delta(String),
    Completed {
        finish_reason: String,
        attempts: usize,
    },
    Failed {
        message: String,
    },
    Cancelled,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct ProductionTurnResult {
    pub events: Vec<ProductionTurnEvent>,
    pub settlement: Option<TurnSettlement>,
}

pub struct ProductionTurnAgentOwner<P: TurnStreamProvider> {
    stream: StreamAttemptHost<P>,
    active: BTreeSet<String>,
}

impl<P: TurnStreamProvider> ProductionTurnAgentOwner<P> {
    pub fn new(provider: P) -> Self {
        Self {
            stream: StreamAttemptHost::new(provider),
            active: BTreeSet::new(),
        }
    }

    pub fn run(
        &mut self,
        input: ProductionTurnInput,
    ) -> Result<ProductionTurnResult, ProviderFailure> {
        self.run_with_event_sink(input, &mut |_| Ok(()))
    }

    pub fn run_with_event_sink(
        &mut self,
        input: ProductionTurnInput,
        sink: &mut dyn FnMut(ProductionTurnEvent) -> Result<(), String>,
    ) -> Result<ProductionTurnResult, ProviderFailure> {
        if !self.active.insert(input.operation_id.clone()) {
            return Err(ProviderFailure::new("operation is already active"));
        }

        let stream_input = StreamAttemptInput {
            operation_id: input.operation_id.clone(),
            agent_id: input.agent_id,
            model: input.model,
            prompt: input.prompt,
            resume_checkpoint_available: input.resume_checkpoint_available,
        };

        let mut emitted_retries = Vec::new();
        let mut emitted_chunks = Vec::new();
        let result = self.stream.run_with_observers(
            &stream_input,
            &mut |retry| {
                emitted_retries.push(retry.clone());
            },
            &mut |chunk| {
                emitted_chunks.push(chunk.to_string());
                sink(ProductionTurnEvent::Delta(chunk.to_string()))
            },
        );
        self.active.remove(&input.operation_id);

        for retry in &emitted_retries {
            sink(ProductionTurnEvent::Retrying {
                attempt: retry.attempt,
                delay_ms: retry.delay_ms,
                reason: retry.reason.clone(),
            })
            .map_err(ProviderFailure::new)?;
        }

        match result {
            Ok(result) => {
                let completed = ProductionTurnEvent::Completed {
                    finish_reason: result.finish_reason.clone(),
                    attempts: result.attempts,
                };
                sink(completed.clone()).map_err(ProviderFailure::new)?;

                let checkpoint = prepare_checkpoint(
                    &input.operation_id,
                    result.attempts,
                    result.chunks.len(),
                )
                .map_err(ProviderFailure::new)?;
                let checkpoint = persist_checkpoint(checkpoint);
                let settlement = settle_completed_turn(
                    checkpoint,
                    result.finish_reason,
                )
                .map_err(ProviderFailure::new)?;

                let mut events = emitted_retries
                    .iter()
                    .map(|retry| ProductionTurnEvent::Retrying {
                        attempt: retry.attempt,
                        delay_ms: retry.delay_ms,
                        reason: retry.reason.clone(),
                    })
                    .collect::<Vec<_>>();
                events.extend(
                    emitted_chunks
                        .iter()
                        .cloned()
                        .map(ProductionTurnEvent::Delta),
                );
                events.push(completed);

                Ok(ProductionTurnResult {
                    events,
                    settlement: Some(settlement),
                })
            }
            Err(error) => {
                let failed = ProductionTurnEvent::Failed {
                    message: error.message.clone(),
                };
                sink(failed.clone()).map_err(ProviderFailure::new)?;
                Ok(ProductionTurnResult {
                    events: vec![failed],
                    settlement: None,
                })
            }
        }
    }

    pub fn cancel(&mut self, operation_id: &str) -> Result<ProductionTurnEvent, String> {
        self.active.remove(operation_id);
        self.stream.cancel(operation_id)?;
        Ok(ProductionTurnEvent::Cancelled)
    }

    pub fn is_active(&self, operation_id: &str) -> bool {
        self.active.contains(operation_id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::runner::stream_attempt::StreamGeneration;

    struct Provider;

    impl TurnStreamProvider for Provider {
        fn start_stream(
            &mut self,
            _input: &StreamAttemptInput,
            _attempt: usize,
        ) -> Result<StreamGeneration, ProviderFailure> {
            Ok(StreamGeneration {
                first_token_delay_ms: 1,
                chunks: vec!["a".into(), "b".into()],
                finish_reason: "stop".into(),
            })
        }

        fn cancel(&mut self, _operation_id: &str) -> Result<(), String> {
            Ok(())
        }
    }

    #[test]
    fn one_owner_streams_and_settles_durable_turn() {
        let mut owner = ProductionTurnAgentOwner::new(Provider);
        let result = owner
            .run(ProductionTurnInput {
                operation_id: "op".into(),
                request_id: "req".into(),
                agent_id: "agent".into(),
                model: "default".into(),
                prompt: "hello".into(),
                resume_checkpoint_available: false,
            })
            .unwrap();
        assert!(matches!(
            result.events.as_slice(),
            [
                ProductionTurnEvent::Delta(_),
                ProductionTurnEvent::Delta(_),
                ProductionTurnEvent::Completed { .. }
            ]
        ));
        assert!(result.settlement.unwrap().checkpoint.durable);
        assert!(!owner.is_active("op"));
    }
}
