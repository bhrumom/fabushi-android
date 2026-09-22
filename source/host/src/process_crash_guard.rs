#[derive(Clone,Copy,Debug,PartialEq,Eq)]
pub enum ProcessCrashKind{Panic,Abort,Signal,Unknown}
#[derive(Clone,Debug,PartialEq,Eq)]
pub struct ProcessCrashRecord{pub kind:ProcessCrashKind,pub message:String}
pub fn handle_process_crash(kind:ProcessCrashKind,message:impl Into<String>)->ProcessCrashRecord{ProcessCrashRecord{kind,message:message.into()}}
