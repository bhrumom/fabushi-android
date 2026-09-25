use std::collections::BTreeSet;

#[derive(Default)]
pub struct RemoteJobRegistry { active:BTreeSet<String> }

impl RemoteJobRegistry {
    pub fn begin(&mut self, operation_id:impl Into<String>)->Result<(),&'static str>{
        let id=operation_id.into();
        if id.trim().is_empty(){return Err("operation id is required");}
        if !self.active.insert(id){return Err("operation id already active");}
        Ok(())
    }
    pub fn settle(&mut self,operation_id:&str)->bool{self.active.remove(operation_id)}
    pub fn cancel(&mut self,operation_id:&str)->Result<(),&'static str>{
        if !self.active.remove(operation_id){return Err("operation id is not active");}
        Ok(())
    }
    pub fn active_count(&self)->usize{self.active.len()}
}
