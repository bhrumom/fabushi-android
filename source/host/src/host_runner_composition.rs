use fabushi_android_shared::{ExecutionError,ExecutionRequest,ExecutionResult};

pub const DEFAULT_SAND_MODEL:&str="default";
pub const SAND_SUMMARIZATION_MAX_PROMPT_CHARS:usize=32_000;

pub trait HostRunnerSession {
    fn execute(&mut self,request:&ExecutionRequest)->Result<ExecutionResult,ExecutionError>;
    fn cancel(&mut self,operation_id:&str)->Result<(),ExecutionError>;
}

pub struct HostRunnerComposition<R:HostRunnerSession>{runner:R}
impl<R:HostRunnerSession> HostRunnerComposition<R>{
    pub fn new(runner:R)->Self{Self{runner}}
    pub fn run(&mut self,request:&ExecutionRequest)->Result<ExecutionResult,ExecutionError>{request.validate()?;self.runner.execute(request)}
    pub fn cancel(&mut self,operation_id:&str)->Result<(),ExecutionError>{self.runner.cancel(operation_id)}
}
